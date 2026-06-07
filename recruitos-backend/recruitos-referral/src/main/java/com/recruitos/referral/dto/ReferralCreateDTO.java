package com.recruitos.referral.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.io.Serializable;

/**
 * DTO for creating a referral
 */
public class ReferralCreateDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotNull(message = "Referrer ID is required")
    private Long referrerId;

    @NotBlank(message = "Referrer name is required")
    private String referrerName;

    @NotNull(message = "Candidate ID is required")
    private Long candidateId;

    @NotBlank(message = "Candidate name is required")
    private String candidateName;

    @NotNull(message = "Job ID is required")
    private Long jobId;

    @NotBlank(message = "Job title is required")
    private String jobTitle;

    private String remark;

    // Getters and Setters

    public Long getReferrerId() {
        return referrerId;
    }

    public void setReferrerId(Long referrerId) {
        this.referrerId = referrerId;
    }

    public String getReferrerName() {
        return referrerName;
    }

    public void setReferrerName(String referrerName) {
        this.referrerName = referrerName;
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

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }
}
