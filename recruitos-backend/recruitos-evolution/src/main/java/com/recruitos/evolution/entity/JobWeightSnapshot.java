package com.recruitos.evolution.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.recruitos.common.mybatis.BaseEntity;

/**
 * Job weight snapshot entity - stores weight snapshots for job-tag pairs
 */
@TableName("job_weight_snapshot")
public class JobWeightSnapshot extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /** Job ID */
    private Long jobId;

    /** Job title */
    private String jobTitle;

    /** Tag ID */
    private Long tagId;

    /** Tag name */
    private String tagName;

    /** Match weight */
    private Double matchWeight;

    /** Search weight */
    private Double searchWeight;

    /** Decision weight */
    private Double decisionWeight;

    /** Version number */
    private Integer version;

    /** Created by user ID */
    private Long createdBy;

    // Getters and Setters

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

    public Long getTagId() {
        return tagId;
    }

    public void setTagId(Long tagId) {
        this.tagId = tagId;
    }

    public String getTagName() {
        return tagName;
    }

    public void setTagName(String tagName) {
        this.tagName = tagName;
    }

    public Double getMatchWeight() {
        return matchWeight;
    }

    public void setMatchWeight(Double matchWeight) {
        this.matchWeight = matchWeight;
    }

    public Double getSearchWeight() {
        return searchWeight;
    }

    public void setSearchWeight(Double searchWeight) {
        this.searchWeight = searchWeight;
    }

    public Double getDecisionWeight() {
        return decisionWeight;
    }

    public void setDecisionWeight(Double decisionWeight) {
        this.decisionWeight = decisionWeight;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public Long getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(Long createdBy) {
        this.createdBy = createdBy;
    }
}
