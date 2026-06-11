package com.recruitos.brain.engine;

import java.util.*;

/**
 * 校准统计分析 — Cohen's Kappa、ICC、评分分布对比。
 * 纯数学实现，无外部依赖。
 */
public class CalibrationStatistics {

    /**
     * Cohen's Kappa — 两个评分者之间的一致性。
     * @param rater1 评分者1的评分列表
     * @param rater2 评分者2的评分列表
     * @param categories 可能的评分类别数（如 5）
     * @return Kappa 值 (-1 到 1)
     */
    public static double cohensKappa(int[] rater1, int[] rater2, int categories) {
        if (rater1.length != rater2.length || rater1.length == 0) return 0;

        int n = rater1.length;
        int[][] matrix = new int[categories][categories];

        // 构建混淆矩阵
        for (int i = 0; i < n; i++) {
            int a = Math.min(rater1[i] - 1, categories - 1);
            int b = Math.min(rater2[i] - 1, categories - 1);
            if (a >= 0 && b >= 0) matrix[a][b]++;
        }

        // 观察一致率 Po
        double po = 0;
        for (int i = 0; i < categories; i++) po += matrix[i][i];
        po /= n;

        // 期望一致率 Pe
        double pe = 0;
        for (int i = 0; i < categories; i++) {
            double rowSum = 0, colSum = 0;
            for (int j = 0; j < categories; j++) {
                rowSum += matrix[i][j];
                colSum += matrix[j][i];
            }
            pe += (rowSum / n) * (colSum / n);
        }

        if (Math.abs(1 - pe) < 1e-10) return 1.0;
        return (po - pe) / (1 - pe);
    }

    /**
     * 多评分者 ICC（Intraclass Correlation Coefficient）— 双向随机效应，绝对一致。
     * 基于 Shrout-Fleiss ICC(2,1) 公式。
     * @param scores 评分矩阵 [评分者][评分项]
     * @return ICC 值
     */
    public static double icc(double[][] scores) {
        int k = scores.length;      // 评分者数
        int n = scores[0].length;   // 评分项数

        if (k < 2 || n < 2) return 0;

        // 总均值
        double grandMean = 0;
        for (double[] row : scores) for (double v : row) grandMean += v;
        grandMean /= (k * n);

        // 评分项间均方 MSR
        double msr = 0;
        for (int j = 0; j < n; j++) {
            double rowMean = 0;
            for (int i = 0; i < k; i++) rowMean += scores[i][j];
            rowMean /= k;
            msr += k * Math.pow(rowMean - grandMean, 2);
        }
        msr /= (n - 1);

        // 评分者间均方 MSC
        double msc = 0;
        for (int i = 0; i < k; i++) {
            double colMean = 0;
            for (int j = 0; j < n; j++) colMean += scores[i][j];
            colMean /= n;
            msc += n * Math.pow(colMean - grandMean, 2);
        }
        msc /= (k - 1);

        // 残差均方 MSE
        double mse = 0;
        for (int i = 0; i < k; i++)
            for (int j = 0; j < n; j++)
                mse += Math.pow(scores[i][j] - grandMean, 2);
        mse = (mse - (n-1)*msr - (k-1)*msc) / ((n-1)*(k-1));
        if (mse < 0) mse = 0;

        if (Math.abs(msr + msc + mse) < 1e-10) return 0;
        return (msr - mse) / (msr + (k - 1) * mse + (k * (msc - mse) / n));
    }

    /**
     * 判断评分差异类型：信号差异 vs 标准差异。
     * @param score1 评分者1分数
     * @param score2 评分者2分数
     * @param evidence1 评分者1的证据文本
     * @param evidence2 评分者2的证据文本
     * @return "SIGNAL_DIFF" | "STANDARD_DIFF" | "UNKNOWN"
     */
    public static String classifyDifference(int score1, int score2, String evidence1, String evidence2) {
        int gap = Math.abs(score1 - score2);
        if (gap < 2) return "CONSISTENT";

        // 简单启发式：如果两个面试官的 evidence 关注不同方面 → 信号差异
        if (evidence1 != null && evidence2 != null) {
            // 提取证据关键词（简化版）
            Set<String> words1 = extractKeywords(evidence1);
            Set<String> words2 = extractKeywords(evidence2);

            // 计算重叠度
            Set<String> intersection = new HashSet<>(words1);
            intersection.retainAll(words2);
            double overlap = (double) intersection.size() / Math.max(words1.size(), words2.size());

            if (overlap < 0.3) return "SIGNAL_DIFF";  // 关注不同方面 → 信号差异
            if (overlap > 0.7) return "STANDARD_DIFF";  // 关注相同但评分不同 → 标准差异
        }

        return gap >= 3 ? "STANDARD_DIFF" : "SIGNAL_DIFF";
    }

    private static Set<String> extractKeywords(String text) {
        Set<String> words = new HashSet<>();
        for (String w : text.replaceAll("[，。！？,.!?]", " ").split("\\s+")) {
            if (w.length() >= 2) words.add(w);
        }
        return words;
    }

    /**
     * 计算宽松指数：评分者均值 / 全局均值。
     */
    public static double leniencyIndex(double raterMean, double globalMean) {
        if (globalMean == 0) return 1.0;
        return raterMean / globalMean;
    }

    /**
     * Kappa 解读
     */
    public static String interpretKappa(double kappa) {
        if (kappa > 0.75) return "优秀一致性";
        if (kappa > 0.60) return "良好一致性";
        if (kappa > 0.40) return "中等一致性";
        return "一致性差，需要校准";
    }

    /**
     * ICC 解读
     */
    public static String interpretIcc(double icc) {
        if (icc > 0.90) return "优秀";
        if (icc > 0.75) return "良好";
        if (icc > 0.50) return "中等";
        return "差";
    }
}
