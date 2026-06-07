package com.recruitos.referral.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.recruitos.common.mybatis.BaseEntity;

import java.time.LocalDateTime;

/**
 * Referral reward entity - tracks rewards for successful referrals
 */
@TableName("referral_reward")
public class ReferralReward extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /** Associated referral ID */
    private Long referralId;

    /** Referrer user ID */
    private Long referrerId;

    /** Referrer name */
    private String referrerName;

    /** Reward type: CASH/GIFT/OTHER */
    private String rewardType;

    /** Reward amount */
    private Double rewardAmount;

    /** Status: PENDING/APPROVED/PAID/CANCELLED */
    private String status;

    /** Approved by user ID */
    private Long approvedBy;

    /** Approved time */
    private LocalDateTime approvedAt;

    /** Paid time */
    private LocalDateTime paidAt;

    /** Remark */
    private String remark;

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

    public String getReferrerName() {
        return referrerName;
    }

    public void setReferrerName(String referrerName) {
        this.referrerName = referrerName;
    }

    public String getRewardType() {
        return rewardType;
    }

    public void setRewardType(String rewardType) {
        this.rewardType = rewardType;
    }

    public Double getRewardAmount() {
        return rewardAmount;
    }

    public void setRewardAmount(Double rewardAmount) {
        this.rewardAmount = rewardAmount;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Long getApprovedBy() {
        return approvedBy;
    }

    public void setApprovedBy(Long approvedBy) {
        this.approvedBy = approvedBy;
    }

    public LocalDateTime getApprovedAt() {
        return approvedAt;
    }

    public void setApprovedAt(LocalDateTime approvedAt) {
        this.approvedAt = approvedAt;
    }

    public LocalDateTime getPaidAt() {
        return paidAt;
    }

    public void setPaidAt(LocalDateTime paidAt) {
        this.paidAt = paidAt;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }
}
