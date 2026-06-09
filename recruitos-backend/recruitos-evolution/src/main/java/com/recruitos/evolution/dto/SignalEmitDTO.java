package com.recruitos.evolution.dto;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.Map;

public class SignalEmitDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotNull(message = "Job ID is required")
    private Long jobId;

    @NotNull(message = "Signal level is required")
    @Min(1)
    @Max(6)
    private Integer signalLevel;

    private Double confidence;
    private Long candidateId;
    private Map<String, Object> tagAdjustments;
    private String sourceModule;
    private String sourceEvent;
    private Long campaignId;
    private Long traceId;
    private String abGroup;

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

    public Double getConfidence() {
        return confidence;
    }

    public void setConfidence(Double confidence) {
        this.confidence = confidence;
    }

    public Long getCandidateId() {
        return candidateId;
    }

    public void setCandidateId(Long candidateId) {
        this.candidateId = candidateId;
    }

    public Map<String, Object> getTagAdjustments() {
        return tagAdjustments;
    }

    public void setTagAdjustments(Map<String, Object> tagAdjustments) {
        this.tagAdjustments = tagAdjustments;
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

    public String getAbGroup() {
        return abGroup;
    }

    public void setAbGroup(String abGroup) {
        this.abGroup = abGroup;
    }
}
