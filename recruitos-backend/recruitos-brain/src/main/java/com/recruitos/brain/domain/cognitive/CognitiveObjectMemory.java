package com.recruitos.brain.domain.cognitive;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 认知层：对象记忆
 * 对每个候选人/面试官/岗位的累积认知画像
 */
public class CognitiveObjectMemory implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;
    private Long tenantId;
    private String objectType;       // CANDIDATE / JOB / INTERVIEWER / HIRING_MANAGER / TEAM
    private Long objectId;
    private String summaryTldr;      // AI对这个对象的一句话总结
    private String evolvingProfile;  // JSON: 随时间累积的认知画像
    private String keySignals;       // JSON: 关键信号摘要
    private String riskFlags;        // JSON: 风险标记
    private LocalDateTime lastUpdated;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getTenantId() { return tenantId; }
    public void setTenantId(Long tenantId) { this.tenantId = tenantId; }
    public String getObjectType() { return objectType; }
    public void setObjectType(String objectType) { this.objectType = objectType; }
    public Long getObjectId() { return objectId; }
    public void setObjectId(Long objectId) { this.objectId = objectId; }
    public String getSummaryTldr() { return summaryTldr; }
    public void setSummaryTldr(String summaryTldr) { this.summaryTldr = summaryTldr; }
    public String getEvolvingProfile() { return evolvingProfile; }
    public void setEvolvingProfile(String evolvingProfile) { this.evolvingProfile = evolvingProfile; }
    public String getKeySignals() { return keySignals; }
    public void setKeySignals(String keySignals) { this.keySignals = keySignals; }
    public String getRiskFlags() { return riskFlags; }
    public void setRiskFlags(String riskFlags) { this.riskFlags = riskFlags; }
    public LocalDateTime getLastUpdated() { return lastUpdated; }
    public void setLastUpdated(LocalDateTime lastUpdated) { this.lastUpdated = lastUpdated; }
}
