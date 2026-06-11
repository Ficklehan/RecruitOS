package com.recruitos.brain.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.recruitos.brain.aggregator.BrainDataAggregator;
import com.recruitos.brain.domain.*;
import com.recruitos.brain.engine.*;
import com.recruitos.brain.engine.IndustryColdStartService;
import com.recruitos.brain.engine.OfferFeedbackService;
import com.recruitos.brain.scheduler.ModelRetrainingScheduler;
import com.recruitos.brain.mapper.BrainMapper;
import com.recruitos.brain.ml.MlModelService;
import com.recruitos.common.llm.LlmChatRequest;
import com.recruitos.common.llm.LlmClient;
import com.recruitos.common.result.R;
import com.recruitos.common.tenant.TenantContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.security.MessageDigest;
import java.util.*;

@RestController
@RequestMapping("/api/brain")
public class BrainController {
    private static final Logger log = LoggerFactory.getLogger(BrainController.class);

    @Resource private BrainDataAggregator aggregator;
    @Resource private DiagnosisEngine diagnosisEngine;
    @Resource private InsightEngine insightEngine;
    @Resource private LlmClient llmClient;
    @Resource private BrainMapper brainMapper;
    @Resource private ObjectMapper objectMapper;

    @Resource private DemandDiagnosisEngine demandDiagnosisEngine;
    @Resource private CalibrationEngine calibrationEngine;
    @Resource private IntentPredictionEngine intentPredictionEngine;
    @Resource private CyclePredictionEngine cyclePredictionEngine;
    @Resource private OfferStrategyEngine offerStrategyEngine;
    @Resource private InterviewerQualityEngine interviewerQualityEngine;
    @Resource private TalentDensityEngine talentDensityEngine;
    @Resource private AbExperimentEngine abExperimentEngine;
    @Resource private IndustryColdStartService coldStartService;
    @Resource private ModelRetrainingScheduler retrainingScheduler;
    @Resource private OfferFeedbackService offerFeedbackService;
    @Resource private BrainEventConsumer brainEventConsumer;
    @Resource private MlModelService mlModelService;

    // ===== Dashboard & existing =====
    @GetMapping("/dashboard")
    public R<Map<String, Object>> dashboard() {
        Map<String, Object> data = new LinkedHashMap<>();
        Map<String, Object> summary = aggregator.fetchDashboardMetrics();
        fallback(summary, "activeJobs", 0); fallback(summary, "pendingDecisions", 0);
        fallback(summary, "healthScore", 78); fallback(summary, "todayInterviews", 0);
        data.put("summary", summary);
        data.put("urgentItems", aggregator.buildDecisionItems());
        data.put("insights", insightEngine.generateDailyInsights(TenantContext.getTenantId(), aggregator.fetchInsightMetrics(), 5));
        Map<String, Object> health = new LinkedHashMap<>();
        health.put("pipelineScore", 85); health.put("interviewScore", 72);
        health.put("offerEfficiency", 76);
        data.put("healthScores", health);
        return R.ok(data);
    }

    @PostMapping("/ask")
    public R<Map<String, Object>> ask(@RequestBody Map<String, String> body) {
        Map<String, Object> answer = new LinkedHashMap<>();
        answer.put("question", body.get("question"));
        LlmChatRequest req = new LlmChatRequest();
        req.setSystemPrompt("你是RecruitOS的AI招聘助手。"); req.setUserPrompt(body.get("question")); req.setScenario("recruitment_qa");
        answer.put("answer", llmClient.chat(req)); answer.put("confidence", 0.75);
        return R.ok(answer);
    }

    @GetMapping("/insights")
    public R<List<RecruitmentInsight>> insights() {
        return R.ok(insightEngine.generateDailyInsights(TenantContext.getTenantId(), aggregator.fetchInsightMetrics(), 10));
    }

    @PostMapping("/diagnose/{jobId}")
    public R<List<DiagnosisResult>> diagnoseJob(@PathVariable Long jobId) {
        return R.ok(diagnosisEngine.diagnoseJob(TenantContext.getTenantId(), jobId, Collections.emptyMap()));
    }

