package com.recruitos.brain.ml;

import java.util.*;

/**
 * 意向预测特征提取器 — 从原始数据中提取 19 维特征向量。
 * 特征定义与 Python 训练脚本保持一致。
 */
public class FeatureExtractor {

    public static final String[] FEATURE_NAMES = {
        "reply_speed_avg", "reply_speed_trend", "message_length_avg",
        "question_count", "question_quality_score",
        "interview_attendance", "interview_reschedule_cnt", "interview_late_minutes",
        "past_tenure_avg_months", "job_hop_count_3y", "current_company_tier",
        "match_score", "match_score_pct",
        "salary_gap_pct", "salary_percentile",
        "pipeline_stage_days", "stages_completed",
        "competing_offers", "market_hotness"
    };

    /**
     * 从聚合的原始信号中提取特征向量。
     * @param signals BrainDataAggregator 返回的原始信号 Map
     * @param candidateProfile 候选人简历信息
     * @return 19 维 double 数组
     */
    public static double[] extract(Map<String, Object> signals, Map<String, Object> candidateProfile) {
        double[] features = new double[19];

        // 通信特征
        features[0] = toDouble(signals.get("replySpeed"), 0.5) * 24; // 转回小时
        features[1] = toDouble(signals.get("replySpeedTrend"), 0.0);
        features[2] = toDouble(signals.get("messageLengthAvg"), 80.0);
        features[3] = toDouble(signals.get("questionCount"), 3.0);
        features[4] = toDouble(signals.get("questionsDepth"), 0.5); // 兼任 question_quality

        // 行为特征
        features[5] = toDouble(signals.get("interviewEngagement"), 0.7); // 兼任 attendance
        features[6] = toDouble(signals.get("rescheduleCount"), 0.0);
        features[7] = toDouble(signals.get("lateMinutes"), 5.0);

        // 简历特征
        features[8] = toDouble(candidateProfile.get("avgTenureMonths"), 24.0);
        features[9] = toDouble(candidateProfile.get("jobHopCount3y"), 1.0);
        features[10] = companyTier((String) candidateProfile.get("currentCompany"));

        // 匹配特征
        features[11] = toDouble(signals.get("matchScore"), 70.0);
        features[12] = toDouble(signals.get("matchScorePct"), 0.5);

        // 薪资特征
        features[13] = toDouble(signals.get("salaryGapPercent"), 10.0);
        features[14] = toDouble(signals.get("salaryPercentile"), 0.5);

        // 进度特征
        features[15] = toDouble(signals.get("pipelineDays"), 21.0);
        features[16] = toDouble(signals.get("stagesCompleted"), 2.0);

        // 竞品特征
        features[17] = Boolean.TRUE.equals(signals.get("competingOffers")) ? 1.0 : 0.0;
        features[18] = toDouble(signals.get("marketHotness"), 0.3);

        return features;
    }

    public static Map<String, Double> extractNamed(Map<String, Object> signals, Map<String, Object> profile) {
        double[] vec = extract(signals, profile);
        Map<String, Double> map = new LinkedHashMap<>();
        for (int i = 0; i < FEATURE_NAMES.length; i++) {
            map.put(FEATURE_NAMES[i], vec[i]);
        }
        return map;
    }

    private static double toDouble(Object o, double def) {
        if (o instanceof Number) return ((Number) o).doubleValue();
        return def;
    }

    private static double companyTier(String company) {
        if (company == null) return 1;
        String c = company.toLowerCase();
        if (c.contains("bytedance") || c.contains("alibaba") || c.contains("tencent")
            || c.contains("google") || c.contains("meta") || c.contains("apple")
            || c.contains("microsoft") || c.contains("amazon")) return 3;
        if (c.contains("meituan") || c.contains("didi") || c.contains("xiaomi")
            || c.contains("jd") || c.contains("netease") || c.contains("baidu")) return 2;
        return 1;
    }
}
