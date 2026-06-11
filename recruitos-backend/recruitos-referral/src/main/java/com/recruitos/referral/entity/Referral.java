package com.recruitos.referral.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.recruitos.common.mybatis.BaseEntity;

/**
 * Referral entity - internal employee referral records
 */
@TableName("referral")
public class Referral extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /** Referrer user ID (who referred the candidate) */
    private Long referrerId;

    /** Referrer name */
    private String referrerName;

    /** Candidate ID */
    private Long candidateId;

    /** Candidate name */
    private String candidateName;

    /** Job ID */
    private Long jobId;

    /** Job title */
    private String jobTitle;

    /** Status: SUBMITTED/SCREENING/INTERVIEWING/HIRED/REJECTED */
    private String status;

    private java.math.BigDecimal rewardAmount;
    private String rewardStatus;
    private java.time.LocalDateTime rewardPaidAt;

    /** Remark */
    private String remark;

    /** Created by user ID */
    private Long createdBy;

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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public java.math.BigDecimal getRewardAmount() {
        return rewardAmount;
    }

    public void setRewardAmount(java.math.BigDecimal rewardAmount) {
        this.rewardAmount = rewardAmount;
    }

    public String getRewardStatus() {
        return rewardStatus;
    }

    public void setRewardStatus(String rewardStatus) {
        this.rewardStatus = rewardStatus;
    }

    public java.time.LocalDateTime getRewardPaidAt() {
        return rewardPaidAt;
    }

    public void setRewardPaidAt(java.time.LocalDateTime rewardPaidAt) {
        this.rewardPaidAt = rewardPaidAt;
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
}
