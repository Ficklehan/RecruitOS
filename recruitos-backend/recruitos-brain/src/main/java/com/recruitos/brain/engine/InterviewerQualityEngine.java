package com.recruitos.brain.engine;

import com.recruitos.brain.domain.InterviewerQuality;
import org.springframework.stereotype.Component;
import java.util.*;

/** 触点7引擎：面试官质量治理 — 含CUSUM时间序列漂移检测 */
@Component
public class InterviewerQualityEngine {

    /** CUSUM 漂移检测的灵敏度参数 */
    private static final double CUSUM_K = 0.5;   // 允许的偏移量
    private static final double CUSUM_H = 4.0;   // 决策阈值

    public InterviewerQuality assess(Long interviewerId, String interviewerName, Map<String, Object> metrics) {
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
        double leniency = (avgScore != null && globalAvg != null && globalAvg > 0) ? avgScore / globalAvg : 1.0;
        iq.setLeniencyIndex(leniency);

        // 偏差标签
        List<InterviewerQuality.BiasTag> tags = new ArrayList<>();
        if (Math.abs(leniency - 1.0) > 0.25) {
            InterviewerQuality.BiasTag tag = new InterviewerQuality.BiasTag();
            tag.setTag(leniency > 1.0 ? "宽松偏差" : "严格偏差");
            tag.setDescription("评分系统性" + (leniency > 1.0 ? "偏高" : "偏低") + String.format("%.0f%%", Math.abs(leniency - 1) * 100));
            tag.setSeverity(Math.min(1.0, Math.abs(leniency - 1)));
            tags.add(tag);
        }
        iq.setBiasTags(tags);

        // ---- CUSUM 时间序列漂移检测 ----
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> rawHistory = (List<Map<String, Object>>) metrics.getOrDefault("recentEvalHistory",
            Collections.emptyList());
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
        iq.setQualityLevel(qualityScore >= 80 ? "EXCELLENT" : qualityScore >= 60 ? "GOOD" : "NEEDS_IMPROVEMENT");
        iq.setNeedsRecertification(qualityScore < 50 || (cusum.hasDrift && qualityScore < 65));

        // Coaching建议
        iq.setCoachingSuggestions(buildCoaching(leniency, accuracy, cusum));

        return iq;
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

            if (cusumPos > maxCusumPos) {
                maxCusumPos = cusumPos;
                driftPosIdx = i;
            }
            if (cusumNeg > maxCusumNeg) {
                maxCusumNeg = cusumNeg;
                driftNegIdx = i;
            }
        }

        boolean hasUpwardDrift = maxCusumPos > CUSUM_H;
        boolean hasDownwardDrift = maxCusumNeg > CUSUM_H;

        if (hasUpwardDrift) {
            return new CusumResult(true, "向上", driftPosIdx + 1, maxCusumPos);
        } else if (hasDownwardDrift) {
            return new CusumResult(true, "向下", driftNegIdx + 1, maxCusumNeg);
        }
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

        // 将历史评分等分成3个时间段
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
        if (cusum.hasDrift) {
            coaching.add("检测到评分" + cusum.driftDirection + "漂移，建议回顾最近评价标准是否变化");
        }
        if (coaching.isEmpty()) coaching.add("质量表现良好，继续保持当前面试标准");
        else coaching.add(0, "CUSUM漂移检测" + (cusum.hasDrift ? "发现异常" : "正常"));
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
