package com.recruitos.agent.dto;

import java.io.Serializable;

/**
 * DTO for creating an agent task
 */
public class AgentTaskCreateDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** Task type: SEARCH / CONTACT / FOLLOWUP / SCHEDULE */
    private String taskType;

    /** Associated job ID */
    private Long jobId;

    /** Job title */
    private String jobTitle;

    /** Agent account ID */
    private Long agentAccountId;

    /** Platform: BOSS / LAGOU / ZHILIAN / LIEPIN / OTHER */
    private String platform;

    /** Priority: 1=high, 2=medium, 3=low */
    private Integer priority;

    /** Target count */
    private Integer targetCount;

    // Getters and Setters

    public String getTaskType() {
        return taskType;
    }

    public void setTaskType(String taskType) {
        this.taskType = taskType;
    }

    public Long getJobId() {
        return jobId;
    }

    public void setJobId(Long jobId) {
        this.jobId = jobId;
    }

    public String getJobTitle() {
        return jobTitle;
    }

    public void setJobTitle(String jobTitle) {
        this.jobTitle = jobTitle;
    }

    public Long getAgentAccountId() {
        return agentAccountId;
    }

    public void setAgentAccountId(Long agentAccountId) {
        this.agentAccountId = agentAccountId;
    }

    public String getPlatform() {
        return platform;
    }

    public void setPlatform(String platform) {
        this.platform = platform;
    }

    public Integer getPriority() {
        return priority;
    }

    public void setPriority(Integer priority) {
        this.priority = priority;
    }

    public Integer getTargetCount() {
        return targetCount;
    }

    public void setTargetCount(Integer targetCount) {
        this.targetCount = targetCount;
    }
}
