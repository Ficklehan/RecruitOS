package com.recruitos.agent.dto;

import java.io.Serializable;
import java.time.LocalDateTime;

public class RecruitmentChannelVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private Long tenantId;
    private String channelCode;
    private String channelName;
    private String channelType;
    private String platformCode;
    private String description;
    private String status;
    private Integer sortOrder;
    private Boolean supportsAgent;
    private Boolean system;
    private Integer accountCount;
    private Integer activeAccountCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getTenantId() { return tenantId; }
    public void setTenantId(Long tenantId) { this.tenantId = tenantId; }

    public String getChannelCode() { return channelCode; }
    public void setChannelCode(String channelCode) { this.channelCode = channelCode; }

    public String getChannelName() { return channelName; }
    public void setChannelName(String channelName) { this.channelName = channelName; }

    public String getChannelType() { return channelType; }
    public void setChannelType(String channelType) { this.channelType = channelType; }

    public String getPlatformCode() { return platformCode; }
    public void setPlatformCode(String platformCode) { this.platformCode = platformCode; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }

    public Boolean getSupportsAgent() { return supportsAgent; }
    public void setSupportsAgent(Boolean supportsAgent) { this.supportsAgent = supportsAgent; }

    public Boolean getSystem() { return system; }
    public void setSystem(Boolean system) { this.system = system; }

    public Integer getAccountCount() { return accountCount; }
    public void setAccountCount(Integer accountCount) { this.accountCount = accountCount; }

    public Integer getActiveAccountCount() { return activeAccountCount; }
    public void setActiveAccountCount(Integer activeAccountCount) { this.activeAccountCount = activeAccountCount; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
