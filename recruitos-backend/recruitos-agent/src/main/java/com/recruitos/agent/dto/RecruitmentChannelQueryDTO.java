package com.recruitos.agent.dto;

import java.io.Serializable;

public class RecruitmentChannelQueryDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String channelType;
    private String status;
    private String keyword;
    private Boolean supportsAgentOnly;
    private Integer pageNum = 1;
    private Integer pageSize = 20;

    public String getChannelType() { return channelType; }
    public void setChannelType(String channelType) { this.channelType = channelType; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getKeyword() { return keyword; }
    public void setKeyword(String keyword) { this.keyword = keyword; }

    public Boolean getSupportsAgentOnly() { return supportsAgentOnly; }
    public void setSupportsAgentOnly(Boolean supportsAgentOnly) { this.supportsAgentOnly = supportsAgentOnly; }

    public Integer getPageNum() { return pageNum; }
    public void setPageNum(Integer pageNum) { this.pageNum = pageNum; }

    public Integer getPageSize() { return pageSize; }
    public void setPageSize(Integer pageSize) { this.pageSize = pageSize; }
}
