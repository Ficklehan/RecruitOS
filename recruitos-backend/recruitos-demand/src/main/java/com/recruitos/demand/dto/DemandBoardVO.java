package com.recruitos.demand.dto;

import java.io.Serializable;

/**
 * Value Object for demand dashboard / board
 */
public class DemandBoardVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** Total demands */
    private long totalDemands;

    /** Pending approval count */
    private long pendingApproval;

    /** Approved count */
    private long approved;

    /** Recruiting count */
    private long recruiting;

    /** Completed count */
    private long completed;

    /** Closed count */
    private long closed;

    /** Total headcount across all demands */
    private int headcountTotal;

    /** Total filled headcount */
    private int headcountFilled;

    /** Overdue demands count */
    private long overdueCount;

    // Getters and Setters

    public long getTotalDemands() {
        return totalDemands;
    }

    public void setTotalDemands(long totalDemands) {
        this.totalDemands = totalDemands;
    }

    public long getPendingApproval() {
        return pendingApproval;
    }

    public void setPendingApproval(long pendingApproval) {
        this.pendingApproval = pendingApproval;
    }

    public long getApproved() {
        return approved;
    }

    public void setApproved(long approved) {
        this.approved = approved;
    }

    public long getRecruiting() {
        return recruiting;
    }

    public void setRecruiting(long recruiting) {
        this.recruiting = recruiting;
    }

    public long getCompleted() {
        return completed;
    }

    public void setCompleted(long completed) {
        this.completed = completed;
    }

    public long getClosed() {
        return closed;
    }

    public void setClosed(long closed) {
        this.closed = closed;
    }

    public int getHeadcountTotal() {
        return headcountTotal;
    }

    public void setHeadcountTotal(int headcountTotal) {
        this.headcountTotal = headcountTotal;
    }

    public int getHeadcountFilled() {
        return headcountFilled;
    }

    public void setHeadcountFilled(int headcountFilled) {
        this.headcountFilled = headcountFilled;
    }

    public long getOverdueCount() {
        return overdueCount;
    }

    public void setOverdueCount(long overdueCount) {
        this.overdueCount = overdueCount;
    }
}
