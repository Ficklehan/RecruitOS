package com.recruitos.referral.dto;

import java.io.Serializable;

/**
 * DTO for querying referrals
 */
public class ReferralQueryDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** Filter by referrer ID */
    private Long referrerId;

    /** Filter by candidate name (fuzzy match) */
    private String candidateName;

    /** Filter by job title (fuzzy match) */
    private String jobTitle;

    /** Filter by status */
    private String status;

    /** Page number (default 1) */
    private Integer pageNum = 1;

    /** Page size (default 10) */
    private Integer pageSize = 10;

    // Getters and Setters

    public Long getReferrerId() {
        return referrerId;
    }

    public void setReferrerId(Long referrerId) {
        this.referrerId = referrerId;
    }

    public String getCandidateName() {
        return candidateName;
    }

    public void setCandidateName(String candidateName) {
        this.candidateName = candidateName;
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

    public Integer getPageNum() {
        return pageNum;
    }

    public void setPageNum(Integer pageNum) {
        this.pageNum = pageNum;
    }

    public Integer getPageSize() {
        return pageSize;
    }

    public void setPageSize(Integer pageSize) {
        this.pageSize = pageSize;
    }
}
