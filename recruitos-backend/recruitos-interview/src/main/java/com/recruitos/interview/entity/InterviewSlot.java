package com.recruitos.interview.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.recruitos.common.mybatis.BaseEntity;

import java.time.LocalDateTime;

/**
 * Interview time slot entity
 */
@TableName("interview_slot")
public class InterviewSlot extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /** Interview ID */
    private Long interviewId;

    /** Slot start time */
    private LocalDateTime slotStart;

    /** Slot end time */
    private LocalDateTime slotEnd;

    /** Whether this slot is selected */
    private Boolean isSelected;

    // Getters and Setters

    public Long getInterviewId() {
        return interviewId;
    }

    public void setInterviewId(Long interviewId) {
        this.interviewId = interviewId;
    }

    public LocalDateTime getSlotStart() {
        return slotStart;
    }

    public void setSlotStart(LocalDateTime slotStart) {
        this.slotStart = slotStart;
    }

    public LocalDateTime getSlotEnd() {
        return slotEnd;
    }

    public void setSlotEnd(LocalDateTime slotEnd) {
        this.slotEnd = slotEnd;
    }

    public Boolean getIsSelected() {
        return isSelected;
    }

    public void setIsSelected(Boolean isSelected) {
        this.isSelected = isSelected;
    }
}
