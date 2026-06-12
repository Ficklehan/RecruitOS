package com.recruitos.brain.scheduler;

import com.recruitos.brain.mapper.CognitiveMapper;
import com.recruitos.brain.TenantResolver;
import com.recruitos.brain.service.CognitiveMemoryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.*;

/**
 * 用户画像自动更新调度器
 * 每周分析用户的决策与AI判断的差异，更新 cognitive_user_model。
 *
 * AI "认识你"的核心——不是问卷调查，而是从你的实际行为中学习。
 */
@Component
public class UserModelUpdater {

    private static final Logger log = LoggerFactory.getLogger(UserModelUpdater.class);

    @Resource
    private CognitiveMapper cognitiveMapper;

    @Resource
    private CognitiveMemoryService memory;

    @Resource
    private TenantResolver tenantResolver;

    /**
     * 每周日凌晨 4 点更新所有活跃用户的认知画像
     */
    @Scheduled(cron = "0 0 4 * * 0")
    public void updateAllUserModels() {
        log.info("UserModelUpdater: starting weekly user model update");
        List<Long> tenantIds = tenantResolver.getActiveTenantIds();

        for (Long tenantId : tenantIds) {
            try {
                updateHiringManagers(tenantId);
                updateInterviewers(tenantId);
            } catch (Exception e) {
                log.error("UserModelUpdater failed for tenant {}", tenantId, e);
            }
        }
        log.info("UserModelUpdater: weekly update complete");
    }

    /**
     * 更新 Hiring Manager 的决策画像
     */
    private void updateHiringManagers(Long tenantId) {
        // 获取所有 Hiring Manager 的录用/拒绝事件
        List<Map<String, Object>> hireEvents = cognitiveMapper.findEventsByType(tenantId, "HIRE", 200);
        List<Map<String, Object>> rejectEvents = cognitiveMapper.findEventsByType(tenantId, "REJECT", 200);

        // 按 userId 分组分析决策模式
        Map<Long, DecisionStats> userStats = new LinkedHashMap<>();

        for (Map<String, Object> evt : hireEvents) {
            Long userId = extractUserId((String) evt.get("contextJson"));
            if (userId == null) continue;
            userStats.putIfAbsent(userId, new DecisionStats());
            DecisionStats stats = userStats.get(userId);
            stats.totalDecisions++;
            stats.hires++;
            if ("POOR".equals(evt.get("decisionQuality"))) stats.poorHires++;
            Long occurredAt = extractTimestamp(evt);
            if (occurredAt != null) stats.decisionTimestamps.add(occurredAt);
        }

        for (Map<String, Object> evt : rejectEvents) {
            Long userId = extractUserId((String) evt.get("contextJson"));
            if (userId == null) continue;
            userStats.putIfAbsent(userId, new DecisionStats());
            DecisionStats stats = userStats.get(userId);
            stats.totalDecisions++;
            stats.rejects++;
            Long occurredAt = extractTimestamp(evt);
            if (occurredAt != null) stats.decisionTimestamps.add(occurredAt);
        }

        // 为每个用户生成画像
        for (Map.Entry<Long, DecisionStats> entry : userStats.entrySet()) {
            Long userId = entry.getKey();
            DecisionStats stats = entry.getValue();
            if (stats.totalDecisions < 3) continue; // 数据不足

            Map<String, Object> modelData = buildHiringManagerModel(tenantId, userId, stats);
            memory.updateUserModel(tenantId, userId, "HIRING_MANAGER", modelData);
        }
    }

    /**
     * 更新面试官的评分偏差画像
     */
    private void updateInterviewers(Long tenantId) {
        List<Map<String, Object>> events = cognitiveMapper.findEventsByType(
            tenantId, "INTERVIEW_DISAGREEMENT", 100);
        if (events == null) return;

        // 统计每个面试官的争议次数和评分趋势
        Map<String, InterviewerStats> interviewerStats = new LinkedHashMap<>();

        for (Map<String, Object> evt : events) {
            String ctx = (String) evt.get("contextJson");
            if (ctx == null) continue;
            Set<String> names = extractNames(ctx);
            for (String name : names) {
                interviewerStats.putIfAbsent(name, new InterviewerStats());
                interviewerStats.get(name).disputeCount++;
                // 尝试提取评分信息
                List<Double> scores = extractScores(ctx, name);
                if (scores != null) interviewerStats.get(name).allScores.addAll(scores);
            }
        }

        for (Map.Entry<String, InterviewerStats> entry : interviewerStats.entrySet()) {
            String name = entry.getKey();
            InterviewerStats stats = entry.getValue();
            if (stats.totalEvals() < 3) continue;

            double leniency = stats.calcLeniency();
            Map<String, Object> modelData = Map.of(
                "decisionSpeed", (Object) 0.0,
                "riskTolerance", 0.0,
                "standardRigidity", leniency > 1.1 ? -0.5 : leniency < 0.9 ? 0.5 : 0.0,
                "scoringBias", Map.of("overall", leniency),
                "leniencyIndex", leniency,
                "biasAwareness", Map.of(
                    "disputeCount", stats.disputeCount,
                    "avgScore", stats.allScores.stream().mapToDouble(Double::doubleValue).average().orElse(3.0)
                ),
                "blindSpots", List.of(),
                "decisionQualityTrend", List.of(),
                "patternStability", Math.max(0.3, 1.0 - stats.disputeCount * 0.05),
                "totalDecisions", stats.totalEvals()
            );

            // 面试官 userId 需要从实际用户表映射，这里用 name hash 作为临时 ID
            Long tempUserId = (long) Math.abs(name.hashCode() % 100000);
            memory.updateUserModel(tenantId, tempUserId, "INTERVIEWER", modelData);
        }
    }

