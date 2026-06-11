package com.recruitos.brain.controller;

import com.recruitos.brain.aggregator.BrainDataAggregator;
import com.recruitos.brain.domain.CandidateIntent;
import com.recruitos.brain.domain.CyclePrediction;
import com.recruitos.brain.domain.OfferStrategy;
import com.recruitos.brain.engine.*;
import com.recruitos.common.result.R;
import com.recruitos.common.tenant.TenantContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.*;

/**
 * BatchBrainController — 批量 AI 推理端点。
 * 解决 N+1 查询问题：前端列表页一次性传入所有候选人 ID，
 * 后端批量聚合数据后统一返回。
 */
@RestController
@RequestMapping("/api/brain/batch")
public class BatchBrainController {
    private static final Logger log = LoggerFactory.getLogger(BatchBrainController.class);

    @Resource private BrainDataAggregator aggregator;
    @Resource private IntentPredictionEngine intentEngine;
    @Resource private CyclePredictionEngine cycleEngine;
    @Resource private OfferStrategyEngine offerEngine;

    /**
     * 批量意向预测。
     * POST /api/brain/batch/intent
     * Body: { "candidateIds": [1,2,3], "jobId": 100 }
     * Response: { "results": { "1": { intentScore: 72, intentLevel: "HIGH", ... }, ... } }
     */
    @PostMapping("/intent")
    public R<Map<String, Object>> batchIntent(@RequestBody Map<String, Object> body) {
        Long tenantId = TenantContext.getTenantId();
        @SuppressWarnings("unchecked")
        List<Integer> rawIds = (List<Integer>) body.getOrDefault("candidateIds", Collections.emptyList());
        Long jobId = toLong(body.get("jobId"));

        Map<String, Object> results = new LinkedHashMap<>();
        for (Integer cid : rawIds) {
            Long candidateId = cid.longValue();
            try {
                Map<String, Object> signals = aggregator.fetchIntentSignals(candidateId, jobId);
                CandidateIntent intent = intentEngine.predict(candidateId, "", jobId, "", signals);
                Map<String, Object> entry = new LinkedHashMap<>();
                entry.put("intentScore", intent.getIntentScore());
                entry.put("intentLevel", intent.getIntentLevel());
                entry.put("confidence", intent.getConfidence());
                entry.put("riskFactors", intent.getRiskFactors());
                entry.put("interventionSuggestions", intent.getInterventionSuggestions());
                entry.put("updatedAt", intent.getUpdatedAt());
                results.put(String.valueOf(candidateId), entry);
            } catch (Exception e) {
                log.warn("Batch intent failed for candidate {}", candidateId, e);
                Map<String, Object> fallback = new LinkedHashMap<>();
                fallback.put("intentScore", 50); fallback.put("intentLevel", "MEDIUM");
                fallback.put("confidence", 0.5);
                results.put(String.valueOf(candidateId), fallback);
            }
        }

        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("jobId", jobId);
        resp.put("results", results);
        resp.put("count", rawIds.size());
        return R.ok(resp);
    }

    /**
     * 批量周期预测。
     * POST /api/brain/batch/cycle
     * Body: { "jobIds": [1,2,3] }
     */
    @PostMapping("/cycle")
    public R<Map<String, Object>> batchCycle(@RequestBody Map<String, Object> body) {
        @SuppressWarnings("unchecked")
        List<Integer> rawIds = (List<Integer>) body.getOrDefault("jobIds", Collections.emptyList());

        Map<String, Object> results = new LinkedHashMap<>();
        for (Integer jid : rawIds) {
            Long jobId = jid.longValue();
            try {
                Map<String, Object> data = aggregator.fetchPipelineData(jobId);
                CyclePrediction cp = cycleEngine.predict(jobId, (String) data.getOrDefault("jobTitle", ""), data);
                Map<String, Object> entry = new LinkedHashMap<>();
                entry.put("estimatedDays", cp.getEstimatedDays());
                entry.put("riskLevel", cp.getRiskLevel());
                entry.put("confidence", cp.getConfidence());
                results.put(String.valueOf(jobId), entry);
            } catch (Exception e) {
                log.warn("Batch cycle failed for job {}", jobId, e);
            }
        }

        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("results", results);
        resp.put("count", rawIds.size());
        return R.ok(resp);
    }

    // === helpers ===
    private Long toLong(Object o) {
        if (o instanceof Number) return ((Number) o).longValue();
        if (o instanceof String) return Long.parseLong((String) o);
        return null;
    }
}
