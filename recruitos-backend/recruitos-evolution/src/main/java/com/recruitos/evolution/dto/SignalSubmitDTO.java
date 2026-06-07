package com.recruitos.evolution.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.io.Serializable;

/**
 * DTO for submitting an evolution signal
 */
public class SignalSubmitDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotBlank(message = "Signal type is required")
    private String signalType;

    @NotNull(message = "Job ID is required")
    private Long jobId;

    private String jobTitle;

    private Long tagId;

    private String tagName;

    @NotNull(message = "Signal value is required")
    private Double signalValue;

    private String source;

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
}
