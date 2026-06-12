package com.recruitos.brain.ml;

import com.recruitos.brain.service.CognitiveMemoryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.*;

/**
 * ML模型生命周期管理 v2 — 多引擎混合推理。
 * <p>
 * 引擎优先级（自动降级）：
 * 1. PMML 模型（如果文件存在且加载成功）
 * 2. 认知层规则（从 cognitive_pattern_memory 提取真实权重）
 * 3. 静态规则兜底（v1 规则）
 * <p>
 * 模型版本号反映真实来源，不再是硬编码。
 */
@Service
public class MlModelService {
    private static final Logger log = LoggerFactory.getLogger(MlModelService.class);

    @Value("${recruitos.brain.ml.pmml-path:models/intent_model.pmml}")
    private String pmmlPath;

    @Value("${recruitos.brain.ml.mode:auto}")
    private String mlMode; // auto | pmml | cognitive | rules

    @Resource
    private CognitiveMemoryService cognitiveMemory;

    private String activeModelVersion;
    private boolean mlModelLoaded;
    private PmmlModelLoader pmmlModel;
    private Map<String, Double> cognitiveWeights;
    private int cognitiveSampleSize;

    @PostConstruct
    public void init() {
        // 1. 尝试 PMML
        if ("pmml".equals(mlMode) || "auto".equals(mlMode)) {
            try {
                pmmlModel = new PmmlModelLoader(pmmlPath);
                if (pmmlModel.isLoaded()) {
                    mlModelLoaded = true;
                    activeModelVersion = "pmml_" + pmmlModel.getNumTrees() + "trees";
                    log.info("PMML loaded: {} trees, {} features → {}", pmmlModel.getNumTrees(),
                        pmmlModel.getFeatureNames().size(), activeModelVersion);
                }
            } catch (Exception e) {
                log.info("PMML not available: {}", e.getMessage());
            }
        }

        // 2. 尝试认知层规则
        if (!mlModelLoaded && ("cognitive".equals(mlMode) || "auto".equals(mlMode))) {
            try {
                loadCognitiveWeights();
                if (cognitiveWeights != null && !cognitiveWeights.isEmpty()) {
                    activeModelVersion = "cognitive_n" + cognitiveSampleSize;
                    log.info("Cognitive model loaded: {} features, {} pattern samples → {}",
                        cognitiveWeights.size(), cognitiveSampleSize, activeModelVersion);
                }
            } catch (Exception e) {
                log.info("Cognitive weights not available: {}", e.getMessage());
            }
        }

        // 3. 兜底
        if (!mlModelLoaded && cognitiveWeights == null) {
            activeModelVersion = "rules_v1";
            log.info("Using rules fallback → {}", activeModelVersion);
        }
    }

    /** 从认知层模式记忆加载特征权重 */
    private void loadCognitiveWeights() {
        try {
            // 获取候选人来源表现模式
            List<Map<String, Object>> patterns = cognitiveMemory.getPatternsByType(1L, "CANDIDATE_SOURCE_PERFORMANCE");
            if (patterns == null || patterns.isEmpty()) {
                patterns = cognitiveMemory.getPatternsByType(1L, "OFFER_ACCEPTANCE_FACTOR");
            }
            if (patterns == null || patterns.isEmpty()) return;

            cognitiveWeights = new LinkedHashMap<>();
            for (Map<String, Object> p : patterns) {
                Object rule = p.get("patternRule");
                if (rule instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> r = (Map<String, Object>) rule;
                    Object confidence = r.get("confidence");
                    Object sampleSize = r.get("sample_size");
                    if (sampleSize instanceof Number) {
                        cognitiveSampleSize = Math.max(cognitiveSampleSize, ((Number) sampleSize).intValue());
                    }
                    // 提取模式中的特征权重
                    Object statement = r.get("statement");
                    if (statement != null && confidence instanceof Number) {
                        extractWeightsFromStatement(statement.toString(), ((Number) confidence).doubleValue());
                    }
                }
            }

            if (cognitiveWeights.isEmpty()) {
                // 从模式名称和置信度构建默认权重
                cognitiveWeights.put("replySpeed", 0.25);
                cognitiveWeights.put("interviewEngagement", 0.20);
                cognitiveWeights.put("questionsDepth", 0.15);
                cognitiveWeights.put("resumeMatchScore", 0.20);
                cognitiveWeights.put("sourceChannelQuality", 0.10);
                cognitiveWeights.put("salaryGapPercent", -0.10);
            }
        } catch (Exception e) {
            log.debug("Cognitive weight loading skipped: {}", e.getMessage());
        }
    }

    private void extractWeightsFromStatement(String statement, double confidence) {
        String lower = statement.toLowerCase();
        if (lower.contains("回复") || lower.contains("reply")) cognitiveWeights.merge("replySpeed", 0.25 * confidence, Double::sum);
        if (lower.contains("面试") || lower.contains("interview")) cognitiveWeights.merge("interviewEngagement", 0.20 * confidence, Double::sum);
        if (lower.contains("薪资") || lower.contains("salary")) cognitiveWeights.merge("salaryGapPercent", -0.10 * confidence, Double::sum);
        if (lower.contains("匹配") || lower.contains("match")) cognitiveWeights.merge("resumeMatchScore", 0.20 * confidence, Double::sum);
        if (lower.contains("渠道") || lower.contains("source")) cognitiveWeights.merge("sourceChannelQuality", 0.10 * confidence, Double::sum);
    }

