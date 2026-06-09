package com.recruitos.agent.dto;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public class CampaignCandidateTraceVO implements Serializable {

    private Long id;
    private String platform;
    private String candidateName;
    private String phone;
    private String traceStatus;
    private String skipReason;
    private String screenStage;
    private String skipReasonSummary;
    private String greetStrategyApplied;
    private Integer opsPackVersion;
    private Long lockedByAccountId;
    private String lockedByAccountName;
    private BigDecimal matchScore;
    private String matchDetail;
    private Long candidateId;
    private Long resumeId;
    private LocalDateTime updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getPlatform() { return platform; }
    public void setPlatform(String platform) { this.platform = platform; }
    public String getCandidateName() { return candidateName; }
    public void setCandidateName(String candidateName) { this.candidateName = candidateName; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getTraceStatus() { return traceStatus; }
    public void setTraceStatus(String traceStatus) { this.traceStatus = traceStatus; }
    public String getSkipReason() { return skipReason; }
    public void setSkipReason(String skipReason) { this.skipReason = skipReason; }
    public String getScreenStage() { return screenStage; }
    public void setScreenStage(String screenStage) { this.screenStage = screenStage; }
    public String getSkipReasonSummary() { return skipReasonSummary; }
    public void setSkipReasonSummary(String skipReasonSummary) { this.skipReasonSummary = skipReasonSummary; }
    public String getGreetStrategyApplied() { return greetStrategyApplied; }
    public void setGreetStrategyApplied(String greetStrategyApplied) { this.greetStrategyApplied = greetStrategyApplied; }
    public Integer getOpsPackVersion() { return opsPackVersion; }
    public void setOpsPackVersion(Integer opsPackVersion) { this.opsPackVersion = opsPackVersion; }
    public Long getLockedByAccountId() { return lockedByAccountId; }
    public void setLockedByAccountId(Long lockedByAccountId) { this.lockedByAccountId = lockedByAccountId; }
    public String getLockedByAccountName() { return lockedByAccountName; }
    public void setLockedByAccountName(String lockedByAccountName) { this.lockedByAccountName = lockedByAccountName; }
    public BigDecimal getMatchScore() { return matchScore; }
    public void setMatchScore(BigDecimal matchScore) { this.matchScore = matchScore; }
    public String getMatchDetail() { return matchDetail; }
    public void setMatchDetail(String matchDetail) { this.matchDetail = matchDetail; }
    public Long getCandidateId() { return candidateId; }
    public void setCandidateId(Long candidateId) { this.candidateId = candidateId; }
    public Long getResumeId() { return resumeId; }
    public void setResumeId(Long resumeId) { this.resumeId = resumeId; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
