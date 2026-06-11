package com.recruitos.common.evolution;

import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 跨模块进化信号发射器（interview / offer / communication / onboard）。
 */
@Component
public class ModuleEvolutionEmitter {

    @Resource
    private EvolutionSignalEmitter evolutionSignalEmitter;

    /** L2：面试评价提交 */
    public void emitInterviewResult(Long jobId, Long candidateId, String decision, Integer overallScore) {
        if (jobId == null) {
            return;
        }
        EvolutionEmitRequest req = base(jobId, candidateId, "recruitos-interview");
        req.setSignalLevel(EvolutionSignalLevel.L2_INTERVIEW);
        boolean passed = "PASS".equalsIgnoreCase(decision);
        req.setSourceEvent(passed ? "INTERVIEW_PASS" : "INTERVIEW_FAIL");
        Map<String, Object> adj = new LinkedHashMap<>();
        adj.put("_interview", passed ? 0.08 : -0.08);
        if (overallScore != null) {
            adj.put("_score", overallScore.doubleValue());
        }
        req.setTagAdjustments(adj);
        evolutionSignalEmitter.emit(req);
    }

    /** L1：Offer 接受 */
    public void emitOfferAccepted(Long jobId, Long candidateId) {
        emitOfferOutcome(jobId, candidateId, true);
    }

    /** L1：Offer 拒绝 */
    public void emitOfferRejected(Long jobId, Long candidateId) {
        emitOfferOutcome(jobId, candidateId, false);
    }

    /** L4：候选人回复 */
    public void emitCandidateReply(Long jobId, Long candidateId, Long conversationId) {
        if (jobId == null) {
            return;
        }
        EvolutionEmitRequest req = base(jobId, candidateId, "recruitos-communication");
        req.setSignalLevel(EvolutionSignalLevel.L4_REPLY);
        req.setSourceEvent("CANDIDATE_REPLY");
        if (conversationId != null) {
            Map<String, Object> adj = new LinkedHashMap<>();
            adj.put("_conversationId", conversationId.doubleValue());
            req.setTagAdjustments(adj);
        }
        evolutionSignalEmitter.emit(req);
    }

    /** L4：候选人沉默（超时未回复） */
    public void emitCandidateSilent(Long jobId, Long candidateId, Long conversationId) {
        if (jobId == null) {
            return;
        }
        EvolutionEmitRequest req = base(jobId, candidateId, "recruitos-communication");
        req.setSignalLevel(EvolutionSignalLevel.L4_REPLY);
        req.setSourceEvent("CANDIDATE_SILENT");
        Map<String, Object> adj = new LinkedHashMap<>();
        adj.put("_silent", 1.0);
        if (conversationId != null) {
            adj.put("_conversationId", conversationId.doubleValue());
        }
        req.setTagAdjustments(adj);
        evolutionSignalEmitter.emit(req);
    }

    /** L4：候选人明确拒绝（关键词命中，停止复聊） */
    public void emitCandidateDecline(Long jobId, Long candidateId, Long conversationId) {
        if (jobId == null) {
            return;
        }
        EvolutionEmitRequest req = base(jobId, candidateId, "recruitos-communication");
        req.setSignalLevel(EvolutionSignalLevel.L4_REPLY);
        req.setSourceEvent("CANDIDATE_DECLINE");
        Map<String, Object> adj = new LinkedHashMap<>();
        adj.put("_decline", 1.0);
        if (conversationId != null) {
            adj.put("_conversationId", conversationId.doubleValue());
        }
        req.setTagAdjustments(adj);
        evolutionSignalEmitter.emit(req);
    }

    /** L6：试用期考核结果（HRone 回流 / Demo） */
    public void emitProbationResult(Long jobId, Long candidateId, boolean passed, Integer overallScore) {
        if (jobId == null) {
            return;
        }
        EvolutionEmitRequest req = base(jobId, candidateId, "recruitos-onboard");
        req.setSignalLevel(EvolutionSignalLevel.L6_PROBATION);
        req.setSourceEvent(passed ? "PROBATION_PASS" : "PROBATION_FAIL");
        Map<String, Object> adj = new LinkedHashMap<>();
        adj.put("_probation", passed ? 0.20 : -0.18);
        if (overallScore != null) {
            adj.put("_score", overallScore.doubleValue());
        }
        req.setTagAdjustments(adj);
        evolutionSignalEmitter.emit(req);
    }

    /** L4：复聊已发送（communication 模块调度触发） */
    public void emitRechatSent(Long jobId, Long candidateId, Long conversationId, int attempt) {
        if (jobId == null) {
            return;
        }
        EvolutionEmitRequest req = base(jobId, candidateId, "recruitos-communication");
        req.setSignalLevel(EvolutionSignalLevel.L4_REPLY);
        req.setSourceEvent("RECHAT_SENT");
        Map<String, Object> adj = new LinkedHashMap<>();
        adj.put("_rechat", (double) attempt);
        if (conversationId != null) {
            adj.put("_conversationId", conversationId.doubleValue());
        }
        req.setTagAdjustments(adj);
        evolutionSignalEmitter.emit(req);
    }

    private void emitOfferOutcome(Long jobId, Long candidateId, boolean accepted) {
        if (jobId == null) {
            return;
        }
        EvolutionEmitRequest req = base(jobId, candidateId, "recruitos-offer");
        req.setSignalLevel(EvolutionSignalLevel.L1_OFFER);
        req.setSourceEvent(accepted ? "OFFER_ACCEPTED" : "OFFER_REJECTED");
        Map<String, Object> adj = new LinkedHashMap<>();
        adj.put("_offer", accepted ? 0.15 : -0.12);
        req.setTagAdjustments(adj);
        evolutionSignalEmitter.emit(req);
    }

    private EvolutionEmitRequest base(Long jobId, Long candidateId, String module) {
        EvolutionEmitRequest req = new EvolutionEmitRequest();
        req.setJobId(jobId);
        req.setCandidateId(candidateId);
        req.setSourceModule(module);
        return req;
    }
}