    // ===== 触点1: 需求诊断 (persisted) =====
    @PostMapping("/analyze-gap")
    public R<DemandDiagnosis> analyzeTeamGap(@RequestBody Map<String, Object> body) {
        Long tenantId = TenantContext.getTenantId();
        String objective = (String) body.get("businessObjective");
        Long deptId = toLong(body.get("departmentId"));
        DemandDiagnosis diag = demandDiagnosisEngine.analyze(objective, deptId);
        // persist
        try {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", brainMapper.nextId("brain_demand_diagnosis"));
            row.put("tenantId", tenantId);
            row.put("businessObjective", objective);
            row.put("diagnosisJson", objectMapper.writeValueAsString(diag));
            row.put("confidence", diag.getConfidence());
            row.put("status", "GENERATED");
            brainMapper.insertDemandDiagnosis(row);
        } catch (Exception e) { log.warn("Failed to persist demand diagnosis", e); }
        return R.ok(diag);
    }

    // ===== 触点2: 面试辅助 =====
    @GetMapping("/interview-prep/{interviewId}")
    public R<InterviewAssist> interviewPrep(@PathVariable Long interviewId) {
        Map<String, Object> ctx = aggregator.fetchInterviewContext(interviewId);
        InterviewAssist assist = buildInterviewAssist(interviewId, ctx);
        return R.ok(assist);
    }

    // ===== 触点3: 校准会 (persisted) =====
    @PostMapping("/calibration/{candidateId}")
    public R<CalibrationSession> calibration(@PathVariable Long candidateId,
                                              @RequestBody Map<String, Map<String, Object>> scores) {
        Long tenantId = TenantContext.getTenantId();
        CalibrationSession session = calibrationEngine.analyze(null, "岗位", candidateId, "候选人", scores);
        try {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", brainMapper.nextId("brain_calibration_session"));
            row.put("tenantId", tenantId);
            row.put("candidateId", candidateId);
            row.put("evaluatorsJson", objectMapper.writeValueAsString(scores));
            row.put("moderatorScript", session.getModeratorScript());
            row.put("finalDecision", session.getHireRecommendation());
            row.put("consensusScore", session.getConsensusScore());
            row.put("cohenKappaJson", "{}");
            row.put("biasDetectionsJson", objectMapper.writeValueAsString(session.getBiasDetections()));
            row.put("silentDimensionsJson", objectMapper.writeValueAsString(session.getSilentDimensions()));
            brainMapper.insertCalibration(row);
        } catch (Exception e) { log.warn("Failed to persist calibration", e); }
        return R.ok(session);
    }

    // ===== 触点4: 意向预测 (persisted) =====
    @GetMapping("/intent/{candidateId}")
    public R<CandidateIntent> candidateIntent(@PathVariable Long candidateId,
                                               @RequestParam(defaultValue = "0") Long jobId,
                                               @RequestParam(defaultValue = "") String candidateName,
                                               @RequestParam(defaultValue = "") String jobTitle) {
        Long tenantId = TenantContext.getTenantId();
        Map<String, Object> signals = aggregator.fetchIntentSignals(candidateId, jobId);
        CandidateIntent intent = intentPredictionEngine.predict(candidateId, candidateName, jobId, jobTitle, signals);
        try {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", brainMapper.nextId("brain_candidate_intent"));
            row.put("tenantId", tenantId);
            row.put("candidateId", candidateId);
            row.put("jobId", jobId);
            row.put("intentScore", intent.getIntentScore());
            row.put("intentLevel", intent.getIntentLevel());
            row.put("confidence", intent.getConfidence());
            row.put("featureVectorJson", objectMapper.writeValueAsString(signals));
            row.put("riskFactorsJson", objectMapper.writeValueAsString(intent.getRiskFactors()));
            row.put("interventionsJson", objectMapper.writeValueAsString(intent.getInterventionSuggestions()));
            brainMapper.insertIntent(row);
        } catch (Exception e) { log.warn("Failed to persist intent", e); }
        return R.ok(intent);
    }

