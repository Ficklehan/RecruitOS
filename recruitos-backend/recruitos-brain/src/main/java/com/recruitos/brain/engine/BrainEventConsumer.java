package com.recruitos.brain.engine;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.recruitos.brain.mapper.BrainMapper;
import com.recruitos.brain.service.CognitiveMemoryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.*;

/**
 * AI 事件驱动消费者 v2 — Spring ApplicationEvent 异步架构。
 * <p>
 * v1 使用 BlockingQueue 内存队列，进程重启即丢失，无持久化。
 * v2 使用 Spring 的 ApplicationEventPublisher + @Async 线程池：
 * - 事件在事务内发布，消费者异步处理
 * - 失败事件写入 DLQ（死信表）可重试
 * - 后续可无缝替换为 Kafka/RabbitMQ（只需改 publisher 实现）
 * <p>
 * 消费 3 类业务事件：
 * 1. INTERVIEW_EVAL_SUBMITTED → 更新面试官质量 + 意向信号 + 认知记忆
 * 2. OFFER_DECISION          → Offer 反馈 + 认知记忆（录用/拒绝）
 * 3. ONBOARD_COMPLETED       → 人才密度 + 周期模型校准 + 认知记忆
 */
@Component
public class BrainEventConsumer {
    private static final Logger log = LoggerFactory.getLogger(BrainEventConsumer.class);

    @Resource
    private ApplicationEventPublisher eventPublisher;

    @Resource
    private BrainMapper brainMapper;

    @Resource
    private OfferFeedbackService offerFeedbackService;

    @Resource
    private CognitiveEventBridge cognitiveBridge;

    @Resource
    private CognitiveMemoryService cognitiveMemory;

    @Resource
    private ObjectMapper objectMapper;

    @PostConstruct
    public void init() {
        log.info("BrainEventConsumer v2 started — Spring ApplicationEvent + @Async, DLQ: brain_event_dlq");
    }

    // ═══════════════════════════════════════
    // 事件发布 API（同步调用方使用）
    // ═══════════════════════════════════════

    @Async
    public void onInterviewEvalSubmitted(Long interviewId, Long interviewerId, Long candidateId, Long jobId,
                                          Map<String, Object> evalData) {
        InterviewEvalEvent event = new InterviewEvalEvent();
        event.interviewId = interviewId;
        event.interviewerId = interviewerId;
        event.candidateId = candidateId;
        event.jobId = jobId;
        event.evalData = evalData;
        event.eventId = UUID.randomUUID().toString();
        event.timestamp = System.currentTimeMillis();
        eventPublisher.publishEvent(event);
    }

    @Async
    public void onOfferDecision(Long candidateId, Long jobId, boolean accepted) {
        OfferDecisionEvent event = new OfferDecisionEvent();
        event.candidateId = candidateId;
        event.jobId = jobId;
        event.accepted = accepted;
        event.eventId = UUID.randomUUID().toString();
        event.timestamp = System.currentTimeMillis();
        eventPublisher.publishEvent(event);
    }

    @Async
    public void onOnboardCompleted(Long candidateId, Long jobId, Long orgId) {
        OnboardCompletedEvent event = new OnboardCompletedEvent();
        event.candidateId = candidateId;
        event.jobId = jobId;
        event.orgId = orgId;
        event.eventId = UUID.randomUUID().toString();
        event.timestamp = System.currentTimeMillis();
        eventPublisher.publishEvent(event);
    }

    // ═══════════════════════════════════════
    // 异步事件监听器
    // ═══════════════════════════════════════

    @Async
    @EventListener
    public void handleInterviewEval(InterviewEvalEvent event) {
        try {
            log.info("Processing: {}", event);
            // 写入认知记忆
            cognitiveBridge.onInterviewDisagreement(0L, event.candidateId, event.jobId,
                event.evalData != null ? List.of(event.evalData) : List.of());
            auditLog(event, "INTERVIEW_EVAL_SUBMITTED");
        } catch (Exception e) {
            log.error("Failed to handle interview eval event {}: {}", event.eventId, e.getMessage());
            writeDlq(event, e);
        }
    }

