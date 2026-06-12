package com.recruitos.brain.domain.cognitive;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 认知层：模式记忆
 * 从事件中提炼的统计规律，如"来自阿里的候选人Offer接受率高12%但18月离职率高8%"
 */
public class CognitivePatternMemory implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;
    private Long tenantId;
    private String patternType;      // CANDIDATE_SOURCE_PERFORMANCE / INTERVIEWER_SCORING_BIAS / SKILL_RETENTION_CORRELATION / OFFER_ACCEPTANCE_FACTOR / DEPARTURE_EARLY_SIGNAL / TEAM_COMPOSITION_RISK / HIRING_MANAGER_PATTERN
    private String patternName;
    private String patternRule;       // JSON: {statement, confidence, sample_size, statistical_test, ...}
    private String evidenceEvents;    // JSON: [101, 203, 307, ...]
    private BigDecimal confidence;
    private Integer sampleSize;
    private String status;            // ACTIVE / STALE / INVALIDATED
    private LocalDateTime discoveredAt;
    private LocalDateTime lastValidatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getTenantId() { return tenantId; }
    public void setTenantId(Long tenantId) { this.tenantId = tenantId; }
    public String getPatternType() { return patternType; }
    public void setPatternType(String patternType) { this.patternType = patternType; }
    public String getPatternName() { return patternName; }
    public void setPatternName(String patternName) { this.patternName = patternName; }
    public String getPatternRule() { return patternRule; }
    public void setPatternRule(String patternRule) { this.patternRule = patternRule; }
    public String getEvidenceEvents() { return evidenceEvents; }
    public void setEvidenceEvents(String evidenceEvents) { this.evidenceEvents = evidenceEvents; }
    public BigDecimal getConfidence() { return confidence; }
    public void setConfidence(BigDecimal confidence) { this.confidence = confidence; }
    public Integer getSampleSize() { return sampleSize; }
    public void setSampleSize(Integer sampleSize) { this.sampleSize = sampleSize; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDateTime getDiscoveredAt() { return discoveredAt; }
    public void setDiscoveredAt(LocalDateTime discoveredAt) { this.discoveredAt = discoveredAt; }
    public LocalDateTime getLastValidatedAt() { return lastValidatedAt; }
    public void setLastValidatedAt(LocalDateTime lastValidatedAt) { this.lastValidatedAt = lastValidatedAt; }
}
