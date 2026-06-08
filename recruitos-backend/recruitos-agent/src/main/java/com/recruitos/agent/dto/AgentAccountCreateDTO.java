package com.recruitos.agent.dto;

import java.io.Serializable;

/**
 * DTO for creating or updating an agent account
 */
public class AgentAccountCreateDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 所属渠道 ID */
    private Long channelId;

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

    /** 登录方式：manual | phone */
    private String authMode;

    /** 平台登录手机号（authMode=phone） */
    private String loginPhone;

    /** 平台登录密码 */
    private String loginPassword;

    // Getters and Setters

    public Long getChannelId() {
        return channelId;
    }

    public void setChannelId(Long channelId) {
        this.channelId = channelId;
    }

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

    public String getAuthMode() {
        return authMode;
    }

    public void setAuthMode(String authMode) {
        this.authMode = authMode;
    }

    public String getLoginPhone() {
        return loginPhone;
    }

    public void setLoginPhone(String loginPhone) {
        this.loginPhone = loginPhone;
    }

    public String getLoginPassword() {
        return loginPassword;
    }

    public void setLoginPassword(String loginPassword) {
        this.loginPassword = loginPassword;
    }
}
