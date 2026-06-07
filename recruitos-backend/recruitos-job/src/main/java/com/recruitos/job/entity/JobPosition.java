package com.recruitos.job.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.recruitos.common.mybatis.BaseEntity;

/**
 * Job position entity mapped to job_position table
 */
@TableName("job_position")
public class JobPosition extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /** Associated recruitment demand ID */
    private Long demandId;

    /** Job number (auto-generated: JOB + yyyyMMdd + 4-digit seq) */
    private String jobNo;

    /** Job title */
    private String title;

    /** Job description text */
    private String jdText;

    /** Parsed JD result as JSON string */
    private String jdParsedJson;

    /** Tags as JSON string */
    private String tags;

    /** Embedding vector ID for semantic search */
    private String embeddingVectorId;

    /** Total head count for this position */
    private Integer headCount;

    /** Number of positions already filled */
    private Integer filledCount;

    /** Status: DRAFT / ACTIVE / PAUSED / CLOSED */
    private String status;

    /** Reason for closing the position */
    private String closedReason;

    // Getters and Setters

    public Long getDemandId() {
        return demandId;
    }

    public void setDemandId(Long demandId) {
        this.demandId = demandId;
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
}
