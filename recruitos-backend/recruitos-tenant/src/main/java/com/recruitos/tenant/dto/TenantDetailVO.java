package com.recruitos.tenant.dto;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class TenantDetailVO implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;
    private String tenantCode;
    private String companyName;
    private String creditCode;
    private String plan;
    private Integer status;
    private LocalDateTime trialEndTime;
    private LocalDateTime createdAt;

    // License info
    private Integer maxJobs;
    private Integer maxAgents;
    private Integer usedJobs;
    private Integer usedAgents;
    private Integer resumeQuota;
    private Integer resumeUsed;
    private Integer messageQuota;
    private Integer messageUsed;
    private LocalDate licenseStartDate;
    private LocalDate licenseEndDate;
    private Integer graceDays;
    private Integer licenseStatus;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTenantCode() { return tenantCode; }
    public void setTenantCode(String tenantCode) { this.tenantCode = tenantCode; }
    public String getCompanyName() { return companyName; }
    public void setCompanyName(String companyName) { this.companyName = companyName; }
    public String getCreditCode() { return creditCode; }
    public void setCreditCode(String creditCode) { this.creditCode = creditCode; }
    public String getPlan() { return plan; }
    public void setPlan(String plan) { this.plan = plan; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
    public LocalDateTime getTrialEndTime() { return trialEndTime; }
    public void setTrialEndTime(LocalDateTime trialEndTime) { this.trialEndTime = trialEndTime; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public Integer getMaxJobs() { return maxJobs; }
    public void setMaxJobs(Integer maxJobs) { this.maxJobs = maxJobs; }
    public Integer getMaxAgents() { return maxAgents; }
    public void setMaxAgents(Integer maxAgents) { this.maxAgents = maxAgents; }
    public Integer getUsedJobs() { return usedJobs; }
    public void setUsedJobs(Integer usedJobs) { this.usedJobs = usedJobs; }
    public Integer getUsedAgents() { return usedAgents; }
    public void setUsedAgents(Integer usedAgents) { this.usedAgents = usedAgents; }
    public Integer getResumeQuota() { return resumeQuota; }
    public void setResumeQuota(Integer resumeQuota) { this.resumeQuota = resumeQuota; }
    public Integer getResumeUsed() { return resumeUsed; }
    public void setResumeUsed(Integer resumeUsed) { this.resumeUsed = resumeUsed; }
    public Integer getMessageQuota() { return messageQuota; }
    public void setMessageQuota(Integer messageQuota) { this.messageQuota = messageQuota; }
    public Integer getMessageUsed() { return messageUsed; }
    public void setMessageUsed(Integer messageUsed) { this.messageUsed = messageUsed; }
    public LocalDate getLicenseStartDate() { return licenseStartDate; }
    public void setLicenseStartDate(LocalDate licenseStartDate) { this.licenseStartDate = licenseStartDate; }
    public LocalDate getLicenseEndDate() { return licenseEndDate; }
    public void setLicenseEndDate(LocalDate licenseEndDate) { this.licenseEndDate = licenseEndDate; }
    public Integer getGraceDays() { return graceDays; }
    public void setGraceDays(Integer graceDays) { this.graceDays = graceDays; }
    public Integer getLicenseStatus() { return licenseStatus; }
    public void setLicenseStatus(Integer licenseStatus) { this.licenseStatus = licenseStatus; }
}
