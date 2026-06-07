package com.recruitos.referral.dto;

import java.io.Serializable;

/**
 * DTO for querying referral rewards
 */
public class RewardQueryDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** Filter by referral ID */
    private Long referralId;

    /** Filter by referrer ID */
    private Long referrerId;

    /** Filter by reward type */
    private String rewardType;

    /** Filter by status */
    private String status;

    /** Page number (default 1) */
    private Integer pageNum = 1;

    /** Page size (default 10) */
    private Integer pageSize = 10;

    // Getters and Setters

    public Long getReferralId() {
        return referralId;
    }

    public void setReferralId(Long referralId) {
        this.referralId = referralId;
    }

    public Long getReferrerId() {
        return referrerId;
    }

    public void setReferrerId(Long referrerId) {
        this.referrerId = referrerId;
    }

    public String getRewardType() {
        return rewardType;
    }

    public void setRewardType(String rewardType) {
        this.rewardType = rewardType;
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
