package com.recruitos.agent.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.recruitos.common.mybatis.BaseEntity;

import java.time.LocalDateTime;

/**
 * Agent account entity - represents a platform account used by the agent
 */
@TableName("agent_account")
public class AgentAccount extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /** Platform: BOSS / LAGOU / ZHILIAN / LIEPIN / OTHER */
    private String platform;

    /** Account display name */
    private String accountName;

    /** Platform account ID */
    private String accountId;

    /** Status: ACTIVE / PAUSED / DISABLED / RATE_LIMITED */
    private String status;

    /** Health score 0-100 */
    private Double healthScore;

    /** Daily action limit */
    private Integer dailyLimit;

    /** Actions used today */
    private Integer usedToday;

    /** Last active timestamp */
    private LocalDateTime lastActiveAt;

    /** Remark */
    private String remark;

    /** Created by user ID */
    private Long createdBy;

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

    public Double getHealthScore() {
        return healthScore;
    }

    public void setHealthScore(Double healthScore) {
        this.healthScore = healthScore;
    }

    public Integer getDailyLimit() {
        return dailyLimit;
    }

    public void setDailyLimit(Integer dailyLimit) {
        this.dailyLimit = dailyLimit;
    }

    public Integer getUsedToday() {
        return usedToday;
    }

    public void setUsedToday(Integer usedToday) {
        this.usedToday = usedToday;
    }

    public LocalDateTime getLastActiveAt() {
        return lastActiveAt;
    }

    public void setLastActiveAt(LocalDateTime lastActiveAt) {
        this.lastActiveAt = lastActiveAt;
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
