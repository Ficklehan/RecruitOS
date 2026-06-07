package com.recruitos.interview.dto;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * DTO for interview time slot
 */
public class InterviewSlotDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** Slot start time */
    private LocalDateTime slotStart;

    /** Slot end time */
    private LocalDateTime slotEnd;

    // Getters and Setters

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
}
