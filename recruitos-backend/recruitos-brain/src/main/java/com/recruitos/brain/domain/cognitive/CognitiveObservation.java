package com.recruitos.brain.domain.cognitive;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 认知层：AI主动观察
 * AI主动推送给用户的洞察，按严重程度分级
 */
public class CognitiveObservation implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;
    private Long tenantId;
    private String observationType;  // ALERT / INSIGHT / SUGGESTION / QUESTION
    private String severity;         // CRITICAL / WARNING / INFO / CURIOSITY
    private String title;            // 一句话摘要
    private String body;             // 完整叙述
    private String relatedObjects;   // JSON: [{type: "JOB", id: 42}, ...]
    private String suggestedAction;  // JSON: {text, action_type, params}
    private String actionTaken;      // PENDING / EXECUTED / DISMISSED / DEFERRED
    private Long actionTakenBy;
    private LocalDateTime actionTakenAt;
    private String feedback;         // HELPFUL / NOT_HELPFUL / PARTIALLY / NONE
    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getTenantId() { return tenantId; }
    public void setTenantId(Long tenantId) { this.tenantId = tenantId; }
    public String getObservationType() { return observationType; }
    public void setObservationType(String observationType) { this.observationType = observationType; }
    public String getSeverity() { return severity; }
    public void setSeverity(String severity) { this.severity = severity; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getBody() { return body; }
    public void setBody(String body) { this.body = body; }
    public String getRelatedObjects() { return relatedObjects; }
    public void setRelatedObjects(String relatedObjects) { this.relatedObjects = relatedObjects; }
    public String getSuggestedAction() { return suggestedAction; }
    public void setSuggestedAction(String suggestedAction) { this.suggestedAction = suggestedAction; }
    public String getActionTaken() { return actionTaken; }
    public void setActionTaken(String actionTaken) { this.actionTaken = actionTaken; }
    public Long getActionTakenBy() { return actionTakenBy; }
    public void setActionTakenBy(Long actionTakenBy) { this.actionTakenBy = actionTakenBy; }
    public LocalDateTime getActionTakenAt() { return actionTakenAt; }
    public void setActionTakenAt(LocalDateTime actionTakenAt) { this.actionTakenAt = actionTakenAt; }
    public String getFeedback() { return feedback; }
    public void setFeedback(String feedback) { this.feedback = feedback; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }
}
