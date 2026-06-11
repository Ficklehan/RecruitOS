package com.recruitos.referral.dto;

import javax.validation.constraints.NotNull;
import java.io.Serializable;

public class ReferralLinkCreateDTO implements Serializable {

    @NotNull
    private Long jobId;
    private Long referrerId;
    private String referrerName;

    public Long getJobId() { return jobId; }
    public void setJobId(Long jobId) { this.jobId = jobId; }
    public Long getReferrerId() { return referrerId; }
    public void setReferrerId(Long referrerId) { this.referrerId = referrerId; }
    public String getReferrerName() { return referrerName; }
    public void setReferrerName(String referrerName) { this.referrerName = referrerName; }
}
