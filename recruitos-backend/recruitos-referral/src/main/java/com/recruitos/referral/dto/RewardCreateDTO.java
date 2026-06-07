package com.recruitos.referral.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.io.Serializable;

/**
 * DTO for creating a referral reward
 */
public class RewardCreateDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotNull(message = "Referral ID is required")
    private Long referralId;

    @NotNull(message = "Referrer ID is required")
    private Long referrerId;

    @NotBlank(message = "Referrer name is required")
    private String referrerName;

    @NotBlank(message = "Reward type is required")
    private String rewardType;

    @NotNull(message = "Reward amount is required")
    private Double rewardAmount;

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

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }
}