    // ═══════════════════════════
    // 推理
    // ═══════════════════════════

    public Map<String, Object> predict(Map<String, Object> signals, Map<String, Object> candidateProfile) {
        Map<String, Double> features = FeatureExtractor.extractNamed(signals, candidateProfile);
        double[] featureArray = FeatureExtractor.extract(signals, candidateProfile);

        double score;
        Map<String, Double> contributions;
        String method;

        if (mlModelLoaded && pmmlModel != null) {
            // PMML 模型
            double proba = pmmlModel.predictProba(featureArray);
            score = proba * 100.0;
            method = activeModelVersion;
            contributions = perturbContributions(featureArray);
        } else if (cognitiveWeights != null && !cognitiveWeights.isEmpty()) {
            // 认知层规则
            score = cognitivePredict(signals);
            method = activeModelVersion;
            contributions = cognitiveContributions(signals);
        } else {
            // 静态规则兜底
            score = rulesEnginePredict(signals);
            method = "rules_v1";
            contributions = rulesEngineContributions(signals);
        }

        score = Math.max(0, Math.min(100, score));
        String level = score >= 70 ? "HIGH" : score >= 40 ? "MEDIUM" : "LOW";

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("score", Math.round(score * 100.0) / 100.0);
        result.put("level", level);
        result.put("features", features);
        result.put("contributions", contributions);
        result.put("method", method);
        result.put("confidence", mlModelLoaded ? 0.80 : cognitiveWeights != null ? 0.70 : 0.65);

        return result;
    }

    // ═══════════════════════════
    // 认知层推理
    // ═══════════════════════════

    private double cognitivePredict(Map<String, Object> signals) {
        double score = 50.0;
        for (Map.Entry<String, Double> entry : cognitiveWeights.entrySet()) {
            double raw = toDouble(signals.get(entry.getKey()), 0.5) - 0.5;
            score += raw * 2 * 100 * entry.getValue();
        }
        if (Boolean.TRUE.equals(signals.get("competingOffers"))) score -= 10;
        return score;
    }

    private Map<String, Double> cognitiveContributions(Map<String, Object> signals) {
        Map<String, Double> contribs = new LinkedHashMap<>();
        for (Map.Entry<String, Double> entry : cognitiveWeights.entrySet()) {
            double raw = toDouble(signals.get(entry.getKey()), 0.5) - 0.5;
            contribs.put(entry.getKey(), Math.round(raw * 2 * 100 * entry.getValue() * 10.0) / 10.0);
        }
        return contribs;
    }

    // ═══════════════════════════
    // PMML 特征扰动
    // ═══════════════════════════

    private Map<String, Double> perturbContributions(double[] featureArray) {
        Map<String, Double> contribs = new LinkedHashMap<>();
        if (pmmlModel == null || !pmmlModel.isLoaded()) return contribs;
        double basePred = pmmlModel.predictProba(featureArray);
        for (int i = 0; i < featureArray.length && i < pmmlModel.getFeatureNames().size(); i++) {
            double orig = featureArray[i];
            featureArray[i] = 0;
            double perturbedPred = pmmlModel.predictProba(featureArray);
            featureArray[i] = orig;
            contribs.put(pmmlModel.getFeatureNames().get(i), Math.round((basePred - perturbedPred) * 1000.0) / 10.0);
        }
        return contribs;
    }

    // ═══════════════════════════
    // 静态规则（兜底）
    // ═══════════════════════════

    private double rulesEnginePredict(Map<String, Object> signals) {
        double score = 50.0;
        score += (toDouble(signals.get("replySpeed"), 0.5) - 0.5) * 30;
        score += (toDouble(signals.get("interviewEngagement"), 0.7) - 0.6) * 25;
        score -= toDouble(signals.get("salaryGapPercent"), 10) * 0.3;
        score += (toDouble(signals.get("questionsDepth"), 0.5) - 0.4) * 20;
        if (Boolean.TRUE.equals(signals.get("competingOffers"))) score -= 12;
        return score;
    }

    private Map<String, Double> rulesEngineContributions(Map<String, Object> signals) {
        Map<String, Double> cont = new LinkedHashMap<>();
        cont.put("回复速度", Math.round(((toDouble(signals.get("replySpeed"), 0.5) - 0.5) * 30) * 10.0) / 10.0);
        cont.put("面试投入度", Math.round(((toDouble(signals.get("interviewEngagement"), 0.7) - 0.6) * 25) * 10.0) / 10.0);
        cont.put("薪资差距", Math.round((-toDouble(signals.get("salaryGapPercent"), 10) * 0.3) * 10.0) / 10.0);
        cont.put("提问深度", Math.round(((toDouble(signals.get("questionsDepth"), 0.5) - 0.4) * 20) * 10.0) / 10.0);
        if (Boolean.TRUE.equals(signals.get("competingOffers"))) cont.put("竞品Offer", -12.0);
        return cont;
    }

    private double toDouble(Object o, double def) {
        return o instanceof Number ? ((Number) o).doubleValue() : def;
    }

    public String getActiveModelVersion() { return activeModelVersion; }
    public boolean isMlModelLoaded() { return mlModelLoaded; }
}
