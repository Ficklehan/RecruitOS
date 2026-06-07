package com.recruitos.onboard.dto;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Onboard detail VO - includes onboard info + tasks
 */
public class OnboardVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private Long tenantId;
    private Long candidateId;
    private String candidateName;
    private Long jobId;
    private String jobTitle;
    private Long offerId;
    private LocalDate onboardDate;
    private String onboardStatus;
    private Long hrId;
    private String hrName;
    private String remark;
    private Long createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /** Onboard tasks */
    private List<OnboardTaskVO> tasks;

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

    public Long getCandidateId() {
        return candidateId;
    }

    public void setCandidateId(Long candidateId) {
        this.candidateId = candidateId;
    }

    public String getCandidateName() {
        return candidateName;
    }

    public void setCandidateName(String candidateName) {
        this.candidateName = candidateName;
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

    public Long getOfferId() {
        return offerId;
    }

    public void setOfferId(Long offerId) {
        this.offerId = offerId;
    }

    public LocalDate getOnboardDate() {
        return onboardDate;
    }

    public void setOnboardDate(LocalDate onboardDate) {
        this.onboardDate = onboardDate;
    }

    public String getOnboardStatus() {
        return onboardStatus;
    }

    public void setOnboardStatus(String onboardStatus) {
        this.onboardStatus = onboardStatus;
    }

    public Long getHrId() {
        return hrId;
    }

    public void setHrId(Long hrId) {
        this.hrId = hrId;
    }

    public String getHrName() {
        return hrName;
    }

    public void setHrName(String hrName) {
        this.hrName = hrName;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
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

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public List<OnboardTaskVO> getTasks() {
        return tasks;
    }

    public void setTasks(List<OnboardTaskVO> tasks) {
        this.tasks = tasks;
    }
}
