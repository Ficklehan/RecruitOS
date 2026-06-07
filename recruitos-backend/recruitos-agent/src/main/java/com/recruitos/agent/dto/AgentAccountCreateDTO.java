package com.recruitos.agent.dto;

import java.io.Serializable;

/**
 * DTO for creating or updating an agent account
 */
public class AgentAccountCreateDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** Platform: BOSS / LAGOU / ZHILIAN / LIEPIN / OTHER */
    private String platform;

    /** Account display name */
    private String accountName;

    /** Platform account ID */
    private String accountId;

    /** Status: ACTIVE / PAUSED / DISABLED / RATE_LIMITED */
    private String status;

    /** Daily action limit */
    private Integer dailyLimit;

    /** Remark */
    private String remark;

    // Getters and Setters

    public String getPlatform() {
        return platform;
    }

    public void setPlatform(String platform) {
        this.platform = platform;
    }

    public String getAccountName() {
        return accountName;
    }

    public void setAccountName(String accountName) {
        this.accountName = accountName;
    }

    public String getAccountId() {
        return accountId;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getDailyLimit() {
        return dailyLimit;
    }

    public void setDailyLimit(Integer dailyLimit) {
        this.dailyLimit = dailyLimit;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }
}
