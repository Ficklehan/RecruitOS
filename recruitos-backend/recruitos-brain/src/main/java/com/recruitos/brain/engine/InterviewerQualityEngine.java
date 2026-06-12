package com.recruitos.brain.engine;

import com.recruitos.brain.domain.InterviewerQuality;
import com.recruitos.brain.service.CognitiveMemoryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.*;

/** 触点7引擎：面试官质量治理 — 含CUSUM时间序列漂移检测 + 认知层观察写入 */
@Component
public class InterviewerQualityEngine {
    private static final Logger log = LoggerFactory.getLogger(InterviewerQualityEngine.class);

    /** CUSUM 漂移检测的灵敏度参数 */
    private static final double CUSUM_K = 0.5;   // 允许的偏移量
    private static final double CUSUM_H = 4.0;   // 决策阈值

    @Resource
    private CognitiveMemoryService cognitiveMemory;

    /**
     * 评估面试官质量，将CUSUM漂移等发现自动写入 cognitive_observation 和 cognitive_event_memory，
     * 从而使AI能主动提醒"面试官X评分在漂移"。
     */
    public InterviewerQuality assess(Long tenantId, Long interviewerId, String interviewerName,
                                      Map<String, Object> metrics) {
        InterviewerQuality iq = new InterviewerQuality();
        iq.setInterviewerId(interviewerId);
        iq.setInterviewerName(interviewerName);
        Integer totalEvals = toInt(metrics.get("totalEvaluations"));
        iq.setTotalEvaluations(totalEvals != null ? totalEvals : 0);

        if (iq.getTotalEvaluations() < 5) {
            iq.setQualityScore(0.0);
            iq.setQualityLevel("INSUFFICIENT_DATA");
            iq.setCoachingSuggestions(Arrays.asList("需完成至少5场面试评价后才能生成质量报告"));
            return iq;
        }

        // 宽松指数
        Double avgScore = toDouble(metrics.get("avgScore"));
        Double globalAvg = toDouble(metrics.get("globalAvgScore"));
        iq.setAvgScore(avgScore);
        iq.setGlobalAvgScore(globalAvg);
        double leniency = (avgScore != null && globalAvg != null && globalAvg > 0)
            ? avgScore / globalAvg : 1.0;
        iq.setLeniencyIndex(leniency);

        // 偏差标签
        List<InterviewerQuality.BiasTag> tags = new ArrayList<>();
        if (Math.abs(leniency - 1.0) > 0.25) {
            InterviewerQuality.BiasTag tag = new InterviewerQuality.BiasTag();
            tag.setTag(leniency > 1.0 ? "宽松偏差" : "严格偏差");
            tag.setDescription("评分系统性" + (leniency > 1.0 ? "偏高" : "偏低")
                + String.format("%.0f%%", Math.abs(leniency - 1) * 100));
            tag.setSeverity(Math.min(1.0, Math.abs(leniency - 1)));
            tags.add(tag);
        }
        iq.setBiasTags(tags);

        // ---- CUSUM 时间序列漂移检测 ----
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> rawHistory = (List<Map<String, Object>>) metrics.getOrDefault(
            "recentEvalHistory", Collections.emptyList());
        List<Double> historyScores = new ArrayList<>();
        if (rawHistory != null) {
            for (Map<String, Object> entry : rawHistory) {
                Double s = toDouble(entry.get("score"));
                if (s != null) historyScores.add(s);
            }
        }

        CusumResult cusum = detectCusumDrift(historyScores, globalAvg != null ? globalAvg : 3.0);

        // 趋势数据
        List<InterviewerQuality.TrendPoint> trend = buildTrend(historyScores);
        iq.setTrend(trend);

        // 漂移警告标签
        if (cusum.hasDrift) {
            InterviewerQuality.BiasTag driftTag = new InterviewerQuality.BiasTag();
            driftTag.setTag(cusum.driftDirection + "漂移");
            driftTag.setDescription("CUSUM检测到评分" + cusum.driftDirection
                + "漂移（自第" + cusum.driftStartIndex + "次评价起）");
            driftTag.setSeverity(Math.min(1.0, cusum.maxCusum / CUSUM_H));
            tags.add(driftTag);
        }

        // 预测准确度
        Double accuracy = toDouble(metrics.get("predictionAccuracy"));
        iq.setPredictionAccuracy(accuracy != null ? accuracy : 0.65);

        // 质量分
        double qualityScore = Math.max(0, Math.min(100,
            70 + (1 - Math.abs(leniency - 1)) * 40
            + (accuracy != null ? (accuracy - 0.5) * 50 : 0)
            - (cusum.hasDrift ? 15 : 0)));
        iq.setQualityScore(qualityScore);
        iq.setQualityLevel(qualityScore >= 80 ? "EXCELLENT"
            : qualityScore >= 60 ? "GOOD" : "NEEDS_IMPROVEMENT");
        iq.setNeedsRecertification(qualityScore < 50
            || (cusum.hasDrift && qualityScore < 65));

        // Coaching建议
        iq.setCoachingSuggestions(buildCoaching(leniency, accuracy, cusum));

        // ---- 写入认知层：持久化到 cognitive_event_memory + cognitive_observation ----
        writeToCognitiveLayer(tenantId, interviewerId, interviewerName, iq, cusum, metrics);

        return iq;
    }

