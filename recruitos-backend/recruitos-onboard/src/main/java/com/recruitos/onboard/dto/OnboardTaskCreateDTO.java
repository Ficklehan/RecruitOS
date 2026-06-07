package com.recruitos.onboard.dto;

import java.io.Serializable;
import java.time.LocalDate;

/**
 * DTO for creating an onboard task
 */
public class OnboardTaskCreateDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** Onboard record ID */
    private Long onboardId;

    /** Task name */
    private String taskName;

    /** Task type: DOC / PREPARATION / TRAINING / IT / OTHER */
    private String taskType;

    /** Assignee user ID */
    private Long assigneeId;

    /** Assignee name */
    private String assigneeName;

    /** Due date */
    private LocalDate dueDate;

    /** Remark */
    private String remark;

    // Getters and Setters

    public Long getOnboardId() {
        return onboardId;
    }

    public void setOnboardId(Long onboardId) {
        this.onboardId = onboardId;
    }

    public String getTaskName() {
        return taskName;
    }

    public void setTaskName(String taskName) {
        this.taskName = taskName;
    }

    public String getTaskType() {
        return taskType;
    }

    public void setTaskType(String taskType) {
        this.taskType = taskType;
    }

    public Long getAssigneeId() {
        return assigneeId;
    }

    public void setAssigneeId(Long assigneeId) {
        this.assigneeId = assigneeId;
    }

    public String getAssigneeName() {
        return assigneeName;
    }

    public void setAssigneeName(String assigneeName) {
        this.assigneeName = assigneeName;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public void setDueDate(LocalDate dueDate) {
        this.dueDate = dueDate;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }
}
