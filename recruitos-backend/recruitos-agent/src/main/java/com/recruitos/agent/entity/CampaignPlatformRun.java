package com.recruitos.agent.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.io.Serializable;
import java.time.LocalDateTime;

@TableName("campaign_platform_run")
public class CampaignPlatformRun implements Serializable {

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;
    @TableField("tenant_id")
    private Long tenantId;
    private Long campaignId;
    private String platform;
    private Long primaryAccountId;
    private String auxiliaryAccountIds;
    private String currentStep;
    private Long currentAccountId;
    private String platformJobUrl;
    private String status;
    private String statsJson;
    private String errorMessage;
    private LocalDateTime startedAt;
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
    public String getPlatform() { return platform; }
    public void setPlatform(String platform) { this.platform = platform; }
    public Long getPrimaryAccountId() { return primaryAccountId; }
    public void setPrimaryAccountId(Long primaryAccountId) { this.primaryAccountId = primaryAccountId; }
    public String getAuxiliaryAccountIds() { return auxiliaryAccountIds; }
    public void setAuxiliaryAccountIds(String auxiliaryAccountIds) { this.auxiliaryAccountIds = auxiliaryAccountIds; }
    public String getCurrentStep() { return currentStep; }
    public void setCurrentStep(String currentStep) { this.currentStep = currentStep; }
    public Long getCurrentAccountId() { return currentAccountId; }
    public void setCurrentAccountId(Long currentAccountId) { this.currentAccountId = currentAccountId; }
    public String getPlatformJobUrl() { return platformJobUrl; }
    public void setPlatformJobUrl(String platformJobUrl) { this.platformJobUrl = platformJobUrl; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getStatsJson() { return statsJson; }
    public void setStatsJson(String statsJson) { this.statsJson = statsJson; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    public LocalDateTime getStartedAt() { return startedAt; }
    public void setStartedAt(LocalDateTime startedAt) { this.startedAt = startedAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