    /**
     * 将面试官质量评估结果写入认知层。
     * - 事件记录到 cognitive_event_memory，供模式挖掘使用
     * - CUSUM 漂移 / 低质量分 / 极端偏差写入 cognitive_observation，供 AI 主动提醒
     */
    private void writeToCognitiveLayer(Long tenantId, Long interviewerId, String interviewerName,
                                        InterviewerQuality iq, CusumResult cusum,
                                        Map<String, Object> metrics) {
        try {
            // 1. 记录评估事件到 cognitive_event_memory
            Map<String, Object> eventContext = new LinkedHashMap<>();
            eventContext.put("qualityScore", iq.getQualityScore());
            eventContext.put("qualityLevel", iq.getQualityLevel());
            eventContext.put("leniencyIndex", iq.getLeniencyIndex());
            eventContext.put("avgScore", iq.getAvgScore());
            eventContext.put("globalAvgScore", iq.getGlobalAvgScore());
            eventContext.put("predictionAccuracy", iq.getPredictionAccuracy());
            eventContext.put("totalEvaluations", iq.getTotalEvaluations());
            eventContext.put("hasDrift", cusum.hasDrift);
            eventContext.put("driftDirection", cusum.driftDirection);
            eventContext.put("cusumValue", cusum.maxCusum);
            eventContext.put("needsRecertification", iq.isNeedsRecertification());
            eventContext.put("biasTags", iq.getBiasTags() != null
                ? iq.getBiasTags().stream().map(InterviewerQuality.BiasTag::getTag).toList()
                : Collections.emptyList());

            cognitiveMemory.recordEvent(tenantId, "INTERVIEWER_ASSESSMENT",
                "INTERVIEWER", interviewerId, eventContext,
                iq.getQualityLevel(), cusum.hasDrift ? "DRIFT_DETECTED" : "STABLE");

            // 2. CUSUM 漂移 → ALERT / WARNING 观察
            if (cusum.hasDrift) {
                writeDriftObservation(tenantId, interviewerId, interviewerName, iq, cusum);
            }

            // 3. 质量分过低 → WARNING 观察
            if (iq.getQualityScore() < 60) {
                writeLowQualityObservation(tenantId, interviewerId, interviewerName, iq);
            }

            // 4. 极端偏差 → INSIGHT 观察
            double leniency = iq.getLeniencyIndex();
            if (leniency > 1.3 || (leniency > 0 && leniency < 0.7)) {
                writeBiasObservation(tenantId, interviewerId, interviewerName, iq);
            }
        } catch (Exception e) {
            log.error("Failed to write interviewer quality to cognitive layer: interviewerId={}",
                interviewerId, e);
        }
    }

