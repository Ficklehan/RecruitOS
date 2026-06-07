package com.recruitos.demand.dto;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * DTO for creating a recruitment demand
 */
public class DemandCreateDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotBlank(message = "Demand title is required")
    private String title;

    @NotNull(message = "Department ID is required")
    private Long orgId;

    @NotNull(message = "Head count is required")
    @Min(value = 1, message = "Head count must be at least 1")
    private Integer headCount;

    @NotBlank(message = "Job level is required")
    private String jobLevel;

    private BigDecimal salaryMin;

    private BigDecimal salaryMax;

    private String urgency;

    private LocalDate expectedOnboardDate;

    @NotBlank(message = "Reason is required")
    private String reason;

    private String jobDuty;

    private String jobRequirement;

    private String workLocations;

    private Long reporterId;

    private String initialInterviewerIds;

    private String finalInterviewerIds;

    // Getters and Setters

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Long getOrgId() {
        return orgId;
    }

    public void setOrgId(Long orgId) {
        this.orgId = orgId;
    }

    public Integer getHeadCount() {
        return headCount;
    }

    public void setHeadCount(Integer headCount) {
        this.headCount = headCount;
    }

    public String getJobLevel() {
        return jobLevel;
    }

    public void setJobLevel(String jobLevel) {
        this.jobLevel = jobLevel;
    }

    public BigDecimal getSalaryMin() {
        return salaryMin;
    }

    public void setSalaryMin(BigDecimal salaryMin) {
        this.salaryMin = salaryMin;
    }

    public BigDecimal getSalaryMax() {
        return salaryMax;
    }

    public void setSalaryMax(BigDecimal salaryMax) {
        this.salaryMax = salaryMax;
    }

    public String getUrgency() {
        return urgency;
    }

    public void setUrgency(String urgency) {
        this.urgency = urgency;
    }

    public LocalDate getExpectedOnboardDate() {
        return expectedOnboardDate;
    }

    public void setExpectedOnboardDate(LocalDate expectedOnboardDate) {
        this.expectedOnboardDate = expectedOnboardDate;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getJobDuty() {
        return jobDuty;
    }

    public void setJobDuty(String jobDuty) {
        this.jobDuty = jobDuty;
    }

    public String getJobRequirement() {
        return jobRequirement;
    }

    public void setJobRequirement(String jobRequirement) {
        this.jobRequirement = jobRequirement;
    }

    public String getWorkLocations() {
        return workLocations;
    }

    public void setWorkLocations(String workLocations) {
        this.workLocations = workLocations;
    }

    public Long getReporterId() {
        return reporterId;
    }

    public void setReporterId(Long reporterId) {
        this.reporterId = reporterId;
    }

    public String getInitialInterviewerIds() {
        return initialInterviewerIds;
    }

    public void setInitialInterviewerIds(String initialInterviewerIds) {
        this.initialInterviewerIds = initialInterviewerIds;
    }

    public String getFinalInterviewerIds() {
        return finalInterviewerIds;
    }

    public void setFinalInterviewerIds(String finalInterviewerIds) {
        this.finalInterviewerIds = finalInterviewerIds;
    }
}
