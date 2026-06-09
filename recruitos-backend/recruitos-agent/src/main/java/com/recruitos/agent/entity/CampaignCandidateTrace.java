package com.recruitos.agent.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@TableName("campaign_candidate_trace")
public class CampaignCandidateTrace implements Serializable {

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;
    @TableField("tenant_id")
    private Long tenantId;
    private Long campaignId;
    private Long platformRunId;
    private Long jobId;
    private String platform;
    private Long accountId;
    private String platformUserId;
    private String candidateName;
    private String phone;
    private String email;
    private String dedupKey;
    private String traceStatus;
    private BigDecimal matchScore;
    private Long candidateId;
    private Long resumeId;
    private String skipReason;
    private String screenStage;
    private String skipReasonJson;
    private String greetStrategyApplied;
    private Integer opsPackVersion;
    private Long lockedByAccountId;
    private String timelineJson;
    @TableField("created_at")
    private LocalDateTime createdAt;
    @TableField("updated_at")
    private LocalDateTime updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getTenantId() { return tenantId; }
    public void setTenantId(Long tenantId) { this.tenantId = tenantId; }
    public Long getCampaignId() { return campaignId; }
    public void setCampaignId(Long campaignId) { this.campaignId = campaignId; }
    public Long getPlatformRunId() { return platformRunId; }
    public void setPlatformRunId(Long platformRunId) { this.platformRunId = platformRunId; }
    public Long getJobId() { return jobId; }
    public void setJobId(Long jobId) { this.jobId = jobId; }
    public String getPlatform() { return platform; }
    public void setPlatform(String platform) { this.platform = platform; }
    public Long getAccountId() { return accountId; }
    public void setAccountId(Long accountId) { this.accountId = accountId; }
    public String getPlatformUserId() { return platformUserId; }
    public void setPlatformUserId(String platformUserId) { this.platformUserId = platformUserId; }
    public String getCandidateName() { return candidateName; }
    public void setCandidateName(String candidateName) { this.candidateName = candidateName; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getDedupKey() { return dedupKey; }
    public void setDedupKey(String dedupKey) { this.dedupKey = dedupKey; }
    public String getTraceStatus() { return traceStatus; }
    public void setTraceStatus(String traceStatus) { this.traceStatus = traceStatus; }
    public BigDecimal getMatchScore() { return matchScore; }
    public void setMatchScore(BigDecimal matchScore) { this.matchScore = matchScore; }
    public Long getCandidateId() { return candidateId; }
    public void setCandidateId(Long candidateId) { this.candidateId = candidateId; }
    public Long getResumeId() { return resumeId; }
    public void setResumeId(Long resumeId) { this.resumeId = resumeId; }
    public String getSkipReason() { return skipReason; }
    public void setSkipReason(String skipReason) { this.skipReason = skipReason; }
    public String getScreenStage() { return screenStage; }
    public void setScreenStage(String screenStage) { this.screenStage = screenStage; }
    public String getSkipReasonJson() { return skipReasonJson; }
    public void setSkipReasonJson(String skipReasonJson) { this.skipReasonJson = skipReasonJson; }
    public String getGreetStrategyApplied() { return greetStrategyApplied; }
    public void setGreetStrategyApplied(String greetStrategyApplied) { this.greetStrategyApplied = greetStrategyApplied; }
    public Integer getOpsPackVersion() { return opsPackVersion; }
    public void setOpsPackVersion(Integer opsPackVersion) { this.opsPackVersion = opsPackVersion; }
    public Long getLockedByAccountId() { return lockedByAccountId; }
    public void setLockedByAccountId(Long lockedByAccountId) { this.lockedByAccountId = lockedByAccountId; }
    public String getTimelineJson() { return timelineJson; }
    public void setTimelineJson(String timelineJson) { this.timelineJson = timelineJson; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
