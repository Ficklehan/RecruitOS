package com.recruitos.brain.engine;

import com.recruitos.brain.domain.CyclePrediction;
import org.springframework.stereotype.Component;
import java.util.*;

/** 触点5引擎：招聘周期预测 */
@Component
public class CyclePredictionEngine {

    public CyclePrediction predict(Long jobId, String jobTitle, Map<String, Object> pipelineData) {
        CyclePrediction cp = new CyclePrediction();
        cp.setJobId(jobId);
        cp.setJobTitle(jobTitle);

        // 基于管道数据推算
        int candidatesScreen = toInt(pipelineData.get("candidatesInScreen"), 0);
        int candidatesInterview = toInt(pipelineData.get("candidatesInInterview"), 0);
        int candidatesOffer = toInt(pipelineData.get("candidatesInOffer"), 0);
        double histScreenRate = toDouble(pipelineData.get("histScreenPassRate"), 0.35);
        double histInterviewRate = toDouble(pipelineData.get("histInterviewPassRate"), 0.40);
        double histOfferRate = toDouble(pipelineData.get("histOfferAcceptRate"), 0.70);

        int baseDays = 45;
        int estimated = baseDays;

        List<CyclePrediction.Bottleneck> bottlenecks = new ArrayList<>();
        List<CyclePrediction.Intervention> interventions = new ArrayList<>();

        // 筛选瓶颈
        if (candidatesScreen < 3) {
            estimated += 10;
            CyclePrediction.Bottleneck b = new CyclePrediction.Bottleneck();
            b.setStage("筛选"); b.setIssue("筛选阶段候选人不足（仅" + candidatesScreen + "人）"); b.setImpact(10);
            bottlenecks.add(b);
            CyclePrediction.Intervention iv = new CyclePrediction.Intervention();
            iv.setAction("增加渠道投入或放宽筛选门槛"); iv.setExpectedEffect("预计可增加5-10名候选人进入筛选"); iv.setEffortDays(3);
            interventions.add(iv);
        }

        // 面试瓶颈
        if (candidatesInterview < 2) {
            estimated += 15;
            CyclePrediction.Bottleneck b = new CyclePrediction.Bottleneck();
            b.setStage("面试"); b.setIssue("面试阶段候选人不足"); b.setImpact(15);
            bottlenecks.add(b);
        }

        // Offer风险
        if (candidatesOffer == 0 && candidatesInterview > 0) {
            estimated += 10;
            CyclePrediction.Bottleneck b = new CyclePrediction.Bottleneck();
            b.setStage("Offer"); b.setIssue("尚无候选人进入Offer阶段"); b.setImpact(10);
            bottlenecks.add(b);
        }

        cp.setEstimatedDays(estimated);
        cp.setMinDays(estimated - 7);
        cp.setMaxDays(estimated + 15);
        cp.setBottlenecks(bottlenecks);
        cp.setInterventions(interventions);

        String risk = estimated < 30 ? "LOW" : estimated < 60 ? "MEDIUM" : "HIGH";
        cp.setRiskLevel(risk);
        cp.setConfidence(bottlenecks.isEmpty() ? 0.55 : 0.72);

        return cp;
    }

    private int toInt(Object o, int def) { return o instanceof Number ? ((Number) o).intValue() : def; }
    private double toDouble(Object o, double def) { return o instanceof Number ? ((Number) o).doubleValue() : def; }
}