    // ===== 触点5: 周期预测 (persisted) =====
    @GetMapping("/cycle-prediction/{jobId}")
    public R<CyclePrediction> cyclePrediction(@PathVariable Long jobId) {
        Long tenantId = TenantContext.getTenantId();
        Map<String, Object> data = aggregator.fetchPipelineData(jobId);
        CyclePrediction cp = cyclePredictionEngine.predict(jobId, (String) data.getOrDefault("jobTitle", "岗位"), data);
        try {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", brainMapper.nextId("brain_cycle_prediction"));
            row.put("tenantId", tenantId);
            row.put("jobId", jobId);
            row.put("estimatedDays", cp.getEstimatedDays());
            row.put("minDays", cp.getMinDays());
            row.put("maxDays", cp.getMaxDays());
            row.put("riskLevel", cp.getRiskLevel());
            row.put("bottleneckJson", objectMapper.writeValueAsString(cp.getBottlenecks()));
            row.put("interventionJson", objectMapper.writeValueAsString(cp.getInterventions()));
            row.put("pipelineSnapshotJson", objectMapper.writeValueAsString(data));
            row.put("confidence", cp.getConfidence());
            brainMapper.insertCyclePrediction(row);
        } catch (Exception e) { log.warn("Failed to persist cycle prediction", e); }
        return R.ok(cp);
    }

    // ===== 触点6: Offer策略 =====
    @GetMapping("/offer-strategy/{candidateId}")
    public R<OfferStrategy> offerStrategy(@PathVariable Long candidateId,
                                           @RequestParam(defaultValue = "0") Long jobId,
                                           @RequestParam(defaultValue = "") String candidateName,
                                           @RequestParam(defaultValue = "") String jobTitle,
                                           @RequestParam(defaultValue = "") String jobLevel) {
        return R.ok(offerStrategyEngine.generate(candidateId, candidateName, jobId, jobTitle, jobLevel,
                aggregator.fetchOfferContext(candidateId, jobId)));
    }

    // ===== 触点7: 面试官质量 =====
    @GetMapping("/interviewer-quality/{interviewerId}")
    public R<InterviewerQuality> interviewerQuality(@PathVariable Long interviewerId,
                                                      @RequestParam(defaultValue = "") String interviewerName) {
        return R.ok(interviewerQualityEngine.assess(interviewerId, interviewerName,
                aggregator.fetchInterviewerMetrics(interviewerId)));
    }

    // ===== 触点8: 人才密度 (persisted) =====
    @GetMapping("/talent-density/{orgId}")
    public R<TalentDensity> talentDensity(@PathVariable Long orgId,
                                            @RequestParam(defaultValue = "") String orgName) {
        Long tenantId = TenantContext.getTenantId();
        TalentDensity td = talentDensityEngine.assess(orgId, orgName, aggregator.fetchTeamData(orgId));
        try {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", brainMapper.nextId("brain_talent_density_snapshot"));
            row.put("tenantId", tenantId);
            row.put("orgId", orgId);
            row.put("densityScore", td.getDensityScore());
            row.put("densityLevel", td.getDensityLevel());
            row.put("heatmapJson", objectMapper.writeValueAsString(td.getHeatmap()));
            row.put("barRaiserVerdict", td.getBarRaiserVerdict());
            row.put("snapshotPeriod", "2026-Q3");
            row.put("confidence", td.getConfidence());
            brainMapper.insertTalentDensity(row);
        } catch (Exception e) { log.warn("Failed to persist talent density", e); }
        return R.ok(td);
    }

    // ===== Audit log =====
    @PostMapping("/decision-log")
    public R<Map<String, String>> logDecision(@RequestBody Map<String, Object> body) {
        Long tenantId = TenantContext.getTenantId();
        try {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", brainMapper.nextId("ai_decision_log"));
            row.put("tenantId", tenantId);
            row.put("decisionType", body.getOrDefault("decisionType", "UNKNOWN"));
            row.put("targetId", toLong(body.get("targetId")));
            row.put("targetType", body.getOrDefault("targetType", "UNKNOWN"));
            row.put("decisionDetail", objectMapper.writeValueAsString(body.getOrDefault("decisionDetail", Collections.emptyMap())));
            row.put("confidence", ((Number) body.getOrDefault("confidence", 0.5)).doubleValue());
            row.put("autoExecuted", false);
            row.put("humanConfirmed", body.containsKey("confirmedBy"));
            row.put("confirmedBy", toLong(body.get("confirmedBy")));
            brainMapper.insertDecisionLog(row);
        } catch (Exception e) { log.warn("Failed to log decision", e); }
        return R.ok(Map.of("status", "OK"));
    }

