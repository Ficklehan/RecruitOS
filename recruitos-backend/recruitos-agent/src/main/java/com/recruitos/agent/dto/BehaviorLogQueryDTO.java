package com.recruitos.agent.dto;

import java.io.Serializable;

/**
 * DTO for querying behavior logs
 */
public class BehaviorLogQueryDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** Filter by agent task ID */
    private Long agentTaskId;

    /** Filter by agent account ID */
    private Long agentAccountId;

    /** Filter by action type */
    private String actionType;

    /** Filter by success status: 0=failed, 1=success */
    private Integer isSuccess;

    /** Page number (default 1) */
    private Integer pageNum = 1;

    /** Page size (default 10) */
    private Integer pageSize = 10;

    // Getters and Setters

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

    public Integer getIsSuccess() {
        return isSuccess;
    }

    public void setIsSuccess(Integer isSuccess) {
        this.isSuccess = isSuccess;
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
