package com.recruitos.demand.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.recruitos.common.mybatis.BaseEntity;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Recruitment demand entity
 */
@TableName("recruit_demand")
public class RecruitDemand extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /** Demand number (auto-generated: DEMAND + yyyyMMdd + 4-digit seq) */
    private String demandNo;

    /** Demand title */
    private String title;

    /** Department ID */
    private Long orgId;

    /** Head count to recruit */
    private Integer headCount;

    /** Job level (P6/P7/P8 etc.) */
    private String jobLevel;

    /** Expected salary minimum */
    private BigDecimal salaryMin;

    /** Expected salary maximum */
    private BigDecimal salaryMax;

    /** Urgency: NORMAL/URGENT/CRITICAL */
    private String urgency;

    /** Expected onboard date */
    private LocalDate expectedOnboardDate;

    /** Reason: NEW/REPLACEMENT/EXPANSION/TEMPORARY */
    private String reason;

    /** Job duty description */
    private String jobDuty;

    /** Job requirements */
    private String jobRequirement;

    /** Work locations (JSON string) */
    private String workLocations;

    /** Reporter (supervisor) user ID */
    private Long reporterId;

    /** Initial interviewer IDs (JSON string) */
    private String initialInterviewerIds;

    /** Final interviewer IDs (JSON string) */
    private String finalInterviewerIds;

    /** Status: DRAFT/PENDING/APPROVED/REJECTED/JOB_CREATED/RECRUITING/COMPLETED/CLOSED */
    private String status;

    /** Approved head count */
    private Integer approvedHeadCount;

    /** Filled count */
    private Integer filledCount;

    /** Reject reason */
    private String rejectReason;

    /** Created by user ID */
    private Long createdBy;

    // Getters and Setters

    public String getDemandNo() {
        return demandNo;
    }

    public void setDemandNo(String demandNo) {
        this.demandNo = demandNo;
    }

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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getApprovedHeadCount() {
        return approvedHeadCount;
    }

    public void setApprovedHeadCount(Integer approvedHeadCount) {
        this.approvedHeadCount = approvedHeadCount;
    }

    public Integer getFilledCount() {
        return filledCount;
    }

    public void setFilledCount(Integer filledCount) {
        this.filledCount = filledCount;
    }

    public String getRejectReason() {
        return rejectReason;
    }

    public void setRejectReason(String rejectReason) {
        this.rejectReason = rejectReason;
    }

    public Long getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(Long createdBy) {
        this.createdBy = createdBy;
    }
}
