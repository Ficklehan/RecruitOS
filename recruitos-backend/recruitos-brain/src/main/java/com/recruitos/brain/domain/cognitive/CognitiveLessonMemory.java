package com.recruitos.brain.domain.cognitive;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 认知层：教训记忆
 * 从负面结果中学到的："3个表现差的录用，面试时都缺少XXX信号"
 */
public class CognitiveLessonMemory implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;
    private Long tenantId;
    private String lessonType;       // BAD_HIRE_PATTERN / MISSED_GOOD_CANDIDATE / INTERVIEW_BLIND_SPOT / PROCESS_FAILURE
    private String title;
    private String description;
    private String evidence;         // JSON
    private String correctiveAction;
    private String severity;         // CRITICAL / IMPORTANT / NOTABLE
    private String status;           // ACTIVE / ADDRESSED / STALE
    private LocalDateTime learnedAt;
    private LocalDateTime addressedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getTenantId() { return tenantId; }
    public void setTenantId(Long tenantId) { this.tenantId = tenantId; }
    public String getLessonType() { return lessonType; }
    public void setLessonType(String lessonType) { this.lessonType = lessonType; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getEvidence() { return evidence; }
    public void setEvidence(String evidence) { this.evidence = evidence; }
    public String getCorrectiveAction() { return correctiveAction; }
    public void setCorrectiveAction(String correctiveAction) { this.correctiveAction = correctiveAction; }
    public String getSeverity() { return severity; }
    public void setSeverity(String severity) { this.severity = severity; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDateTime getLearnedAt() { return learnedAt; }
    public void setLearnedAt(LocalDateTime learnedAt) { this.learnedAt = learnedAt; }
    public LocalDateTime getAddressedAt() { return addressedAt; }
    public void setAddressedAt(LocalDateTime addressedAt) { this.addressedAt = addressedAt; }
}