    /**
     * CUSUM 漂移 → ALERT 观察。
     * 当评分出现统计显著漂移时，AI 应主动提醒用户。
     */
    private void writeDriftObservation(Long tenantId, Long interviewerId, String interviewerName,
                                        InterviewerQuality iq, CusumResult cusum) {
        String title = String.format("[%s] 面试官 %s 评分出现%s漂移",
            iq.getQualityScore() < 50 ? "CRITICAL" : "WARNING",
            interviewerName,
            "向上".equals(cusum.driftDirection) ? "向上" : "向下");

        String body = String.format(
            "CUSUM检测发现面试官 %s 的评分自第 %d 次评价起出现统计显著的%s漂移"
            + "（CUSUM值=%.1f，阈值=%.1f）。\n"
            + "当前质量分=%.1f（%s），宽松指数=%.2f，共计 %d 次评价。\n"
            + "建议立即检查该面试官最近的评分标准是否发生变化，必要时安排校准会谈。",
            interviewerName, cusum.driftStartIndex,
            "向上".equals(cusum.driftDirection) ? "向上（评分越来越宽松）" : "向下（评分越来越严格）",
            cusum.maxCusum, CUSUM_H,
            iq.getQualityScore(), iq.getQualityLevel(),
            iq.getLeniencyIndex(), iq.getTotalEvaluations());

        String severity = iq.getQualityScore() < 50 ? "CRITICAL" : "WARNING";

        List<Map<String, Object>> relatedObjects = List.of(
            Map.of("type", "INTERVIEWER", "id", interviewerId, "name", interviewerName)
        );
        Map<String, Object> suggestedAction = Map.of(
            "text", "查看面试官 " + interviewerName + " 的详细评估报告并安排校准会谈",
            "action_type", "NAVIGATE",
            "params", Map.of("route", "/interviewer-quality/" + interviewerId)
        );

        cognitiveMemory.createObservation(tenantId, "ALERT", severity,
            title, body, relatedObjects, suggestedAction,
            LocalDateTime.now().plusDays(30));
    }

    /**
     * 质量分过低 → WARNING 观察。
     * 提醒用户安排Bar Raiser影子面试或强制复训。
     */
    private void writeLowQualityObservation(Long tenantId, Long interviewerId, String interviewerName,
                                             InterviewerQuality iq) {
        String title = String.format("面试官 %s 质量评分过低（%.0f）",
            interviewerName, iq.getQualityScore());
        String body = String.format(
            "面试官 %s 的综合质量评分为 %.1f（等级：%s），低于60分阈值。\n"
            + "宽松指数=%.2f，预测准确度=%.0f%%，共计 %d 次评价。\n"
            + "建议：(1) 安排Bar Raiser进行3场影子面试；(2) 参加校准会培训；"
            + "(3) 暂时限制该面试官评估关键岗位候选人。",
            interviewerName, iq.getQualityScore(), iq.getQualityLevel(),
            iq.getLeniencyIndex(),
            iq.getPredictionAccuracy() != null ? iq.getPredictionAccuracy() * 100 : 0,
            iq.getTotalEvaluations());

        List<Map<String, Object>> relatedObjects = List.of(
            Map.of("type", "INTERVIEWER", "id", interviewerId, "name", interviewerName)
        );
        Map<String, Object> suggestedAction = Map.of(
            "text", "为该面试官安排影子面试及校准培训",
            "action_type", "CREATE_TASK",
            "params", Map.of("taskType", "RECERTIFICATION", "interviewerId", interviewerId)
        );

        cognitiveMemory.createObservation(tenantId, "ALERT", "WARNING",
            title, body, relatedObjects, suggestedAction,
            LocalDateTime.now().plusDays(14));
    }

    /**
     * 系统偏差 → INSIGHT 观察。
     * 当面试官的宽松/严格指数严重偏离时，提示配置行为锚点评分表。
     */
    private void writeBiasObservation(Long tenantId, Long interviewerId, String interviewerName,
                                       InterviewerQuality iq) {
        double leniency = iq.getLeniencyIndex();
        String biasType = leniency > 1.3 ? "宽松偏差" : "严格偏差";
        String title = String.format("面试官 %s 存在系统性%s", interviewerName, biasType);
        String body = String.format(
            "面试官 %s 的宽松指数为 %.2f（正常范围 0.7-1.3），偏差方向：%s。\n"
            + "共计 %d 次评价，平均评分 %.2f vs 全局平均 %.2f。\n"
            + "建议为该面试官配置行为锚点评分表，减少主观印象对评分的干扰。",
            interviewerName, leniency, biasType,
            iq.getTotalEvaluations(), iq.getAvgScore(), iq.getGlobalAvgScore());

        List<Map<String, Object>> relatedObjects = List.of(
            Map.of("type", "INTERVIEWER", "id", interviewerId, "name", interviewerName)
        );
        Map<String, Object> suggestedAction = Map.of(
            "text", "为面试官 " + interviewerName + " 配置行为锚点评分表",
            "action_type", "CONFIGURE",
            "params", Map.of("configType", "SCORING_RUBRIC", "interviewerId", interviewerId)
        );

        cognitiveMemory.createObservation(tenantId, "INSIGHT", "INFO",
            title, body, relatedObjects, suggestedAction,
            LocalDateTime.now().plusDays(60));
    }

