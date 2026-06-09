package com.recruitos.evolution.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.recruitos.common.mybatis.BaseEntity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@TableName("evolution_signal")
public class EvolutionSignal extends BaseEntity {

    private static final long serialVersionUID = 1L;

    private Long jobId;
    private Integer signalLevel;
    private BigDecimal confidence;
    private Long candidateId;
    private String sourceModule;
    private String sourceEvent;
    private Long campaignId;
    private Long traceId;
    private String tagAdjustments;
    private BigDecimal learningRate;
    private String status;
    private String abGroup;
    private LocalDateTime appliedAt;

    public Long getJobId() {
        return jobId;
    }

    public void setJobId(Long jobId) {
        this.jobId = jobId;
    }

    public Integer getSignalLevel() {
        return signalLevel;
    }

    public void setSignalLevel(Integer signalLevel) {
        this.signalLevel = signalLevel;
    }

    public BigDecimal getConfidence() {
        return confidence;
    }

    public void setConfidence(BigDecimal confidence) {
        this.confidence = confidence;
    }

    public Long getCandidateId() {
        return candidateId;
    }

    public void setCandidateId(Long candidateId) {
        this.candidateId = candidateId;
    }

    public String getSourceModule() {
        return sourceModule;
    }

    public void setSourceModule(String sourceModule) {
        this.sourceModule = sourceModule;
    }

    public String getSourceEvent() {
        return sourceEvent;
    }

    public void setSourceEvent(String sourceEvent) {
        this.sourceEvent = sourceEvent;
    }

    public Long getCampaignId() {
        return campaignId;
    }

    public void setCampaignId(Long campaignId) {
        this.campaignId = campaignId;
    }

    public Long getTraceId() {
        return traceId;
    }

    public void setTraceId(Long traceId) {
        this.traceId = traceId;
    }

    public String getTagAdjustments() {
        return tagAdjustments;
    }

    public void setTagAdjustments(String tagAdjustments) {
        this.tagAdjustments = tagAdjustments;
    }

    public BigDecimal getLearningRate() {
        return learningRate;
    }

    public void setLearningRate(BigDecimal learningRate) {
        this.learningRate = learningRate;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getAbGroup() {
        return abGroup;
    }

    public void setAbGroup(String abGroup) {
        this.abGroup = abGroup;
    }

    public LocalDateTime getAppliedAt() {
        return appliedAt;
    }

    public void setAppliedAt(LocalDateTime appliedAt) {
        this.appliedAt = appliedAt;
    }
}
