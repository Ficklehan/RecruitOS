package com.recruitos.agent.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Agent behavior log entity - records individual agent actions
 */
@TableName("agent_behavior_log")
public class AgentBehaviorLog implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    /** Tenant ID */
    @TableField("tenant_id")
    private Long tenantId;

    /** Associated agent task ID */
    private Long agentTaskId;

    /** Associated agent account ID */
    private Long agentAccountId;

    /** Legacy action column (init.sql) */
    @TableField("action")
    private String action;

    /** Action type: VIEW_PROFILE / SEND_MESSAGE / FOLLOW_UP / SCHEDULE_INTERVIEW / OTHER */
  @TableField("action_type")
    private String actionType;

    /** Target person name */
    private String targetName;

    /** Target platform */
    private String targetPlatform;

    /** Action detail */
    private String actionDetail;

    /** Is success: 0=failed, 1=success */
    @TableField("is_success")
    private Integer isSuccess;

    @TableField("success")
    private Integer success;

    /** Error message if failed */
    private String errorMessage;

    /** Random delay in milliseconds */
    private Integer randomDelay;

    /** When the action was executed */
    private LocalDateTime executedAt;

    /** Created timestamp */
    @TableField("created_at")
    private LocalDateTime createdAt;

    // Getters and Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getTenantId() {
        return tenantId;
    }

    public void setTenantId(Long tenantId) {
        this.tenantId = tenantId;
    }

    public Long getAgentTaskId() {
        return agentTaskId;
    }

    public void setAgentTaskId(Long agentTaskId) {
        this.agentTaskId = agentTaskId;
    }

    public Long getAgentAccountId() {
        return agentAccountId;
    }

    public void setAgentAccountId(Long agentAccountId) {
        this.agentAccountId = agentAccountId;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getActionType() {
        return actionType;
    }

    public void setActionType(String actionType) {
        this.actionType = actionType;
    }

    public String getTargetName() {
        return targetName;
    }

    public void setTargetName(String targetName) {
        this.targetName = targetName;
    }

    public String getTargetPlatform() {
        return targetPlatform;
    }

    public void setTargetPlatform(String targetPlatform) {
        this.targetPlatform = targetPlatform;
    }

    public String getActionDetail() {
        return actionDetail;
    }

    public void setActionDetail(String actionDetail) {
        this.actionDetail = actionDetail;
    }

    public Integer getIsSuccess() {
        return isSuccess;
    }

    public void setIsSuccess(Integer isSuccess) {
        this.isSuccess = isSuccess;
    }

    public Integer getSuccess() {
        return success;
    }

    public void setSuccess(Integer success) {
        this.success = success;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public Integer getRandomDelay() {
        return randomDelay;
    }

    public void setRandomDelay(Integer randomDelay) {
        this.randomDelay = randomDelay;
    }

    public LocalDateTime getExecutedAt() {
        return executedAt;
    }

    public void setExecutedAt(LocalDateTime executedAt) {
        this.executedAt = executedAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