    /**
     * CUSUM（累积和）漂移检测。
     * 检测评分均值是否发生统计显著的向上或向下漂移。
     */
    private CusumResult detectCusumDrift(List<Double> scores, double targetMean) {
        if (scores == null || scores.size() < 8) return new CusumResult(false, null, 0, 0);

        double cusumPos = 0, cusumNeg = 0;
        double maxCusumPos = 0, maxCusumNeg = 0;
        int driftPosIdx = -1, driftNegIdx = -1;

        for (int i = 0; i < scores.size(); i++) {
            double deviation = scores.get(i) - targetMean;
            cusumPos = Math.max(0, cusumPos + deviation - CUSUM_K);
            cusumNeg = Math.max(0, cusumNeg - deviation - CUSUM_K);

            if (cusumPos > maxCusumPos) { maxCusumPos = cusumPos; driftPosIdx = i; }
            if (cusumNeg > maxCusumNeg) { maxCusumNeg = cusumNeg; driftNegIdx = i; }
        }

        boolean hasUpwardDrift = maxCusumPos > CUSUM_H;
        boolean hasDownwardDrift = maxCusumNeg > CUSUM_H;

        if (hasUpwardDrift)
            return new CusumResult(true, "向上", driftPosIdx + 1, maxCusumPos);
        if (hasDownwardDrift)
            return new CusumResult(true, "向下", driftNegIdx + 1, maxCusumNeg);
        return new CusumResult(false, null, 0, Math.max(maxCusumPos, maxCusumNeg));
    }

    /**
     * 构建时间序列趋势点。
     */
    private List<InterviewerQuality.TrendPoint> buildTrend(List<Double> scores) {
        List<InterviewerQuality.TrendPoint> trend = new ArrayList<>();
        if (scores == null || scores.isEmpty()) {
            InterviewerQuality.TrendPoint tp = new InterviewerQuality.TrendPoint();
            tp.setPeriod("最近1月"); tp.setAvgScore(3.0); tp.setLeniency(1.0);
            trend.add(tp);
            return trend;
        }

        int n = scores.size();
        int chunkSize = Math.max(1, n / 3);
        String[] periods = {"早期", "中期", "近期"};

        for (int p = 0; p < 3 && p * chunkSize < n; p++) {
            int start = p * chunkSize;
            int end = Math.min((p + 1) * chunkSize, n);
            double sum = 0;
            for (int i = start; i < end; i++) sum += scores.get(i);
            double avg = sum / (end - start);

            InterviewerQuality.TrendPoint tp = new InterviewerQuality.TrendPoint();
            tp.setPeriod(periods[p]);
            tp.setAvgScore(Math.round(avg * 100.0) / 100.0);
            tp.setLeniency(Math.round((avg / 3.0) * 100.0) / 100.0);
            trend.add(tp);
        }
        return trend;
    }

    /**
     * 生成 Coaching 建议。
     */
    private List<String> buildCoaching(double leniency, Double accuracy, CusumResult cusum) {
        List<String> coaching = new ArrayList<>();
        if (leniency > 1.2) coaching.add("建议使用行为锚点评分表，减少'感觉不错'型评分");
        if (leniency < 0.8) coaching.add("建议在评分前先列出候选人的优势，避免过度聚焦缺陷");
        if (accuracy != null && accuracy < 0.6) coaching.add("建议参加校准会培训，提升面试预测准确性");
        if (cusum.hasDrift)
            coaching.add("检测到评分" + cusum.driftDirection + "漂移，建议回顾最近评价标准是否变化");
        if (coaching.isEmpty()) coaching.add("质量表现良好，继续保持当前面试标准");
        else coaching.add(0, cusum.hasDrift ? "CUSUM漂移检测：发现异常" : "CUSUM漂移检测：正常");
        return coaching;
    }

    private Integer toInt(Object o) { return o instanceof Number ? ((Number) o).intValue() : null; }
    private Double toDouble(Object o) { return o instanceof Number ? ((Number) o).doubleValue() : null; }

    /** CUSUM 检测结果 */
    static class CusumResult {
        final boolean hasDrift;
        final String driftDirection; // "向上" / "向下"
        final int driftStartIndex;   // 漂移起始索引（1-based）
        final double maxCusum;

        CusumResult(boolean hasDrift, String driftDirection, int driftStartIndex, double maxCusum) {
            this.hasDrift = hasDrift;
            this.driftDirection = driftDirection;
            this.driftStartIndex = driftStartIndex;
            this.maxCusum = maxCusum;
        }
    }
}
