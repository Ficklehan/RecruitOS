package com.recruitos.interview.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.recruitos.common.mybatis.BaseEntity;

import java.time.LocalDateTime;

/**
 * Interview entity
 */
@TableName("interview")
public class Interview extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /** Candidate ID */
    private Long candidateId;

    /** Job ID */
    private Long jobId;

    /** Demand ID */
    private Long demandId;

    /** Round: INITIAL / FINAL */
    private String round;

    /** Round sequence number */
    private Integer roundSeq;

    /** Interviewer user ID */
    private Long interviewerId;

    /** Scheduled start time */
    private LocalDateTime scheduledStartTime;

    /** Scheduled end time */
    private LocalDateTime scheduledEndTime;

    /** Actual start time */
    private LocalDateTime actualStartTime;

    /** Actual end time */
    private LocalDateTime actualEndTime;

    /** Format: ONSITE / VIDEO / PHONE */
    private String format;

    /** Meeting link */
    private String meetingLink;

    /** Meeting platform */
    private String meetingPlatform;

    /** Location */
    private String location;

    /** Duration in minutes */
    private Integer durationMinutes;

    /** Status: PENDING_ARRANGE / ARRANGED / IN_PROGRESS / COMPLETED / CANCELLED / NO_SHOW */
    private String status;

    /** Recording URL */
    private String recordingUrl;

    /** AI-generated summary */
    private String aiSummary;

    /** Whether candidate agreed to recording */
    private Boolean candidateAgreedRecord;

    /** Cancel reason */
    private String cancelReason;

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

    public Integer getRoundSeq() {
        return roundSeq;
    }

    public void setRoundSeq(Integer roundSeq) {
        this.roundSeq = roundSeq;
    }

    public Long getInterviewerId() {
        return interviewerId;
    }

    public void setInterviewerId(Long interviewerId) {
        this.interviewerId = interviewerId;
    }

    public LocalDateTime getScheduledStartTime() {
        return scheduledStartTime;
    }

    public void setScheduledStartTime(LocalDateTime scheduledStartTime) {
        this.scheduledStartTime = scheduledStartTime;
    }

    public LocalDateTime getScheduledEndTime() {
        return scheduledEndTime;
    }

    public void setScheduledEndTime(LocalDateTime scheduledEndTime) {
        this.scheduledEndTime = scheduledEndTime;
    }

    public LocalDateTime getActualStartTime() {
        return actualStartTime;
    }

    public void setActualStartTime(LocalDateTime actualStartTime) {
        this.actualStartTime = actualStartTime;
    }

    public LocalDateTime getActualEndTime() {
        return actualEndTime;
    }

    public void setActualEndTime(LocalDateTime actualEndTime) {
        this.actualEndTime = actualEndTime;
    }

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    public String getMeetingLink() {
        return meetingLink;
    }

    public void setMeetingLink(String meetingLink) {
        this.meetingLink = meetingLink;
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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getRecordingUrl() {
        return recordingUrl;
    }

    public void setRecordingUrl(String recordingUrl) {
        this.recordingUrl = recordingUrl;
    }

    public String getAiSummary() {
        return aiSummary;
    }

    public void setAiSummary(String aiSummary) {
        this.aiSummary = aiSummary;
    }

    public Boolean getCandidateAgreedRecord() {
        return candidateAgreedRecord;
    }

    public void setCandidateAgreedRecord(Boolean candidateAgreedRecord) {
        this.candidateAgreedRecord = candidateAgreedRecord;
    }

    public String getCancelReason() {
        return cancelReason;
    }

    public void setCancelReason(String cancelReason) {
        this.cancelReason = cancelReason;
    }
}
