package com.recruitos.tenant.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

@TableName("tenant_license")
public class TenantLicense implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;
    private Long tenantId;
    private String plan;
    private Integer maxJobs;
    private Integer maxAgents;
    private Integer usedJobs;
    private Integer usedAgents;
    private Integer resumeQuota;
    private Integer resumeUsed;
    private Integer messageQuota;
    private Integer messageUsed;
    private LocalDate startDate;
    private LocalDate endDate;
    private Integer graceDays;
    private Integer status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getTenantId() { return tenantId; }
    public void setTenantId(Long tenantId) { this.tenantId = tenantId; }
    public String getPlan() { return plan; }
    public void setPlan(String plan) { this.plan = plan; }
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
    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }
    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }
    public Integer getGraceDays() { return graceDays; }
    public void setGraceDays(Integer graceDays) { this.graceDays = graceDays; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
