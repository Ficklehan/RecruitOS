package com.recruitos.offer.dto;

import java.io.Serializable;

/**
 * DTO for offer approval action
 */
public class OfferApprovalDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** Approver user ID */
    private Long approverId;

    /** Approval result: APPROVED / REJECTED */
    private String result;

    /** Remark */
    private String remark;

    // Getters and Setters

    public Long getApproverId() {
        return approverId;
    }

    public void setApproverId(Long approverId) {
        this.approverId = approverId;
    }

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }
}
