package com.recruitos.brain.engine;

import com.recruitos.brain.domain.CalibrationSession;
import org.springframework.stereotype.Component;
import java.util.*;

@Component
public class CalibrationEngine {

    public CalibrationSession analyze(Long jobId, String jobTitle, Long candidateId, String candidateName,
                                       Map<String, Map<String, Object>> evaluatorScores) {
        CalibrationSession session = new CalibrationSession();
        session.setJobId(jobId);
        session.setJobTitle(jobTitle);
        session.setCandidateId(candidateId);
        session.setCandidateName(candidateName);

        List<String> allDims = Arrays.asList("Product Sense", "Execution", "Leadership", "Culture", "Craft");

        // 构建对比矩阵
        List<CalibrationSession.EvaluatorComparison> comparisons = new ArrayList<>();
        Map<String, List<Integer>> dimScores = new LinkedHashMap<>();
        Map<String, List<String>> dimEvidences = new LinkedHashMap<>();

        for (Map.Entry<String, Map<String, Object>> entry : evaluatorScores.entrySet()) {
            CalibrationSession.EvaluatorComparison ec = new CalibrationSession.EvaluatorComparison();
            ec.setEvaluatorName(entry.getKey());
            List<CalibrationSession.DimensionScore> scores = new ArrayList<>();
            for (String dim : allDims) {
                CalibrationSession.DimensionScore ds = new CalibrationSession.DimensionScore();
                ds.setDimension(dim);
                Map<String, Object> raw = entry.getValue();
                ds.setScore(toInt(raw.get(dim), 3));
                ds.setEvidence((String) raw.getOrDefault(dim + "_evidence", ""));
                ds.setSignalStrength((String) raw.getOrDefault(dim + "_signal", "Moderate"));
                scores.add(ds);
                dimScores.computeIfAbsent(dim, k -> new ArrayList<>()).add(ds.getScore());
                dimEvidences.computeIfAbsent(dim, k -> new ArrayList<>()).add(ds.getEvidence());
            }
            ec.setScores(scores);
            ec.setOverallVerdict((String) entry.getValue().getOrDefault("overallVerdict", ""));
            comparisons.add(ec);
        }
        session.setComparisons(comparisons);

        // 维度分析 + Kappa 计算
        List<CalibrationSession.Dimension> dimensions = new ArrayList<>();
        List<String> silentDims = new ArrayList<>();
        Map<String, Double> kappaMap = new LinkedHashMap<>();

        for (String dim : allDims) {
            CalibrationSession.Dimension d = new CalibrationSession.Dimension();
            d.setName(dim);
            d.setWeight(0.20);
            List<Integer> scores = dimScores.getOrDefault(dim, Collections.emptyList());
            if (scores.size() >= 2) {
                d.setAvgScore((int) Math.round(scores.stream().mapToInt(Integer::intValue).average().orElse(0)));
                d.setMaxGap(Collections.max(scores) - Collections.min(scores));
                d.setDisputed(d.getMaxGap() >= 2);

                // 计算 Cohen's Kappa（两两配对取平均）
                double kappaSum = 0; int kpairs = 0;
                for (int i = 0; i < scores.size(); i++) {
                    for (int j = i + 1; j < scores.size(); j++) {
                        kappaSum += CalibrationStatistics.cohensKappa(
                            new int[]{scores.get(i)}, new int[]{scores.get(j)}, 5);
                        kpairs++;
                    }
                }
                double avgKappa = kpairs > 0 ? kappaSum / kpairs : 0;
                kappaMap.put(dim, avgKappa);

                if (d.isDisputed()) {
                    // 判断信号差异 vs 标准差异
                    List<String> evids = dimEvidences.getOrDefault(dim, Collections.emptyList());
                    String type = evids.size() >= 2
                        ? CalibrationStatistics.classifyDifference(
                            Collections.min(scores), Collections.max(scores),
                            evids.get(scores.indexOf(Collections.min(scores))),
                            evids.get(scores.indexOf(Collections.max(scores))))
                        : "UNKNOWN";
                    d.setAiRecommendation("信号差异".equals(type)
                        ? "面试官关注了不同信号，建议交换信息后重新评分"
                        : "评分标准不一致（Kappa=" + String.format("%.2f", avgKappa) + "），建议校准标准");
                }
            } else {
                silentDims.add(dim);
            }
            dimensions.add(d);
        }
        session.setDimensions(dimensions);
        session.setSilentDimensions(silentDims);

        // 构建 Cohen's Kappa JSON-style map
        Map<String, Object> kappaJson = new LinkedHashMap<>();
        for (Map.Entry<String, Double> e : kappaMap.entrySet()) {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("kappa", Math.round(e.getValue() * 1000.0) / 1000.0);
            entry.put("interpretation", CalibrationStatistics.interpretKappa(e.getValue()));
            kappaJson.put(e.getKey(), entry);
        }

        // 偏差检测
        session.setBiasDetections(detectBiases(comparisons, dimensions));

        // 主持脚本
        session.setModeratorScript(generateScript(comparisons, dimensions, silentDims, kappaMap));

        double avgAll = dimensions.stream().filter(d -> !silentDims.contains(d.getName()))
                .mapToInt(CalibrationSession.Dimension::getAvgScore).average().orElse(3.0);
        session.setConsensusScore(avgAll);
        session.setHireRecommendation(avgAll >= 4.0 ? "Strong Hire" : avgAll >= 3.0 ? "Hire" : avgAll >= 2.5 ? "Leaning Hire" : "No Hire");
        session.setConfidence(silentDims.isEmpty() ? 0.85 : 0.65);

        return session;
    }

