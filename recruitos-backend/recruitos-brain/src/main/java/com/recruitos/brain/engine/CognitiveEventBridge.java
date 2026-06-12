package com.recruitos.brain.engine;

import com.recruitos.brain.service.CognitiveMemoryService;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.*;

/**
 * 认知事件桥接器
 * 将 RecruitOS 业务事件翻译为认知层事件记忆。
 * 被 BrainEventConsumer 和各引擎调用，而非独立调度。
 */
@Component
public class CognitiveEventBridge {

    @Resource
    private CognitiveMemoryService memory;

    /**
     * 录用事件 → 认知记忆
     */
    public void onCandidateHired(Long tenantId, Long candidateId, Long jobId,
                                  Map<String, Object> context) {
        Long eventId = memory.recordEvent(tenantId, "HIRE", "CANDIDATE", candidateId,
            context, "ACCEPTED", null);

        // 更新候选人对象记忆
        Map<String, Object> profile = Map.of(
            "status", "HIRED",
            "jobId", jobId,
            "hiredAt", new Date().toString()
        );
        memory.updateObjectProfile(tenantId, "CANDIDATE", candidateId,
            "已被录用至岗位#" + jobId, profile,
            List.of(Map.of("type", "HIRED", "date", new Date().toString())),
            List.of()
        );
    }

    /**
     * 拒绝候选人事件
     */
    public void onCandidateRejected(Long tenantId, Long candidateId, Long jobId,
                                     String reason, Map<String, Object> context) {
        memory.recordEvent(tenantId, "REJECT", "CANDIDATE", candidateId,
            context, "REJECTED", reason);

        memory.updateObjectProfile(tenantId, "CANDIDATE", candidateId,
            "岗位#" + jobId + " 未通过", Map.of("status", "REJECTED", "reason", reason),
            List.of(Map.of("type", "REJECTED", "reason", reason)),
            List.of()
        );
    }

    /**
     * 员工离职事件 → 回溯标记录用决策质量
     */
    public void onEmployeeDeparted(Long tenantId, Long candidateId, String resignationReason) {
        // 找到该候选人的录用事件
        List<Map<String, Object>> events = memory.getObjectEvents(tenantId, "CANDIDATE", candidateId);
        if (events != null) {
            for (Map<String, Object> evt : events) {
                if ("HIRE".equals(evt.get("eventType"))) {
                    memory.markDecisionQuality(
                        (Long) evt.get("id"),
                        "POOR",
                        "员工于 " + new Date() + " 离职，原因: " + resignationReason
                    );
                }
            }
        }

        // 记录离职事件
        memory.recordEvent(tenantId, "DEPARTURE", "CANDIDATE", candidateId,
            Map.of("resignationReason", resignationReason),
            "RESIGNED", resignationReason);

        // 如果有相关教训模式，触发教训记忆
        // 后续由模式挖掘调度器处理
    }

    /**
     * Offer 被拒事件
     */
    public void onOfferDeclined(Long tenantId, Long candidateId, Long jobId,
                                 String declineReason, Map<String, Object> context) {
        memory.recordEvent(tenantId, "REJECT", "CANDIDATE", candidateId,
            context, "DECLINED", declineReason);
    }

    /**
     * 面试评价分歧事件
     */
    public void onInterviewDisagreement(Long tenantId, Long candidateId, Long jobId,
                                         List<Map<String, Object>> evaluatorScores) {
        memory.recordEvent(tenantId, "INTERVIEW_DISAGREEMENT", "CANDIDATE", candidateId,
            Map.of("jobId", jobId, "evaluatorScores", evaluatorScores),
            "DISAGREEMENT", "面试官评分存在显著分歧");
    }

    /**
     * 管道停滞事件
     */
    public void onPipelineBlocked(Long tenantId, Long jobId, int daysWithoutNewCandidate) {
        memory.recordEvent(tenantId, "PIPELINE_BLOCKAGE", "JOB", jobId,
            Map.of("daysWithoutNewCandidate", daysWithoutNewCandidate),
            "BLOCKED", "管道已" + daysWithoutNewCandidate + "天无新增候选人");
    }

    /**
     * HR 覆盖 AI 筛选结果的事件
     */
    public void onScreeningOverride(Long tenantId, Long candidateId, Long jobId,
                                     String aiRecommendation, String hrDecision, String hrReason) {
        memory.recordEvent(tenantId, "SCREENING_OVERRIDE", "CANDIDATE", candidateId,
            Map.of("jobId", jobId, "aiRecommendation", aiRecommendation, "hrDecision", hrDecision),
            hrDecision, hrReason);
    }
}
