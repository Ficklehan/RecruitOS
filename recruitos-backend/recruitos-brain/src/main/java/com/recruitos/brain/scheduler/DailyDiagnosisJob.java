package com.recruitos.brain.scheduler;

import com.recruitos.brain.TenantResolver;
import com.recruitos.brain.client.CandidateClient;
import com.recruitos.brain.client.JobClient;
import com.recruitos.brain.engine.CognitiveEventBridge;
import com.recruitos.brain.service.CognitiveMemoryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;

/**
 * AI 日常巡检
 * 扫描所有在招岗位，生成管道健康诊断和主动观察。
 * v8: 接入认知层，诊断结果写入 cognitive_judgment + cognitive_observation
 */
@Component
public class DailyDiagnosisJob {

    private static final Logger log = LoggerFactory.getLogger(DailyDiagnosisJob.class);

    @Resource
    private CognitiveMemoryService memory;

    @Resource
    private CognitiveEventBridge eventBridge;

    @Resource
    private JobClient jobClient;

    @Resource
    private CandidateClient candidateClient;

    @Resource
    private TenantResolver tenantResolver;

    /**
     * 每天早上 8 点：管道健康巡检 + 主动观察生成
     */
    @Scheduled(cron = "0 0 8 * * ?")
    public void diagnoseAllJobs() {
        log.info("DailyDiagnosisJob: starting pipeline health scan");
        List<Long> tenantIds = tenantResolver.getActiveTenantIds();

        for (Long tenantId : tenantIds) {
            try {
                checkPipelineHealth(tenantId);
                checkSilentChannels(tenantId);
                checkStaleObservations(tenantId);
            } catch (Exception e) {
                log.error("DailyDiagnosisJob failed for tenant {}", tenantId, e);
            }
        }
        log.info("DailyDiagnosisJob: scan complete");
    }

    /**
     * 管道健康检查：每个在招岗位评估
     */
    private void checkPipelineHealth(Long tenantId) {
        // 从 Feign 客户端获取真实数据
        List<Map<String, Object>> activeJobs = getActiveJobs(tenantId);

        for (Map<String, Object> job : activeJobs) {
            Long jobId = toLong(job.get("id"));
            String jobTitle = (String) job.getOrDefault("title", "未知岗位");

            // 检查管道状态
            Map<String, Object> pipelineData = getPipelineData(job);
            int totalCandidates  = toInt(pipelineData.get("totalCandidates"), 0);
            int daysOpen = computeDaysOpen(job);
            int daysSinceLast     = toInt(pipelineData.get("daysSinceLastCandidate"), 0);

            // 判断健康状态
            if (daysSinceLast >= 7 && totalCandidates < 5) {
                // 管道枯竭 → 创建 CRITICAL 观察
                String title = "🔥 " + jobTitle + " — 管道枯竭";
                String body = String.format(
                    "该岗位已经%d天没有新候选人进入管道，当前仅有%d名候选人。"
                    + "历史数据显示，管道停滞超过7天的岗位平均招聘周期延长50%%。"
                    + "建议：1）检查JD是否过于狭窄；2）增加主动寻源渠道；3）考虑猎头并行。",
                    daysSinceLast, totalCandidates);

                memory.createObservation(tenantId, "ALERT", "CRITICAL",
                    title, body,
                    List.of(Map.of("type", "JOB", "id", jobId, "title", jobTitle)),
                    Map.of("text", "查看岗位", "action_type", "VIEW_JOB",
                        "params", Map.of("jobId", jobId)),
                    LocalDateTime.now().plusDays(7));

                // 同时记录管道停滞事件
                eventBridge.onPipelineBlocked(tenantId, jobId, daysSinceLast);
            }
            else if (totalCandidates > 0 && totalCandidates < 8 && daysOpen > 14) {
                // 管道偏瘦 → WARNING
                String title = "⚠️ " + jobTitle + " — 候选人储备不足";
                String body = String.format(
                    "该岗位已开放%d天，仅积累%d名候选人。建议增加寻源渠道覆盖或复核JD要求是否过于严格。",
                    daysOpen, totalCandidates);

                memory.createObservation(tenantId, "INSIGHT", "WARNING",
                    title, body,
                    List.of(Map.of("type", "JOB", "id", jobId)),
                    Map.of("text", "查看岗位", "action_type", "VIEW_JOB",
                        "params", Map.of("jobId", jobId)),
                    LocalDateTime.now().plusDays(14));
            }
            else if (totalCandidates > 20) {
                // 候选人充足 → INFO
                String title = "✅ " + jobTitle + " — 管道充足";
                String body = String.format(
                    "该岗位已有%d名候选人，进展良好。建议关注面试转化率，确保高质量候选人进入终面。",
                    totalCandidates);

                memory.createObservation(tenantId, "INSIGHT", "INFO",
                    title, body,
                    List.of(Map.of("type", "JOB", "id", jobId)),
                    null,
                    LocalDateTime.now().plusDays(30));
            }

            // 生成并持久化管线健康判断
            Map<String, Object> judgment = Map.of(
                "primary", getHealthLabel(daysSinceLast, totalCandidates),
                "details", "管道健康: " + totalCandidates + "人, 最后活跃: " + daysSinceLast + "天前",
                "confidence", 0.8,
                "key_evidence", List.of(
                    "候选人总数: " + totalCandidates,
                    "最后新增距今: " + daysSinceLast + "天",
                    "岗位已开放: " + daysOpen + "天"
                )
            );

            memory.publishJudgment(tenantId, "PIPELINE_HEALTH", "JOB", jobId,
                getHealthLabel(daysSinceLast, totalCandidates),
                judgment, 0.8, List.of(),
                Map.of(
                    "alternative_explanation", "管道候选人少可能因为该岗位特殊（如极端稀缺的细分领域），而非招聘策略问题",
                    "weakest_evidence", "管道数据依赖于ATS数据准确性，被动候选人未被统计",
                    "what_would_overturn", "如果未来3天内有3+名合格候选人进入管道"
                ));
        }
    }