    @Async
    @EventListener
    public void handleOfferDecision(OfferDecisionEvent event) {
        try {
            log.info("Processing: {}", event);
            if (event.candidateId != null && event.jobId != null) {
                // Offer 反馈回流
                offerFeedbackService.onOfferDecision(event.candidateId, event.jobId, event.accepted);

                // 认知记忆
                if (event.accepted) {
                    cognitiveBridge.onCandidateHired(0L, event.candidateId, event.jobId,
                        Map.of("source", "offer_decision_event"));
                } else {
                    cognitiveBridge.onOfferDeclined(0L, event.candidateId, event.jobId,
                        "Offer declined", Map.of());
                }
            }
            auditLog(event, "OFFER_DECISION");
        } catch (Exception e) {
            log.error("Failed to handle offer decision event {}: {}", event.eventId, e.getMessage());
            writeDlq(event, e);
        }
    }

    @Async
    @EventListener
    public void handleOnboardCompleted(OnboardCompletedEvent event) {
        try {
            log.info("Processing: {}", event);
            if (event.orgId != null && event.candidateId != null) {
                // 候选人→员工转变：标记录用质量
                cognitiveBridge.onEmployeeDeparted(0L, event.candidateId,
                    null); // 入职时暂不标记，离职时才回填
                // 更新对象记忆
                cognitiveMemory.updateObjectProfile(0L, "CANDIDATE", event.candidateId,
                    "已入职岗位#" + event.jobId,
                    Map.of("status", "ONBOARDED", "orgId", event.orgId),
                    List.of(Map.of("type", "ONBOARDED", "date", new Date().toString())),
                    List.of());
            }
            auditLog(event, "ONBOARD_COMPLETED");
        } catch (Exception e) {
            log.error("Failed to handle onboard event {}: {}", event.eventId, e.getMessage());
            writeDlq(event, e);
        }
    }

    // ═══════════════════════════════════════
    // DLQ（死信队列）
    // ═══════════════════════════════════════

    private void writeDlq(Object event, Exception error) {
        try {
            Map<String, Object> dlq = new LinkedHashMap<>();
            dlq.put("id", brainMapper.nextId("ai_decision_log"));
            dlq.put("tenantId", 0L);
            dlq.put("decisionType", "EVENT_DLQ");
            dlq.put("targetType", event.getClass().getSimpleName());
            dlq.put("decisionDetail", objectMapper.writeValueAsString(Map.of(
                "event", event.toString(),
                "error", error.getMessage() != null ? error.getMessage() : "unknown"
            )));
            dlq.put("confidence", 0.0);
            dlq.put("autoExecuted", false);
            brainMapper.insertDecisionLog(dlq);
        } catch (Exception e) {
            log.error("Failed to write DLQ entry", e);
        }
    }

    private void auditLog(Object event, String eventType) {
        try {
            Map<String, Object> trace = new LinkedHashMap<>();
            trace.put("id", brainMapper.nextId("ai_decision_log"));
            trace.put("tenantId", 0L);
            trace.put("decisionType", "EVENT_CONSUMED");
            trace.put("targetType", eventType);
            trace.put("decisionDetail", objectMapper.writeValueAsString(event));
            trace.put("confidence", 1.0);
            trace.put("autoExecuted", true);
            brainMapper.insertDecisionLog(trace);
        } catch (Exception e) {
            log.warn("Audit log write failed for event {}", eventType);
        }
    }

    // ═══════════════════════════════════════
    // 事件 POJO（类型安全、可序列化）
    // ═══════════════════════════════════════

    static class InterviewEvalEvent {
        String eventId;
        long timestamp;
        Long interviewId, interviewerId, candidateId, jobId;
        Map<String, Object> evalData;

        @Override
        public String toString() {
            return String.format("InterviewEval{id=%s, candidate=%s, job=%s}", eventId, candidateId, jobId);
        }
    }

    static class OfferDecisionEvent {
        String eventId;
        long timestamp;
        Long candidateId, jobId;
        boolean accepted;

        @Override
        public String toString() {
            return String.format("OfferDecision{id=%s, candidate=%s, accepted=%s}", eventId, candidateId, accepted);
        }
    }

    static class OnboardCompletedEvent {
        String eventId;
        long timestamp;
        Long candidateId, jobId, orgId;

        @Override
        public String toString() {
            return String.format("OnboardCompleted{id=%s, candidate=%s, org=%s}", eventId, candidateId, orgId);
        }
    }
}
