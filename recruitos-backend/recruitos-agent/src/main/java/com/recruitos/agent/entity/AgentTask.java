package com.recruitos.agent.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.recruitos.common.mybatis.BaseEntity;

import java.time.LocalDateTime;

/**
 * Agent task entity - represents an automated recruiting task
 */
@TableName("agent_task")
public class AgentTask extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /** Task type: SEARCH / CONTACT / FOLLOWUP / SCHEDULE */
    private String taskType;

    /** Associated job ID */
    private Long jobId;

    /** Job title */
    private String jobTitle;

    /** Agent account ID executing this task */
    private Long agentAccountId;

    /** Platform: BOSS / LAGOU / ZHILIAN / LIEPIN / OTHER */
    private String platform;

    /** Status: PENDING / RUNNING / PAUSED / COMPLETED / FAILED */
    private String status;

    /** Priority: 1=high, 2=medium, 3=low */
    private Integer priority;

    /** Target count */
    private Integer targetCount;

    /** Completed count */
    private Integer completedCount;

    /** Failed count */
    private Integer failedCount;

    /** Task started timestamp */
    private LocalDateTime startedAt;

    /** Task completed timestamp */
    private LocalDateTime completedAt;

    /** Error message if failed */
    private String errorMessage;

    /** Created by user ID */
    private Long createdBy;

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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
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

    public Integer getCompletedCount() {
        return completedCount;
    }

    public void setCompletedCount(Integer completedCount) {
        this.completedCount = completedCount;
    }

    public Integer getFailedCount() {
        return failedCount;
    }

    public void setFailedCount(Integer failedCount) {
        this.failedCount = failedCount;
    }

    public LocalDateTime getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(LocalDateTime startedAt) {
        this.startedAt = startedAt;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public Long getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(Long createdBy) {
        this.createdBy = createdBy;
    }
}
