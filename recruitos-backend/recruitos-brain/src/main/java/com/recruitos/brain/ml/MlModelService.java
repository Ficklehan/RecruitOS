package com.recruitos.brain.ml;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.*;

/**
 * ML模型生命周期管理。
 * 支持：规则引擎兜底 → PMML 模型加载 → 未来在线学习。
 */
@Service
public class MlModelService {
    private static final Logger log = LoggerFactory.getLogger(MlModelService.class);

    @Value("${recruitos.brain.ml.pmml-path:models/intent_model.pmml}")
    private String pmmlPath;

    @Value("${recruitos.brain.ml.mode:rules}")
    private String mlMode; // rules | pmml | hybrid

    private String activeModelVersion = "rules_v1";
    private boolean mlModelLoaded = false;
    private PmmlModelLoader pmmlModel;

    @PostConstruct
    public void init() {
        if ("pmml".equals(mlMode) || "hybrid".equals(mlMode)) {
            try {
                pmmlModel = new PmmlModelLoader(pmmlPath);
                if (pmmlModel.isLoaded()) {
                    mlModelLoaded = true;
                    activeModelVersion = "pmml_v1";
                    log.info("PMML model loaded successfully. {} trees, {} features.",
                        pmmlModel.getNumTrees(), pmmlModel.getFeatureNames().size());
                } else {
                    log.warn("PMML model failed to load, falling back to rules engine.");
                }
            } catch (Exception e) {
                log.warn("PMML model loading error, using rules fallback.", e);
            }
        }
        log.info("MlModelService initialized. Mode: {}, Active model: {}. ML model: {}",
            mlMode, activeModelVersion, mlModelLoaded ? "LOADED" : "NOT_LOADED (rules fallback)");
    }

    /**
     * 预测意向 — 优先 PMML 模型，回退规则引擎。
     * @return {score, level, features, contributions, method, confidence}
     */
    public Map<String, Object> predict(Map<String, Object> signals, Map<String, Object> candidateProfile) {
        Map<String, Double> features = FeatureExtractor.extractNamed(signals, candidateProfile);
        double[] featureArray = FeatureExtractor.extract(signals, candidateProfile);

        double score;
        Map<String, Double> contributions;
        String method;

        if (mlModelLoaded && pmmlModel != null) {
            // PMML 模型推理
            double proba = pmmlModel.predictProba(featureArray);
            score = proba * 100.0;
            method = "pmml_v1";
            contributions = computeFeatureContributions(featureArray);
            log.debug("PMML prediction: proba={}, score={}", String.format("%.2f", proba), String.format("%.1f", score));
        } else {
            // 规则引擎兜底
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
        result.put("confidence", mlModelLoaded ? 0.80 : 0.65);

        return result;
    }

    /**
     * 使用 PMML 模型时，基于特征扰动计算贡献度（简化 SHAP 近似）。
     */
    private Map<String, Double> computeFeatureContributions(double[] featureArray) {
        Map<String, Double> contribs = new LinkedHashMap<>();
        if (pmmlModel == null || !pmmlModel.isLoaded()) return contribs;

        double basePred = pmmlModel.predictProba(featureArray);

        for (int i = 0; i < featureArray.length && i < pmmlModel.getFeatureNames().size(); i++) {
            double orig = featureArray[i];
            // 扰动：设为中位值（归零效应）
            featureArray[i] = 0;
            double perturbedPred = pmmlModel.predictProba(featureArray);
            featureArray[i] = orig;

            double contrib = (basePred - perturbedPred) * 100.0;
            contribs.put(pmmlModel.getFeatureNames().get(i), Math.round(contrib * 10.0) / 10.0);
        }
        return contribs;
    }

    /**
     * 规则引擎打分。
     */
    private double rulesEnginePredict(Map<String, Object> signals) {
        double score = 50.0;
        score += (toDouble(signals.get("replySpeed"), 0.5) - 0.5) * 30;
        score += (toDouble(signals.get("interviewEngagement"), 0.7) - 0.6) * 25;
        score -= toDouble(signals.get("salaryGapPercent"), 10) * 0.3;
        score += (toDouble(signals.get("questionsDepth"), 0.5) - 0.4) * 20;
        if (Boolean.TRUE.equals(signals.get("competingOffers"))) score -= 12;
        return score;
    }

    /**
     * 规则引擎各特征贡献度。
     */
    private Map<String, Double> rulesEngineContributions(Map<String, Object> signals) {
        Map<String, Double> contributions = new LinkedHashMap<>();
        double replySpeed = toDouble(signals.get("replySpeed"), 0.5);
        contributions.put("回复速度", Math.round(((replySpeed - 0.5) * 30) * 10.0) / 10.0);
        double engagement = toDouble(signals.get("interviewEngagement"), 0.7);
        contributions.put("面试投入度", Math.round(((engagement - 0.6) * 25) * 10.0) / 10.0);
        double salaryGap = toDouble(signals.get("salaryGapPercent"), 10);
        contributions.put("薪资差距", Math.round((-salaryGap * 0.3) * 10.0) / 10.0);
        double depth = toDouble(signals.get("questionsDepth"), 0.5);
        contributions.put("提问深度", Math.round(((depth - 0.4) * 20) * 10.0) / 10.0);
        if (Boolean.TRUE.equals(signals.get("competingOffers"))) {
            contributions.put("竞品Offer", -12.0);
        }
        return contributions;
    }

    private double toDouble(Object o, double def) {
        return o instanceof Number ? ((Number) o).doubleValue() : def;
    }

    public String getActiveModelVersion() { return activeModelVersion; }
    public boolean isMlModelLoaded() { return mlModelLoaded; }
}
