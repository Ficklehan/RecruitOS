package com.recruitos.agent.dto;

import java.io.Serializable;

public class RecruitmentChannelCreateDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String channelCode;
    private String channelName;
    private String channelType;
    private String platformCode;
    private String description;
    private String status;
    private Integer sortOrder;
    private Integer supportsAgent;

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

    public Integer getSupportsAgent() { return supportsAgent; }
    public void setSupportsAgent(Integer supportsAgent) { this.supportsAgent = supportsAgent; }
}
