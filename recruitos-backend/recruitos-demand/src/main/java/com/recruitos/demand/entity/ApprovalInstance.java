package com.recruitos.demand.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.recruitos.common.mybatis.BaseEntity;

/**
 * Approval instance entity
 * Note: tenant_id is not auto-filled for this entity, must set manually
 */
@TableName("approval_instance")
public class ApprovalInstance extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /** Business type: DEMAND/OFFER */
    private String bizType;

    /** Related business ID */
    private Long bizId;

    /** Flowable process instance ID (nullable for simplified approval) */
    private String processInstId;

    /** Status: RUNNING/APPROVED/REJECTED/CANCELLED */
    private String status;

    /** Current approval node name */
    private String currentNode;

    /** Current approver user ID */
    private Long currentApproverId;

    // Getters and Setters

    public String getBizType() {
        return bizType;
    }

    public void setBizType(String bizType) {
        this.bizType = bizType;
    }

    public Long getBizId() {
        return bizId;
    }

    public void setBizId(Long bizId) {
        this.bizId = bizId;
    }

    public String getProcessInstId() {
        return processInstId;
    }

    public void setProcessInstId(String processInstId) {
        this.processInstId = processInstId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getCurrentNode() {
        return currentNode;
    }

    public void setCurrentNode(String currentNode) {
        this.currentNode = currentNode;
    }

    public Long getCurrentApproverId() {
        return currentApproverId;
    }

    public void setCurrentApproverId(Long currentApproverId) {
        this.currentApproverId = currentApproverId;
    }
}
