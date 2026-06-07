package com.recruitos.interview.dto;

import java.io.Serializable;
import java.util.List;

/**
 * DTO for arranging an interview
 */
public class InterviewArrangeDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** Candidate ID */
    private Long candidateId;

    /** Job ID */
    private Long jobId;

    /** Demand ID */
    private Long demandId;

    /** Round: INITIAL / FINAL */
    private String round;

    /** Interviewer user ID */
    private Long interviewerId;

    /** Format: ONSITE / VIDEO / PHONE */
    private String format;

    /** Meeting platform */
    private String meetingPlatform;

    /** Location */
    private String location;

    /** Duration in minutes */
    private Integer durationMinutes;

    /** Candidate time slots */
    private List<InterviewSlotDTO> slots;

    // Getters and Setters

    public Long getCandidateId() {
        return candidateId;
    }

    public void setCandidateId(Long candidateId) {
        this.candidateId = candidateId;
    }

    public Long getJobId() {
        return jobId;
    }

    public void setJobId(Long jobId) {
        this.jobId = jobId;
    }

    public Long getDemandId() {
        return demandId;
    }

    public void setDemandId(Long demandId) {
        this.demandId = demandId;
    }

    public String getRound() {
        return round;
    }

    public void setRound(String round) {
        this.round = round;
    }

    public Long getInterviewerId() {
        return interviewerId;
    }

    public void setInterviewerId(Long interviewerId) {
        this.interviewerId = interviewerId;
    }

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    public String getMeetingPlatform() {
        return meetingPlatform;
    }

    public void setMeetingPlatform(String meetingPlatform) {
        this.meetingPlatform = meetingPlatform;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public Integer getDurationMinutes() {
        return durationMinutes;
    }

    public void setDurationMinutes(Integer durationMinutes) {
        this.durationMinutes = durationMinutes;
    }

    public List<InterviewSlotDTO> getSlots() {
        return slots;
    }

    public void setSlots(List<InterviewSlotDTO> slots) {
        this.slots = slots;
    }
}
