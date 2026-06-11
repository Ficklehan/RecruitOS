package com.recruitos.brain.mapper;

import org.apache.ibatis.annotations.*;
import java.util.*;

@Mapper
public interface BrainMapper {

    // === brain_candidate_intent ===
    @Insert("INSERT INTO brain_candidate_intent (id, tenant_id, candidate_id, job_id, intent_score, intent_level, feature_vector_json, model_version, confidence, risk_factors_json, interventions_json, created_at) " +
            "VALUES (#{id}, #{tenantId}, #{candidateId}, #{jobId}, #{intentScore}, #{intentLevel}, #{featureVectorJson}, #{modelVersion}, #{confidence}, #{riskFactorsJson}, #{interventionsJson}, NOW())")
    void insertIntent(Map<String, Object> params);

    @Select("SELECT * FROM brain_candidate_intent WHERE tenant_id = #{tenantId} AND candidate_id = #{candidateId} ORDER BY created_at DESC LIMIT 1")
    Map<String, Object> getLatestIntent(@Param("tenantId") Long tenantId, @Param("candidateId") Long candidateId);

    // === brain_calibration_session ===
    @Insert("INSERT INTO brain_calibration_session (id, tenant_id, candidate_id, job_id, evaluators_json, dimension_stats_json, cohen_kappa_json, moderator_script, final_decision, consensus_score, bias_detections_json, silent_dimensions_json, created_at) " +
            "VALUES (#{id}, #{tenantId}, #{candidateId}, #{jobId}, #{evaluatorsJson}, #{dimensionStatsJson}, #{cohenKappaJson}, #{moderatorScript}, #{finalDecision}, #{consensusScore}, #{biasDetectionsJson}, #{silentDimensionsJson}, NOW())")
    void insertCalibration(Map<String, Object> params);

    // === brain_cycle_prediction ===
    @Insert("INSERT INTO brain_cycle_prediction (id, tenant_id, job_id, estimated_days, min_days, max_days, risk_level, bottleneck_json, intervention_json, pipeline_snapshot_json, confidence, created_at) " +
            "VALUES (#{id}, #{tenantId}, #{jobId}, #{estimatedDays}, #{minDays}, #{maxDays}, #{riskLevel}, #{bottleneckJson}, #{interventionJson}, #{pipelineSnapshotJson}, #{confidence}, NOW())")
    void insertCyclePrediction(Map<String, Object> params);

    @Select("SELECT * FROM brain_cycle_prediction WHERE tenant_id = #{tenantId} AND job_id = #{jobId} ORDER BY created_at DESC LIMIT 1")
    Map<String, Object> getLatestCyclePrediction(@Param("tenantId") Long tenantId, @Param("jobId") Long jobId);

    // === brain_talent_density_snapshot ===
    @Insert("INSERT INTO brain_talent_density_snapshot (id, tenant_id, org_id, density_score, density_level, heatmap_json, bar_raiser_verdict, snapshot_period, confidence, created_at) " +
            "VALUES (#{id}, #{tenantId}, #{orgId}, #{densityScore}, #{densityLevel}, #{heatmapJson}, #{barRaiserVerdict}, #{snapshotPeriod}, #{confidence}, NOW())")
    void insertTalentDensity(Map<String, Object> params);

    // === brain_demand_diagnosis ===
    @Insert("INSERT INTO brain_demand_diagnosis (id, tenant_id, demand_id, business_objective, diagnosis_json, confidence, llm_model, llm_prompt_hash, status, created_at) " +
            "VALUES (#{id}, #{tenantId}, #{demandId}, #{businessObjective}, #{diagnosisJson}, #{confidence}, #{llmModel}, #{llmPromptHash}, #{status}, NOW())")
    void insertDemandDiagnosis(Map<String, Object> params);

    // === brain_llm_trace ===
    @Insert("INSERT INTO brain_llm_trace (id, tenant_id, scenario, prompt_hash, prompt_preview, raw_output, parsed_success, parse_error, latency_ms, model, tokens_used, created_at) " +
            "VALUES (#{id}, #{tenantId}, #{scenario}, #{promptHash}, #{promptPreview}, #{rawOutput}, #{parsedSuccess}, #{parseError}, #{latencyMs}, #{model}, #{tokensUsed}, NOW())")
    void insertLlmTrace(Map<String, Object> params);

    // === ai_decision_log (existing table from v14) ===
    @Insert("INSERT INTO ai_decision_log (id, tenant_id, decision_type, target_id, target_type, decision_detail, confidence, auto_executed, human_confirmed, confirmed_by, created_at) " +
            "VALUES (#{id}, #{tenantId}, #{decisionType}, #{targetId}, #{targetType}, #{decisionDetail}, #{confidence}, #{autoExecuted}, #{humanConfirmed}, #{confirmedBy}, NOW())")
    void insertDecisionLog(Map<String, Object> params);

    // === ID generation ===
    @Select("SELECT COALESCE(MAX(id), 0) + 1 FROM ${tableName}")
    Long nextId(@Param("tableName") String tableName);

    // === Offer Feedback (T3.4) ===
    @Select("SELECT * FROM brain_candidate_intent WHERE candidate_id = #{candidateId} AND job_id = #{jobId} ORDER BY created_at DESC LIMIT 1")
    Map<String, Object> findLatestIntent(@Param("candidateId") Long candidateId, @Param("jobId") Long jobId);

    @Update("UPDATE brain_candidate_intent SET actual_outcome = #{outcome}, updated_at = NOW() WHERE id = #{id}")
    void updateIntentOutcome(@Param("id") Long id, @Param("outcome") String outcome);

    @Select("SELECT COUNT(*) FROM brain_candidate_intent WHERE actual_outcome = #{outcome}")
    int countIntentOutcomes(@Param("outcome") String outcome);

    @Select("SELECT COUNT(*) FROM brain_candidate_intent WHERE intent_level = 'HIGH' AND actual_outcome = 'ACCEPTED'")
    int countHighIntentAccepted();

    @Select("SELECT COUNT(*) FROM brain_candidate_intent WHERE intent_level = 'HIGH' AND actual_outcome IS NOT NULL")
    int countHighIntent();

    // === Model Retraining (T3.3) ===
    @Select("SELECT COUNT(*) FROM brain_candidate_intent WHERE actual_outcome IS NOT NULL")
    int countDecisions();

    // === Brain llm trace insert (updated) ===
    @Insert("INSERT INTO brain_llm_trace (id, tenant_id, trace_type, request_json, response_json, latency_ms, created_at) " +
            "VALUES (#{id}, #{tenantId}, #{traceType}, #{requestJson}, #{responseJson}, #{latencyMs}, NOW())")
    void insertLlmTraceV2(Map<String, Object> params);

}
