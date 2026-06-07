package com.recruitos.evolution.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.recruitos.common.mybatis.BaseEntity;

/**
 * Evolution signal entity - tracks match/search/decision signals for jobs
 */
@TableName("evolution_signal")
public class EvolutionSignal extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /** Signal type: MATCH/SEARCH/DECISION */
    private String signalType;

    /** Job ID */
    private Long jobId;

    /** Job title */
    private String jobTitle;

    /** Tag ID */
    private Long tagId;

    /** Tag name */
    private String tagName;

    /** Raw signal value */
    private Double signalValue;

    /** Where signal came from */
    private String source;

    /** Processing status: 0=unprocessed, 1=processed */
    private Integer processed;

    /** Created by user ID */
    private Long createdBy;

    // Getters and Setters

    public String getSignalType() {
        return signalType;
    }

    public void setSignalType(String signalType) {
        this.signalType = signalType;
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

    public Double getSignalValue() {
        return signalValue;
    }

    public void setSignalValue(Double signalValue) {
        this.signalValue = signalValue;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public Integer getProcessed() {
        return processed;
    }

    public void setProcessed(Integer processed) {
        this.processed = processed;
    }

    public Long getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(Long createdBy) {
        this.createdBy = createdBy;
    }
}
