package com.recruitos.agent.service;

import com.recruitos.common.evolution.EvolutionEmitRequest;
import com.recruitos.common.evolution.EvolutionSignalEmitter;
import com.recruitos.common.evolution.EvolutionSignalLevel;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class CampaignEvolutionEmitter {

    @Resource
    private EvolutionSignalEmitter evolutionSignalEmitter;

    public void emitScreen(Long jobId, Long campaignId, Long traceId, Long candidateId,
                           boolean passed, Map<String, Object> tagAdjustments) {
        EvolutionEmitRequest req = base(jobId, campaignId, traceId, candidateId);
        req.setSignalLevel(EvolutionSignalLevel.L3_SCREEN);
        req.setSourceEvent(passed ? "SCREEN_PASS" : "SCREEN_SKIP");
        req.setTagAdjustments(tagAdjustments != null ? tagAdjustments : defaultTagAdjust(passed));
        evolutionSignalEmitter.emit(req);
    }

    public void emitGreet(Long jobId, Long campaignId, Long traceId, Long candidateId) {
        EvolutionEmitRequest req = base(jobId, campaignId, traceId, candidateId);
        req.setSignalLevel(EvolutionSignalLevel.L5_GREET_RESUME);
        req.setSourceEvent("GREET_SENT");
        evolutionSignalEmitter.emit(req);
    }

    public void emitResumeReceived(Long jobId, Long campaignId, Long traceId, Long candidateId) {
        EvolutionEmitRequest req = base(jobId, campaignId, traceId, candidateId);
        req.setSignalLevel(EvolutionSignalLevel.L5_GREET_RESUME);
        req.setSourceEvent("RESUME_RECEIVED");
        Map<String, Object> adj = new LinkedHashMap<>();
        adj.put("_resume", 1.0);
        req.setTagAdjustments(adj);
        evolutionSignalEmitter.emit(req);
    }

    public void emitRechat(Long jobId, Long campaignId, Long traceId, Long candidateId, int attempt) {
        EvolutionEmitRequest req = base(jobId, campaignId, traceId, candidateId);
        req.setSignalLevel(EvolutionSignalLevel.L4_REPLY);
        req.setSourceEvent("RECHAT_SENT");
        Map<String, Object> adj = new LinkedHashMap<>();
        adj.put("_rechat", (double) attempt);
        req.setTagAdjustments(adj);
        evolutionSignalEmitter.emit(req);
    }

    public void emitCandidateReply(Long jobId, Long campaignId, Long traceId, Long candidateId) {
        EvolutionEmitRequest req = base(jobId, campaignId, traceId, candidateId);
        req.setSignalLevel(EvolutionSignalLevel.L4_REPLY);
        req.setSourceEvent("CANDIDATE_REPLY");
        evolutionSignalEmitter.emit(req);
    }

    private EvolutionEmitRequest base(Long jobId, Long campaignId, Long traceId, Long candidateId) {
        EvolutionEmitRequest req = new EvolutionEmitRequest();
        req.setJobId(jobId);
        req.setCampaignId(campaignId);
        req.setTraceId(traceId);
        req.setCandidateId(candidateId);
        req.setSourceModule("recruitos-agent");
        return req;
    }

    private Map<String, Object> defaultTagAdjust(boolean passed) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("_screen", passed ? 0.05 : -0.05);
        return m;
    }
}
