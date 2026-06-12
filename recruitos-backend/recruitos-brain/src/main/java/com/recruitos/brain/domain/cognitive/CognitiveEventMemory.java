package com.recruitos.brain.domain.cognitive;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 认知层：事件记忆
 * 每次录用/拒绝/离职/Offer谈判的完整上下文快照，不可变。
 */
public class CognitiveEventMemory implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;
    private Long tenantId;
    private String eventType;       // HIRE / REJECT / DEPARTURE / OFFER_NEGOTIATION / CANDIDATE_DISENGAGE / PIPELINE_BLOCKAGE / INTERVIEW_DISAGREEMENT / SCREENING_OVERRIDE
    private String eventSubject;     // CANDIDATE / JOB / INTERVIEWER
    private Long subjectId;
    private String contextJson;      // 完整上下文 JSON
    private String outcome;          // ACCEPTED / REJECTED / RESIGNED / DECLINED
    private String outcomeReason;
    private String decisionQuality;  // GOOD / NEUTRAL / POOR / UNKNOWN
    private LocalDateTime occurredAt;
    private LocalDateTime recordedAt;

    // --- getters/setters ---
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getTenantId() { return tenantId; }
    public void setTenantId(Long tenantId) { this.tenantId = tenantId; }
    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }
    public String getEventSubject() { return eventSubject; }
    public void setEventSubject(String eventSubject) { this.eventSubject = eventSubject; }
    public Long getSubjectId() { return subjectId; }
    public void setSubjectId(Long subjectId) { this.subjectId = subjectId; }
    public String getContextJson() { return contextJson; }
    public void setContextJson(String contextJson) { this.contextJson = contextJson; }
    public String getOutcome() { return outcome; }
    public void setOutcome(String outcome) { this.outcome = outcome; }
    public String getOutcomeReason() { return outcomeReason; }
    public void setOutcomeReason(String outcomeReason) { this.outcomeReason = outcomeReason; }
    public String getDecisionQuality() { return decisionQuality; }
    public void setDecisionQuality(String decisionQuality) { this.decisionQuality = decisionQuality; }
    public LocalDateTime getOccurredAt() { return occurredAt; }
    public void setOccurredAt(LocalDateTime occurredAt) { this.occurredAt = occurredAt; }
    public LocalDateTime getRecordedAt() { return recordedAt; }
    public void setRecordedAt(LocalDateTime recordedAt) { this.recordedAt = recordedAt; }
}
