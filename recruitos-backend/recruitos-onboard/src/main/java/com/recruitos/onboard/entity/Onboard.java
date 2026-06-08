package com.recruitos.onboard.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.recruitos.common.mybatis.BaseEntity;

import java.time.LocalDate;

/**
 * Onboard entity
 */
@TableName("onboard")
public class Onboard extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /** Candidate ID */
    private Long candidateId;

    /** Candidate name */
    private String candidateName;

    /** Job ID */
    private Long jobId;

    /** Job title */
    private String jobTitle;

    /** Offer ID */
    private Long offerId;

    /** Onboard date */
    private LocalDate onboardDate;

    /** Onboard status: PENDING / CONFIRMED / CANCELLED / COMPLETED */
    @TableField("status")
    private String onboardStatus;

    /** HR user ID */
    private Long hrId;

    /** HR name */
    private String hrName;

    /** Remark */
    private String remark;

    /** Created by user ID */
    private Long createdBy;

    // Getters and Setters

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
}
