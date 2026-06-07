package com.recruitos.agent.dto;

import java.io.Serializable;

/**
 * DTO for querying agent tasks
 */
public class AgentTaskQueryDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** Filter by task type */
    private String taskType;

    /** Filter by platform */
    private String platform;

    /** Filter by status */
    private String status;

    /** Filter by agent account ID */
    private Long agentAccountId;

    /** Filter by job ID */
    private Long jobId;

    /** Page number (default 1) */
    private Integer pageNum = 1;

    /** Page size (default 10) */
    private Integer pageSize = 10;

    // Getters and Setters

    public String getTaskType() {
        return taskType;
    }

    public void setTaskType(String taskType) {
        this.taskType = taskType;
    }

    public String getPlatform() {
        return platform;
    }

    public void setPlatform(String platform) {
        this.platform = platform;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Long getAgentAccountId() {
        return agentAccountId;
    }

    public void setAgentAccountId(Long agentAccountId) {
        this.agentAccountId = agentAccountId;
    }

    public Long getJobId() {
        return jobId;
    }

    public void setJobId(Long jobId) {
        this.jobId = jobId;
    }

    public Integer getPageNum() {
        return pageNum;
    }

    public void setPageNum(Integer pageNum) {
        this.pageNum = pageNum;
    }

    public Integer getPageSize() {
        return pageSize;
    }

    public void setPageSize(Integer pageSize) {
        this.pageSize = pageSize;
    }
}