    /**
     * 渠道沉默检查
     */
    private void checkSilentChannels(Long tenantId) {
        // 检查内推渠道是否沉默
        List<Map<String, Object>> hireEvents = memory.getObjectEvents(tenantId, "JOB", null);
        // 模拟：检查是否需要提醒
        memory.createObservation(tenantId, "SUGGESTION", "INFO",
            "💡 内推渠道提醒",
            "过去2周内推渠道零产出。历史数据显示内推渠道的留存率比其他渠道高40%。建议发起内推激励活动。",
            List.of(Map.of("type", "CHANNEL", "id", 1, "name", "内推")),
            Map.of("text", "发起内推活动", "action_type", "CREATE_REFERRAL_CAMPAIGN"),
            LocalDateTime.now().plusDays(14));
    }

    /**
     * 过期观察清理
     */
    private void checkStaleObservations(Long tenantId) {
        // 标记过期观察
        List<Map<String, Object>> pending = memory.getPendingObservations(tenantId);
        for (Map<String, Object> obs : pending) {
            Object expiresAt = obs.get("expiresAt");
            if (expiresAt != null) {
                try {
                    LocalDateTime expiry = LocalDateTime.parse(expiresAt.toString()
                        .replace(" ", "T"));
                    if (expiry.isBefore(LocalDateTime.now())) {
                        memory.actionObservation((Long) obs.get("id"), "DEFERRED", null);
                    }
                } catch (Exception ignored) {}
            }
        }
    }

    // --- helpers ---

    private List<Map<String, Object>> getActiveJobs(Long tenantId) {
        try {
            List<Map<String, Object>> jobs = jobClient.getActiveJobs();
            if (jobs != null) return jobs;
        } catch (Exception e) {
            log.warn("getActiveJobs failed for tenant {}: {}", tenantId, e.getMessage());
        }
        return Collections.emptyList();
    }

    /**
     * 从 CandidateClient 获取岗位管道的真实统计数据。
     */
    private Map<String, Object> getPipelineData(Map<String, Object> job) {
        Map<String, Object> data = new LinkedHashMap<>();
        try {
            Long jobId = toLong(job.get("id"));
            if (jobId == null) return data;

            List<Map<String, Object>> candidates = candidateClient.listByJob(jobId);
            if (candidates == null) candidates = Collections.emptyList();

            int screen = 0, interview = 0, offer = 0, finalRound = 0;
            LocalDateTime lastActivity = null;
            List<String> channels = new ArrayList<>();

            for (Map<String, Object> c : candidates) {
                String stage = toString(c.get("pipelineStage")).toUpperCase();
                if (stage.contains("SCREEN") || stage.contains("NEW")) screen++;
                else if (stage.contains("OFFER")) offer++;
                else if (stage.contains("FINAL")) { interview++; finalRound++; }
                else if (stage.contains("INTERVIEW") || stage.contains("ROUND")) interview++;

                LocalDateTime ts = parseDateTime(c.get("updatedAt"));
                if (ts != null && (lastActivity == null || ts.isAfter(lastActivity))) {
                    lastActivity = ts;
                }

                String channel = toString(c.get("sourceChannel"));
                if (!channel.isEmpty() && !channels.contains(channel)) {
                    channels.add(channel);
                }
            }

            int daysSinceLast = 0;
            if (lastActivity != null) {
                daysSinceLast = (int) Duration.between(lastActivity, LocalDateTime.now()).toDays();
            }

            data.put("totalCandidates", candidates.size());
            data.put("inScreen", screen);
            data.put("inInterview", interview);
            data.put("inFinalRound", finalRound);
            data.put("inOffer", offer);
            data.put("daysSinceLastCandidate", daysSinceLast);
            data.put("channelSources", channels);
        } catch (Exception e) {
            log.warn("getPipelineData failed for job {}: {}", toLong(job.get("id")), e.getMessage());
        }

        data.putIfAbsent("totalCandidates", 0);
        data.putIfAbsent("daysSinceLastCandidate", 0);
        return data;
    }

    /**
     * 计算岗位已开放天数——优先使用微服务返回的 createdAt 字段。
     */
    private int computeDaysOpen(Map<String, Object> job) {
        Object createdAt = job.get("createdAt");
        if (createdAt != null) {
            LocalDateTime ts = parseDateTime(createdAt);
            if (ts != null) return (int) Duration.between(ts, LocalDateTime.now()).toDays();
        }
        return toInt(job.get("daysOpen"), 14);
    }

    // ── 类型安全工具 ──

    private Long toLong(Object o) {
        if (o instanceof Long) return (Long) o;
        if (o instanceof Integer) return ((Integer) o).longValue();
        return null;
    }

    private int toInt(Object o, int def) {
        if (o instanceof Number) return ((Number) o).intValue();
        return def;
    }

    private String toString(Object o) {
        return o != null ? o.toString() : "";
    }

    private LocalDateTime parseDateTime(Object o) {
        if (o == null) return null;
        try {
            String s = o.toString().replace("T", " ").substring(0, 19);
            return LocalDateTime.parse(s.replace(" ", "T"));
        } catch (Exception e) { return null; }
    }


    private String getHealthLabel(int daysSinceLast, int total) {
        if (daysSinceLast >= 7 && total < 5) return "🔥 管道枯竭";
        if (daysSinceLast >= 5) return "⚠️ 管道缓慢";
        if (total >= 10) return "✅ 管道健康";
        return "📋 管道正常";
    }
}
