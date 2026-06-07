package com.recruitos.demand.dto;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Value Object for demand detail response
 */
public class DemandVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String demandNo;
    private String title;
    private Long orgId;
    private String orgName;
    private Integer headCount;
    private String jobLevel;
    private BigDecimal salaryMin;
    private BigDecimal salaryMax;
    private String urgency;
    private LocalDate expectedOnboardDate;
    private String reason;
    private String jobDuty;
    private String jobRequirement;
    private String workLocations;
    private Long reporterId;
    private String initialInterviewerIds;
    private String finalInterviewerIds;
    private String status;
    private Integer approvedHeadCount;
    private Integer filledCount;
    private String rejectReason;
    private Long createdBy;
    private String creatorName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Approval info
    private Long approvalInstanceId;
    private String approvalStatus;
    private Long currentApproverId;
    private String currentNode;

    // Getters and Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

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

    public String getOrgName() {
        return orgName;
    }

    public void setOrgName(String orgName) {
        this.orgName = orgName;
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

    public String getCreatorName() {
        return creatorName;
    }

    public void setCreatorName(String creatorName) {
        this.creatorName = creatorName;
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

    public Long getApprovalInstanceId() {
        return approvalInstanceId;
    }

    public void setApprovalInstanceId(Long approvalInstanceId) {
        this.approvalInstanceId = approvalInstanceId;
    }

    public String getApprovalStatus() {
        return approvalStatus;
    }

    public void setApprovalStatus(String approvalStatus) {
        this.approvalStatus = approvalStatus;
    }

    public Long getCurrentApproverId() {
        return currentApproverId;
    }

    public void setCurrentApproverId(Long currentApproverId) {
        this.currentApproverId = currentApproverId;
    }

    public String getCurrentNode() {
        return currentNode;
    }

    public void setCurrentNode(String currentNode) {
        this.currentNode = currentNode;
    }
}
