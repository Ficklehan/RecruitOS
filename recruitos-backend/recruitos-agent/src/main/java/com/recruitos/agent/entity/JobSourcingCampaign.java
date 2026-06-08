package com.recruitos.agent.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.recruitos.common.mybatis.BaseEntity;

import java.time.LocalDateTime;

@TableName("job_sourcing_campaign")
public class JobSourcingCampaign extends BaseEntity {

    private Long jobId;
    private String name;
    private String mode;
    private String status;
    private String configJson;
    private String statsJson;
    private Integer resumeConfirmRequired;
    private Integer publishConfirmRequired;
    private Long createdBy;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;

    public Long getJobId() { return jobId; }
    public void setJobId(Long jobId) { this.jobId = jobId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getMode() { return mode; }
    public void setMode(String mode) { this.mode = mode; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getConfigJson() { return configJson; }
    public void setConfigJson(String configJson) { this.configJson = configJson; }
    public String getStatsJson() { return statsJson; }
    public void setStatsJson(String statsJson) { this.statsJson = statsJson; }
    public Integer getResumeConfirmRequired() { return resumeConfirmRequired; }
    public void setResumeConfirmRequired(Integer resumeConfirmRequired) { this.resumeConfirmRequired = resumeConfirmRequired; }
    public Integer getPublishConfirmRequired() { return publishConfirmRequired; }
    public void setPublishConfirmRequired(Integer publishConfirmRequired) { this.publishConfirmRequired = publishConfirmRequired; }
    public Long getCreatedBy() { return createdBy; }
    public void setCreatedBy(Long createdBy) { this.createdBy = createdBy; }
    public LocalDateTime getStartedAt() { return startedAt; }
    public void setStartedAt(LocalDateTime startedAt) { this.startedAt = startedAt; }
    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }
}