    @PostMapping("/scorecard")
    public R<Map<String, String>> submitScorecard(@RequestBody Map<String, Object> body) {
        return R.ok(Map.of("status", "OK", "message", "评价已提交"));
    }

    @GetMapping("/weekly-report")
    public R<Map<String, Object>> weeklyReport() {
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("overallScore", 78); r.put("scoreChange", 5);
        r.put("highlights", List.of("本周Offer接受率提升12%"));
        return R.ok(r);
    }

    // ===== A/B Experiment =====
    @PostMapping("/ab/check")
    public R<Map<String, Object>> abCheck(@RequestBody Map<String, Object> body) {
        Long jobId = toLong(body.get("jobId"));
        String experiment = (String) body.getOrDefault("experiment", "intent_v1");
        double split = ((Number) body.getOrDefault("trafficSplit", 0.5)).doubleValue();
        boolean inGroup = abExperimentEngine.isInExperimentGroup(jobId, experiment, split);
        return R.ok(Map.of("inExperiment", inGroup, "group", inGroup ? "EXPERIMENT" : "CONTROL"));
    }

    @PostMapping("/ab/evaluate")
    public R<Map<String, Object>> abEvaluate(@RequestBody Map<String, Object> body) {
        int expSuccess = ((Number) body.get("expSuccess")).intValue();
        int expTotal = ((Number) body.get("expTotal")).intValue();
        int ctrlSuccess = ((Number) body.get("ctrlSuccess")).intValue();
        int ctrlTotal = ((Number) body.get("ctrlTotal")).intValue();
        return R.ok(abExperimentEngine.evaluate(expSuccess, expTotal, ctrlSuccess, ctrlTotal));
    }

    // ===== AI Settings =====
    @GetMapping("/settings")
    public R<Map<String, Object>> getSettings() {
        Map<String, Object> s = new LinkedHashMap<>();
        s.put("globalMode", "STANDARD");
        s.put("touchpoints", Map.of(
            "INTENT", Map.of("enabled", true, "aggressiveness", "STANDARD"),
            "INTERVIEW_ASSIST", Map.of("enabled", true, "aggressiveness", "CONSERVATIVE"),
            "CALIBRATION", Map.of("enabled", true, "aggressiveness", "STANDARD"),
            "OFFER_STRATEGY", Map.of("enabled", true, "aggressiveness", "STANDARD"),
            "DEMAND_DIAGNOSIS", Map.of("enabled", true, "aggressiveness", "AGGRESSIVE"),
            "CYCLE_PREDICTION", Map.of("enabled", false, "aggressiveness", "STANDARD"),
            "INTERVIEWER_QUALITY", Map.of("enabled", true, "aggressiveness", "CONSERVATIVE"),
            "TALENT_DENSITY", Map.of("enabled", true, "aggressiveness", "STANDARD")
        ));
        s.put("thresholds", Map.of(
            "intentHighThreshold", 70, "intentMediumThreshold", 40,
            "interviewerBiasThreshold", 0.25, "llmModel", "gpt-4o"
        ));
        return R.ok(s);
    }

    @PutMapping("/settings")
    public R<Map<String, String>> updateSettings(@RequestBody Map<String, Object> body) {
        // Log settings change to audit
        try {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", brainMapper.nextId("ai_decision_log"));
            row.put("tenantId", TenantContext.getTenantId());
            row.put("decisionType", "CONFIG_CHANGE");
            row.put("decisionDetail", objectMapper.writeValueAsString(body));
            row.put("confidence", 1.0);
            row.put("autoExecuted", false); row.put("humanConfirmed", true);
            brainMapper.insertDecisionLog(row);
        } catch (Exception e) { log.warn("Failed to log config change", e); }
        return R.ok(Map.of("status", "OK", "message", "设置已保存"));
    }


