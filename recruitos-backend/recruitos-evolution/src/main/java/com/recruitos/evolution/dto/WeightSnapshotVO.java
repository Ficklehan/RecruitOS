package com.recruitos.evolution.dto;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Value Object for weight snapshot response
 */
public class WeightSnapshotVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private Long tenantId;
    private Long jobId;
    private String jobTitle;
    private Long tagId;
    private String tagName;
    private Double matchWeight;
    private Double searchWeight;
    private Double decisionWeight;
    private Integer version;
    private Long createdBy;
    private LocalDateTime createdAt;

    // Getters and Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getTenantId() {
        return tenantId;
    }

    public void setTenantId(Long tenantId) {
        this.tenantId = tenantId;
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

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
