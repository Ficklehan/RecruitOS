package com.recruitos.brain.domain.cognitive;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 认知层：用户画像
 * AI对每个用户的认知：决策风格、评分偏差、盲区
 */
public class CognitiveUserModel implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;
    private Long tenantId;
    private Long userId;
    private String role;             // HIRING_MANAGER / INTERVIEWER / RECRUITER / HRBP / BAR_RAISER
    private BigDecimal decisionSpeed;     // z-score
    private BigDecimal riskTolerance;     // z-score
    private BigDecimal standardRigidity;  // z-score
    private String scoringBiasJson;       // JSON: 每个维度的系统性偏差
    private BigDecimal leniencyIndex;
    private String biasAwareness;         // JSON: 已知偏差模式
    private String blindSpotsJson;        // JSON: 历史上反复忽视的信号
    private String decisionQualityTrend;  // JSON: 决策质量时间序列
    private BigDecimal patternStability;
    private Integer totalDecisions;
    private LocalDateTime lastEvaluatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getTenantId() { return tenantId; }
    public void setTenantId(Long tenantId) { this.tenantId = tenantId; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public BigDecimal getDecisionSpeed() { return decisionSpeed; }
    public void setDecisionSpeed(BigDecimal decisionSpeed) { this.decisionSpeed = decisionSpeed; }
    public BigDecimal getRiskTolerance() { return riskTolerance; }
    public void setRiskTolerance(BigDecimal riskTolerance) { this.riskTolerance = riskTolerance; }
    public BigDecimal getStandardRigidity() { return standardRigidity; }
    public void setStandardRigidity(BigDecimal standardRigidity) { this.standardRigidity = standardRigidity; }
    public String getScoringBiasJson() { return scoringBiasJson; }
    public void setScoringBiasJson(String scoringBiasJson) { this.scoringBiasJson = scoringBiasJson; }
    public BigDecimal getLeniencyIndex() { return leniencyIndex; }
    public void setLeniencyIndex(BigDecimal leniencyIndex) { this.leniencyIndex = leniencyIndex; }
    public String getBiasAwareness() { return biasAwareness; }
    public void setBiasAwareness(String biasAwareness) { this.biasAwareness = biasAwareness; }
    public String getBlindSpotsJson() { return blindSpotsJson; }
    public void setBlindSpotsJson(String blindSpotsJson) { this.blindSpotsJson = blindSpotsJson; }
    public String getDecisionQualityTrend() { return decisionQualityTrend; }
    public void setDecisionQualityTrend(String decisionQualityTrend) { this.decisionQualityTrend = decisionQualityTrend; }
    public BigDecimal getPatternStability() { return patternStability; }
    public void setPatternStability(BigDecimal patternStability) { this.patternStability = patternStability; }
    public Integer getTotalDecisions() { return totalDecisions; }
    public void setTotalDecisions(Integer totalDecisions) { this.totalDecisions = totalDecisions; }
    public LocalDateTime getLastEvaluatedAt() { return lastEvaluatedAt; }
    public void setLastEvaluatedAt(LocalDateTime lastEvaluatedAt) { this.lastEvaluatedAt = lastEvaluatedAt; }
}