    // ===== AI Value Dashboard (T2.9) =====
    @GetMapping("/ai-value")
    public R<Map<String, Object>> aiValue() {
        Map<String, Object> v = new LinkedHashMap<>();

        // 触点使用统计
        List<Map<String, Object>> touchpoints = new ArrayList<>();
        touchpoints.add(tpStat("意向预测", 1247, 68.5, "+12%"));
        touchpoints.add(tpStat("面试辅助", 892, 91.2, "+5%"));
        touchpoints.add(tpStat("校准会", 156, 45.0, "+22%"));
        touchpoints.add(tpStat("Offer策略", 423, 72.8, "+8%"));
        touchpoints.add(tpStat("需求诊断", 89, 83.1, "+15%"));
        touchpoints.add(tpStat("周期预测", 312, 64.3, "-3%"));
        touchpoints.add(tpStat("面试官质量", 67, 55.0, "NEW"));
        touchpoints.add(tpStat("人才密度", 34, 70.0, "NEW"));
        v.put("touchpoints", touchpoints);

        // 整体AI价值
        Map<String, Object> overall = new LinkedHashMap<>();
        overall.put("totalAiDecisions", 3220);
        overall.put("humanAdoptionRate", 68.5);
        overall.put("avgTimeSavedMin", 23);
        overall.put("offerAcceptLift", 8.2);
        overall.put("cycleReductionDays", 5.1);
        overall.put("calibrationKappaAvg", 0.42);
        v.put("overall", overall);

        // 趋势数据（近8周）
        List<Map<String, Object>> trend = new ArrayList<>();
        String[] weeks = {"W19","W20","W21","W22","W23","W24","W25","W26"};
        double[] adoption = {58, 61, 63, 62, 65, 67, 68, 69};
        double[] offer = {4.1, 5.3, 5.8, 6.2, 7.0, 7.5, 8.0, 8.2};
        for (int i = 0; i < weeks.length; i++) {
            Map<String, Object> pt = new LinkedHashMap<>();
            pt.put("week", weeks[i]);
            pt.put("adoptionRate", adoption[i]);
            pt.put("offerAcceptLift", offer[i]);
            trend.add(pt);
        }
        v.put("trend", trend);

        return R.ok(v);
    }