    private List<CalibrationSession.BiasDetection> detectBiases(List<CalibrationSession.EvaluatorComparison> comparisons,
                                                                  List<CalibrationSession.Dimension> dimensions) {
        List<CalibrationSession.BiasDetection> detections = new ArrayList<>();
        if (comparisons.size() < 2) return detections;
        double globalAvg = dimensions.stream().mapToInt(CalibrationSession.Dimension::getAvgScore).average().orElse(3);
        for (CalibrationSession.EvaluatorComparison ec : comparisons) {
            double avg = ec.getScores().stream().mapToInt(CalibrationSession.DimensionScore::getScore).average().orElse(3);
            double leniency = CalibrationStatistics.leniencyIndex(avg, globalAvg);
            if (Math.abs(leniency - 1.0) >= 0.15) {
                CalibrationSession.BiasDetection bd = new CalibrationSession.BiasDetection();
                bd.setEvaluatorName(ec.getEvaluatorName());
                bd.setBiasType(leniency > 1 ? "宽大偏差" : "严格偏差");
                bd.setDescription(ec.getEvaluatorName() + "宽松指数 " + String.format("%.2f", leniency));
                bd.setConfidence(0.75);
                detections.add(bd);
            }
        }
        return detections;
    }

    private String generateScript(List<CalibrationSession.EvaluatorComparison> comparisons,
                                   List<CalibrationSession.Dimension> dimensions, List<String> silentDims,
                                   Map<String, Double> kappaMap) {
        StringBuilder sb = new StringBuilder();
        sb.append("## 校准会议程\n\n");
        dimensions.stream().filter(CalibrationSession.Dimension::isDisputed)
                .sorted((a, b) -> Integer.compare(b.getMaxGap(), a.getMaxGap()))
                .forEach(d -> {
                    double k = kappaMap.getOrDefault(d.getName(), 0.0);
                    sb.append("1. **").append(d.getName()).append("** — 差距").append(d.getMaxGap())
                      .append("分 | Kappa=").append(String.format("%.3f", k))
                      .append(" (").append(CalibrationStatistics.interpretKappa(k)).append(")\n");
                    sb.append("   ").append(d.getAiRecommendation()).append("\n");
                });
        if (!silentDims.isEmpty()) sb.append("\n⚠️ 未覆盖维度：").append(String.join("、", silentDims)).append("\n");
        return sb.toString();
    }

    private int toInt(Object o, int def) { return o instanceof Number ? ((Number) o).intValue() : def; }
}
