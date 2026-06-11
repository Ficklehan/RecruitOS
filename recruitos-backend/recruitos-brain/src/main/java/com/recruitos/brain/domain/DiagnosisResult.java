package com.recruitos.brain.domain;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * AI诊断结果 — 巡检发现的招聘健康问题。
 */
public class DiagnosisResult implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;
    private Long tenantId;
    private String diagnosisType;      // JOB_HEALTH / FUNNEL_BOTTLENECK / INTERVIEWER_BIAS / CHANNEL_ROI
    private Long targetId;
    private String targetType;          // JOB / CAMPAIGN / INTERVIEWER
    private String severity;            // CRITICAL / WARNING / INFO
    private String title;
    private Map<String, Object> evidence;
    private String rootCause;
    private String recommendation;
    private String expectedImpact;
    private Double confidence;
    private String status;              // PENDING / ACKNOWLEDGED / DISMISSED / ACTIONED
    private Long acknowledgedBy;
    private LocalDateTime actionedAt;
    private LocalDateTime createdAt;

    // --- quick constructors ---
    public static DiagnosisResult of(String type, String severity, String title, String rootCause,
                                      String recommendation, Double confidence) {
        DiagnosisResult r = new DiagnosisResult();
        r.diagnosisType = type;
        r.severity = severity;
        r.title = title;
        r.rootCause = rootCause;
        r.recommendation = recommendation;
        r.confidence = confidence;
        r.status = "PENDING";
        r.createdAt = LocalDateTime.now();
        return r;
    }

    // Getters & Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getTenantId() { return tenantId; }
    public void setTenantId(Long tenantId) { this.tenantId = tenantId; }
    public String getDiagnosisType() { return diagnosisType; }
    public void setDiagnosisType(String diagnosisType) { this.diagnosisType = diagnosisType; }
    public Long getTargetId() { return targetId; }
    public void setTargetId(Long targetId) { this.targetId = targetId; }
    public String getTargetType() { return targetType; }
    public void setTargetType(String targetType) { this.targetType = targetType; }
    public String getSeverity() { return severity; }
    public void setSeverity(String severity) { this.severity = severity; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public Map<String, Object> getEvidence() { return evidence; }
    public void setEvidence(Map<String, Object> evidence) { this.evidence = evidence; }
    public String getRootCause() { return rootCause; }
    public void setRootCause(String rootCause) { this.rootCause = rootCause; }
    public String getRecommendation() { return recommendation; }
    public void setRecommendation(String recommendation) { this.recommendation = recommendation; }
    public String getExpectedImpact() { return expectedImpact; }
    public void setExpectedImpact(String expectedImpact) { this.expectedImpact = expectedImpact; }
    public Double getConfidence() { return confidence; }
    public void setConfidence(Double confidence) { this.confidence = confidence; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Long getAcknowledgedBy() { return acknowledgedBy; }
    public void setAcknowledgedBy(Long acknowledgedBy) { this.acknowledgedBy = acknowledgedBy; }
    public LocalDateTime getActionedAt() { return actionedAt; }
    public void setActionedAt(LocalDateTime actionedAt) { this.actionedAt = actionedAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