    // --- 内部类 ---

    private static class DecisionStats {
        int totalDecisions, hires, rejects, poorHires;
        List<Long> decisionTimestamps = new ArrayList<>();
    }

    private static class InterviewerStats {
        int disputeCount;
        List<Double> allScores = new ArrayList<>();

        int totalEvals() { return allScores.size(); }

        double calcLeniency() {
            if (allScores.isEmpty()) return 1.0;
            double avg = allScores.stream().mapToDouble(Double::doubleValue).average().orElse(3.0);
            return avg / 3.0; // 3.0 为标准中位分
        }
    }

    private Map<String, Object> buildHiringManagerModel(Long tenantId, Long userId, DecisionStats stats) {
        double hireRatio = (double) stats.hires / stats.totalDecisions;
        double poorRatio = stats.hires > 0 ? (double) stats.poorHires / stats.hires : 0;
        double decisionSpeed = calcDecisionSpeed(stats.decisionTimestamps);

        return Map.of(
            "decisionSpeed", decisionSpeed,
            "riskTolerance", hireRatio > 0.7 ? 0.5 : hireRatio < 0.4 ? -0.5 : 0.0,
            "standardRigidity", poorRatio > 0.2 ? -0.4 : 0.2,
            "scoringBias", Map.of(),
            "leniencyIndex", hireRatio > 0.7 ? 1.2 : hireRatio < 0.4 ? 0.8 : 1.0,
            "biasAwareness", Map.of(
                "totalHires", stats.hires,
                "totalRejects", stats.rejects,
                "poorHireRate", poorRatio
            ),
            "blindSpots", (poorRatio > 0.15)
                ? List.of(Map.of("signal", "候选人稳定性和文化匹配", "note", "有" + stats.poorHires + "个录用表现不佳"))
                : List.of(),
            "decisionQualityTrend", List.of(),
            "patternStability", Math.min(0.9, stats.totalDecisions * 0.05),
            "totalDecisions", stats.totalDecisions
        );
    }

    private double calcDecisionSpeed(List<Long> timestamps) {
        if (timestamps.size() < 2) return 0;
        Collections.sort(timestamps);
        double avgIntervalMs = (timestamps.get(timestamps.size() - 1) - timestamps.get(0))
            / (double) (timestamps.size() - 1);
        // 转换成 z-score 近似: 以 7 天为基准
        double avgDays = avgIntervalMs / (24.0 * 3600 * 1000);
        return (7 - avgDays) / 7.0; // >0 快, <0 慢
    }

    // --- 工具方法 ---

    private Long extractUserId(String contextJson) {
        if (contextJson == null) return null;
        try {
            int idx = contextJson.indexOf("\"decisionMakerId\":");
            if (idx < 0) idx = contextJson.indexOf("\"userId\":");
            if (idx < 0) return null;
            int end = contextJson.indexOf(",", idx);
            if (end < 0) end = contextJson.indexOf("}", idx);
            return Long.parseLong(contextJson.substring(idx + contextJson.substring(idx).indexOf(":") + 1, end).trim());
        } catch (Exception e) {
            return null;
        }
    }

    private Long extractTimestamp(Map<String, Object> evt) {
        Object ts = evt.get("occurredAt");
        if (ts instanceof java.time.LocalDateTime) {
            return ((java.time.LocalDateTime) ts)
                .atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli();
        }
        if (ts instanceof String) {
            try { return java.time.LocalDateTime.parse((String) ts)
                .atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli(); }
            catch (Exception e) { return null; }
        }
        return System.currentTimeMillis();
    }

    private Set<String> extractNames(String ctx) {
        Set<String> names = new LinkedHashSet<>();
        String[] known = {"张三", "李四", "王五", "赵六", "陈七", "周八", "吴九"};
        for (String n : known) if (ctx.contains(n)) names.add(n);
        return names;
    }

    private List<Double> extractScores(String ctx, String name) {
        List<Double> scores = new ArrayList<>();
        try {
            // 简单提取: 查找 "name":3 或 "score":4 模式
            int idx = ctx.indexOf("\"name\":\"" + name + "\"");
            if (idx < 0) return scores;
            int scoreIdx = ctx.indexOf("\"score\":", idx);
            if (scoreIdx < 0) return scores;
            int end = ctx.indexOf(",", scoreIdx);
            if (end < 0) end = ctx.indexOf("}", scoreIdx);
            scores.add(Double.parseDouble(ctx.substring(scoreIdx + 8, end).trim()));
        } catch (Exception ignored) {}
        return scores;
    }
}
