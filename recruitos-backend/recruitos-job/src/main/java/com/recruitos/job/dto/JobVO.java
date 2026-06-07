package com.recruitos.job.dto;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Value Object for job position detail response
 */
public class JobVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private Long tenantId;
    private Long demandId;
    private String demandTitle;
    private String demandNo;
    private String jobNo;
    private String title;
    private String jdText;
    private String jdParsedJson;
    private String tags;
    private String embeddingVectorId;
    private Integer headCount;
    private Integer filledCount;
    private String status;
    private String closedReason;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

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

    public Long getDemandId() {
        return demandId;
    }

    public void setDemandId(Long demandId) {
        this.demandId = demandId;
    }

    public String getDemandTitle() {
        return demandTitle;
    }

    public void setDemandTitle(String demandTitle) {
        this.demandTitle = demandTitle;
    }

    public String getDemandNo() {
        return demandNo;
    }

    public void setDemandNo(String demandNo) {
        this.demandNo = demandNo;
    }

    public String getJobNo() {
        return jobNo;
    }

    public void setJobNo(String jobNo) {
        this.jobNo = jobNo;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getJdText() {
        return jdText;
    }

    public void setJdText(String jdText) {
        this.jdText = jdText;
    }

    public String getJdParsedJson() {
        return jdParsedJson;
    }

    public void setJdParsedJson(String jdParsedJson) {
        this.jdParsedJson = jdParsedJson;
    }

    public String getTags() {
        return tags;
    }

    public void setTags(String tags) {
        this.tags = tags;
    }

    public String getEmbeddingVectorId() {
        return embeddingVectorId;
    }

    public void setEmbeddingVectorId(String embeddingVectorId) {
        this.embeddingVectorId = embeddingVectorId;
    }

    public Integer getHeadCount() {
        return headCount;
    }

    public void setHeadCount(Integer headCount) {
        this.headCount = headCount;
    }

    public Integer getFilledCount() {
        return filledCount;
    }

    public void setFilledCount(Integer filledCount) {
        this.filledCount = filledCount;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getClosedReason() {
        return closedReason;
    }

    public void setClosedReason(String closedReason) {
        this.closedReason = closedReason;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
