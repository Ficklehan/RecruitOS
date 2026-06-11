package com.recruitos.brain.engine;

import com.recruitos.brain.domain.CandidateIntent;
import com.recruitos.brain.ml.MlModelService;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Component
public class IntentPredictionEngine {
    @Resource private MlModelService mlModelService;

    public CandidateIntent predict(Long candidateId, String candidateName, Long jobId, String jobTitle,
                                    Map<String, Object> signals) {
        // 使用 ML 模型服务（当前规则引擎，后续 ML）
        Map<String, Object> candidateProfile = buildProfile(candidateId);
        Map<String, Object> result = mlModelService.predict(signals, candidateProfile);

        CandidateIntent intent = new CandidateIntent();
        intent.setCandidateId(candidateId);
        intent.setCandidateName(candidateName);
        intent.setJobId(jobId);
        intent.setJobTitle(jobTitle);
        intent.setIntentScore(((Number) result.get("score")).doubleValue());
        intent.setIntentLevel((String) result.get("level"));
        intent.setConfidence(((Number) result.get("confidence")).doubleValue());
        intent.setUpdatedAt(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

        // 风险因素
        List<CandidateIntent.RiskFactor> risks = new ArrayList<>();
        @SuppressWarnings("unchecked")
        Map<String, Double> contributions = (Map<String, Double>) result.get("contributions");
        for (Map.Entry<String, Double> e : contributions.entrySet()) {
            if (e.getValue() < -8) {
                risks.add(rf(e.getKey(), e.getValue() < -15 ? "CRITICAL" : "HIGH",
                        e.getKey() + "贡献 " + String.format("%.0f", e.getValue()) + " 分"));
            }
        }
        intent.setRiskFactors(risks);

        // 干预建议
        intent.setInterventionSuggestions(buildInterventions(risks, intent.getIntentScore()));

        return intent;
    }

    private Map<String, Object> buildProfile(Long candidateId) {
        Map<String, Object> p = new LinkedHashMap<>();
        p.put("avgTenureMonths", 24); p.put("jobHopCount3y", 1);
        p.put("currentCompany", "未知");
        return p;
    }

    private CandidateIntent.RiskFactor rf(String f, String l, String d) {
        CandidateIntent.RiskFactor r = new CandidateIntent.RiskFactor();
        r.setFactor(f); r.setLevel(l); r.setDetail(d);
        return r;
    }

    private List<String> buildInterventions(List<CandidateIntent.RiskFactor> risks, double score) {
        List<String> iv = new ArrayList<>();
        if (score < 40) iv.add("建议Hiring Manager直接沟通业务愿景");
        boolean hasCritical = risks.stream().anyMatch(r -> "CRITICAL".equals(r.getLevel()));
        if (hasCritical) iv.add("加速流程：压缩面试轮次，缩短反馈周期");
        boolean hasSalary = risks.stream().anyMatch(r -> r.getFactor().contains("薪资"));
        if (hasSalary) iv.add("评估薪酬弹性空间");
        if (iv.isEmpty()) iv.add("按正常节奏推进");
        return iv;
    }
}
