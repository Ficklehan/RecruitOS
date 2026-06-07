package com.recruitos.agent.dto;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * VO for behavior log detail
 */
public class BehaviorLogVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private Long tenantId;
    private Long agentTaskId;
    private Long agentAccountId;
    private String actionType;
    private String targetName;
    private String targetPlatform;
    private String actionDetail;
    private Integer isSuccess;
    private String errorMessage;
    private Integer randomDelay;
    private LocalDateTime executedAt;
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
