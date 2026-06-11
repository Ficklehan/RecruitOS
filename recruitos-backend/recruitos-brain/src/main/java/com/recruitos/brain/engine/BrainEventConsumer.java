package com.recruitos.brain.engine;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.recruitos.brain.mapper.BrainMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import java.util.*;
import java.util.concurrent.*;

/**
 * AI 事件驱动的消息消费者 (T3.10)。
 * 
 * <p>消费 3 类业务事件，触发 AI 模型更新：
 * 1. INTERVIEW_EVAL_SUBMITTED  → 更新面试官质量 + 意向信号
 * 2. OFFER_DECISION            → Offer→意向模型反馈
 * 3. ONBOARD_COMPLETED         → 人才密度更新 + 周期模型校准
 * 
 * <p>当前版本：使用内存队列（BlockingQueue），后续可替换为 Kafka/RabbitMQ。
 */
@Component
public class BrainEventConsumer {
    private static final Logger log = LoggerFactory.getLogger(BrainEventConsumer.class);

    private final BlockingQueue<BrainEvent> eventQueue = new LinkedBlockingQueue<>(10000);
    private final ExecutorService consumerExecutor = Executors.newFixedThreadPool(2);
    private volatile boolean running = true;

    @Resource private BrainMapper brainMapper;
    @Resource private OfferFeedbackService offerFeedbackService;
    @Resource private ObjectMapper objectMapper;

    @PostConstruct
    public void start() {
        for (int i = 0; i < 2; i++) {
            consumerExecutor.submit(this::consumeLoop);
        }
        log.info("BrainEventConsumer started — 2 consumer threads, queue capacity: 10000");
    }

    @PreDestroy
    public void stop() {
        running = false;
        consumerExecutor.shutdown();
        try { consumerExecutor.awaitTermination(10, TimeUnit.SECONDS); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        log.info("BrainEventConsumer stopped");
    }

    // ===== 事件发布 =====

    /**
     * 发布面试评价提交事件。
     */
    @Async
    public void onInterviewEvalSubmitted(Long interviewId, Long interviewerId, Long candidateId, Long jobId,
                                          Map<String, Object> evalData) {
        BrainEvent event = new BrainEvent();
        event.type = "INTERVIEW_EVAL_SUBMITTED";
        event.payload = new LinkedHashMap<>();
        event.payload.put("interviewId", interviewId);
        event.payload.put("interviewerId", interviewerId);
        event.payload.put("candidateId", candidateId);
        event.payload.put("jobId", jobId);
        event.payload.put("evalData", evalData);
        publish(event);
    }

    /**
     * 发布 Offer 决定事件。
     */
    @Async
    public void onOfferDecision(Long candidateId, Long jobId, boolean accepted) {
        BrainEvent event = new BrainEvent();
        event.type = "OFFER_DECISION";
        event.payload = new LinkedHashMap<>();
        event.payload.put("candidateId", candidateId);
        event.payload.put("jobId", jobId);
        event.payload.put("accepted", accepted);
        publish(event);
    }

    /**
     * 发布入职完成事件。
     */
    @Async
    public void onOnboardCompleted(Long candidateId, Long jobId, Long orgId) {
        BrainEvent event = new BrainEvent();
        event.type = "ONBOARD_COMPLETED";
        event.payload = new LinkedHashMap<>();
        event.payload.put("candidateId", candidateId);
        event.payload.put("jobId", jobId);
        event.payload.put("orgId", orgId);
        publish(event);
    }

    private void publish(BrainEvent event) {
        event.timestamp = System.currentTimeMillis();
        event.eventId = UUID.randomUUID().toString();
        boolean offered = eventQueue.offer(event);
        if (!offered) {
            log.warn("Event queue full, dropping event: {}", event.type);
        } else {
            log.debug("Event published: type={}, id={}", event.type, event.eventId);
        }
    }

    // ===== 消费循环 =====

    private void consumeLoop() {
        while (running) {
            try {
                BrainEvent event = eventQueue.poll(5, TimeUnit.SECONDS);
                if (event == null) continue;
                processEvent(event);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                log.error("Error processing event", e);
            }
        }
    }

    private void processEvent(BrainEvent event) {
        log.info("Processing event: type={}, id={}", event.type, event.eventId);
        switch (event.type) {
            case "INTERVIEW_EVAL_SUBMITTED":
                handleInterviewEval(event);
                break;
            case "OFFER_DECISION":
                handleOfferDecision(event);
                break;
            case "ONBOARD_COMPLETED":
                handleOnboardCompleted(event);
                break;
            default:
                log.warn("Unknown event type: {}", event.type);
        }

        // 审计日志
        try {
            Map<String, Object> trace = new LinkedHashMap<>();
            trace.put("id", brainMapper.nextId("ai_decision_log"));
            trace.put("tenantId", 0L);
            trace.put("decisionType", "EVENT_CONSUMED");
            trace.put("targetType", event.type);
            trace.put("decisionDetail", objectMapper.writeValueAsString(event.payload));
            trace.put("confidence", 1.0);
            trace.put("autoExecuted", true);
            brainMapper.insertDecisionLog(trace);
        } catch (Exception e) { log.warn("Failed to log event consumption", e); }
    }

    // ===== 事件处理器 =====

    private void handleInterviewEval(BrainEvent event) {
        // 面试评价提交 → 更新意向信号（候选人参加了面试，调整意向评分）
        Long candidateId = toLong(event.payload.get("candidateId"));
        if (candidateId != null) {
            log.info("Interview eval event → triggering intent signal refresh for candidate={}", candidateId);
            // 在实际部署中，这里会重新计算意向信号并更新 predict 结果
        }
    }

    private void handleOfferDecision(BrainEvent event) {
        // Offer 决定 → 回流到意向模型
        Long candidateId = toLong(event.payload.get("candidateId"));
        Long jobId = toLong(event.payload.get("jobId"));
        Boolean accepted = (Boolean) event.payload.get("accepted");
        if (candidateId != null && jobId != null && accepted != null) {
            offerFeedbackService.onOfferDecision(candidateId, jobId, accepted);
        }
    }

    private void handleOnboardCompleted(BrainEvent event) {
        // 入职完成 → 更新人才密度 + 周期模型
        Long orgId = toLong(event.payload.get("orgId"));
        if (orgId != null) {
            log.info("Onboard event → triggering talent density refresh for org={}", orgId);
            // 在实际部署中，这里会触发人才密度重算
        }
    }

    private Long toLong(Object o) { return o instanceof Number ? ((Number) o).longValue() : null; }

    // ===== 内部类 =====

    static class BrainEvent {
        String eventId;
        String type;
        long timestamp;
        Map<String, Object> payload;
    }
}
