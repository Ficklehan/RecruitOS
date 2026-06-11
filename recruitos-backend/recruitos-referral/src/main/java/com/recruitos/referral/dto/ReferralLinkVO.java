package com.recruitos.referral.dto;

import java.io.Serializable;

public class ReferralLinkVO implements Serializable {

    private String token;
    private String url;
    private Long jobId;
    private String jobTitle;
    private Long referrerId;
    private String referrerName;

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }
    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }
    public Long getJobId() { return jobId; }
    public void setJobId(Long jobId) { this.jobId = jobId; }
    public String getJobTitle() { return jobTitle; }
    public void setJobTitle(String jobTitle) { this.jobTitle = jobTitle; }
    public Long getReferrerId() { return referrerId; }
    public void setReferrerId(Long referrerId) { this.referrerId = referrerId; }
    public String getReferrerName() { return referrerName; }
    public void setReferrerName(String referrerName) { this.referrerName = referrerName; }
}