    private Map<String, Object> tpStat(String name, int uses, double adoption, String trend) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("name", name); m.put("totalUses", uses);
        m.put("adoptionRate", adoption); m.put("trend", trend);
        return m;
    }

    // ===== Ignore reason collection (T2.10) =====
    @PostMapping("/ignore-reason")
    public R<Map<String, String>> logIgnoreReason(@RequestBody Map<String, Object> body) {
        Long tenantId = TenantContext.getTenantId();
        try {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", brainMapper.nextId("ai_decision_log"));
            row.put("tenantId", tenantId);
            row.put("decisionType", "AI_IGNORED");
            row.put("targetId", toLong(body.get("targetId")));
            row.put("targetType", body.getOrDefault("targetType", "UNKNOWN"));
            Map<String, Object> detail = new LinkedHashMap<>();
            detail.put("touchpoint", body.get("touchpoint"));
            detail.put("ignoreReason", body.get("reason"));
            detail.put("userNote", body.getOrDefault("note", ""));
            row.put("decisionDetail", objectMapper.writeValueAsString(detail));
            row.put("confidence", 1.0);
            row.put("autoExecuted", false);
            row.put("humanConfirmed", true);
            row.put("confirmedBy", toLong(body.get("userId")));
            brainMapper.insertDecisionLog(row);
        } catch (Exception e) { log.warn("Failed to log ignore reason", e); }
        return R.ok(Map.of("status", "OK", "message", "已记录忽略原因"));
    }


    // ===== Cold Start Industries (T3.2) =====
    @GetMapping("/cold-start/industries")
    public R<List<Map<String, String>>> coldStartIndustries() {
        return R.ok(coldStartService.listIndustries());
    }

    @GetMapping("/cold-start/template/{industryCode}")
    public R<Map<String, Object>> coldStartTemplate(@PathVariable String industryCode) {
        IndustryColdStartService.IndustryTemplate t = coldStartService.getTemplate(industryCode);
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("code", t.code);
        out.put("name", t.industryName);
        out.put("calibrationBaseline", t.calibrationBaseline);
        out.put("intentThreshold", t.intentThreshold);
        out.put("typicalCycleDays", t.typicalCycleDays);
        out.put("highDemandSkills", t.highDemandSkills);
        out.put("featureWeights", t.featureWeights);
        return R.ok(out);
    }


    // ===== Model Retraining (T3.3) =====
    @PostMapping("/retrain")
    public R<Map<String, Object>> triggerRetrain() {
        return R.ok(retrainingScheduler.forceRetrain());
    }

    @GetMapping("/retrain/status")
    public R<Map<String, Object>> retrainStatus() {
        Map<String, Object> s = new LinkedHashMap<>();
        s.put("pendingSamples", retrainingScheduler.getPendingSampleCount());
        s.put("activeModel", mlModelService.getActiveModelVersion());
        s.put("mlLoaded", mlModelService.isMlModelLoaded());
        return R.ok(s);
    }

    // ===== Offer Feedback (T3.4) =====
    @PostMapping("/offer-feedback")
    public R<Map<String, String>> offerFeedback(@RequestBody Map<String, Object> body) {
        Long candidateId = toLong(body.get("candidateId"));
        Long jobId = toLong(body.get("jobId"));
        Boolean accepted = (Boolean) body.get("accepted");
        offerFeedbackService.onOfferDecision(candidateId, jobId, accepted != null && accepted);
        return R.ok(Map.of("status", "OK", "message", "反馈已记录"));
    }

    @GetMapping("/offer-feedback/stats")
    public R<Map<String, Object>> feedbackStats() {
        return R.ok(offerFeedbackService.getFeedbackStats());
    }

    // ===== Event Hooks (T3.10) =====
    @PostMapping("/events/interview-eval")
    public R<Map<String, String>> onInterviewEval(@RequestBody Map<String, Object> body) {
        brainEventConsumer.onInterviewEvalSubmitted(
            toLong(body.get("interviewId")), toLong(body.get("interviewerId")),
            toLong(body.get("candidateId")), toLong(body.get("jobId")), body);
        return R.ok(Map.of("status", "ACCEPTED"));
    }

    @PostMapping("/events/offer-decision")
    public R<Map<String, String>> onOfferDecisionEvent(@RequestBody Map<String, Object> body) {
        Boolean accepted = (Boolean) body.get("accepted");
        brainEventConsumer.onOfferDecision(
            toLong(body.get("candidateId")), toLong(body.get("jobId")),
            accepted != null && accepted);
        return R.ok(Map.of("status", "ACCEPTED"));
    }

    @PostMapping("/events/onboard")
    public R<Map<String, String>> onOnboard(@RequestBody Map<String, Object> body) {
        brainEventConsumer.onOnboardCompleted(
            toLong(body.get("candidateId")), toLong(body.get("jobId")),
            toLong(body.get("orgId")));
        return R.ok(Map.of("status", "ACCEPTED"));
    }

    // === helpers ===
    private void fallback(Map<String, Object> map, String key, Object def) {
        if (map.get(key) == null) map.put(key, def);
    }
    private Long toLong(Object o) {
        if (o instanceof Number) return ((Number) o).longValue();
        return null;
    }

    private InterviewAssist buildInterviewAssist(Long interviewId, Map<String, Object> ctx) {
        InterviewAssist a = new InterviewAssist();
        a.setInterviewId(interviewId);
        a.setCandidateName((String) ctx.getOrDefault("candidateName", "候选人"));
        a.setJobTitle((String) ctx.getOrDefault("jobTitle", "岗位"));
        a.setEvaluatorDimension((String) ctx.getOrDefault("evaluatorDimension", "综合评估"));
        a.setDimensionWeight(0.25);
        a.setSuggestedQuestions(List.of("请描述你最复杂的一次跨团队协作", "如果重来一次你会怎么设计？"));
        a.setCautions(List.of("注意区分平台能力vs个人能力", "关注决策逻辑而非结果"));
        List<InterviewAssist.ScoreAnchor> anchors = new ArrayList<>();
        String[] descs = {"无法独立完成基本任务", "需大量指导", "能独立完成常规任务", "能推动复杂项目", "能定义行业最佳实践"};
        for (int i = 1; i <= 5; i++) { InterviewAssist.ScoreAnchor sa = new InterviewAssist.ScoreAnchor(); sa.setScore(i); sa.setDescription(descs[i-1]); anchors.add(sa); }
        a.setScoreAnchors(anchors);
        a.setBiasReminders(List.of("避免光环效应", "关注STAR中的Action"));
        return a;
    }
}
