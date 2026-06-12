package com.recruitos.brain.domain.cognitive;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 认知层：AI判断
 * AI的独立观点，必须包含自我质疑(contradiction)
 */
public class CognitiveJudgment implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;
    private Long tenantId;
    private String judgmentType;     // CANDIDATE_OPINION / PIPELINE_HEALTH / HIRING_RISK / INTERVIEW_QUALITY / DECISION_CONSISTENCY / TEAM_COMPOSITION / PROCESS_BOTTLENECK
    private String subjectType;
    private Long subjectId;
    private String judgmentText;     // 自然语言判断
    private String judgmentJson;     // 结构化判断：{primary, confidence, key_evidence, alternative_view, contradiction, similar_past_cases}
    private BigDecimal confidence;
    private String evidenceMemory;   // JSON: 引用的记忆事件ID列表
    private String contradiction;    // JSON: AI自我质疑 "这个判断可能错在哪？"
    private String status;           // DRAFT / PUBLISHED / SUPERSEDED / WITHDRAWN
    private LocalDateTime createdAt;
    private Long supersededBy;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getTenantId() { return tenantId; }
    public void setTenantId(Long tenantId) { this.tenantId = tenantId; }
    public String getJudgmentType() { return judgmentType; }
    public void setJudgmentType(String judgmentType) { this.judgmentType = judgmentType; }
    public String getSubjectType() { return subjectType; }
    public void setSubjectType(String subjectType) { this.subjectType = subjectType; }
    public Long getSubjectId() { return subjectId; }
    public void setSubjectId(Long subjectId) { this.subjectId = subjectId; }
    public String getJudgmentText() { return judgmentText; }
    public void setJudgmentText(String judgmentText) { this.judgmentText = judgmentText; }
    public String getJudgmentJson() { return judgmentJson; }
    public void setJudgmentJson(String judgmentJson) { this.judgmentJson = judgmentJson; }
    public BigDecimal getConfidence() { return confidence; }
    public void setConfidence(BigDecimal confidence) { this.confidence = confidence; }
    public String getEvidenceMemory() { return evidenceMemory; }
    public void setEvidenceMemory(String evidenceMemory) { this.evidenceMemory = evidenceMemory; }
    public String getContradiction() { return contradiction; }
    public void setContradiction(String contradiction) { this.contradiction = contradiction; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public Long getSupersededBy() { return supersededBy; }
    public void setSupersededBy(Long supersededBy) { this.supersededBy = supersededBy; }
}
