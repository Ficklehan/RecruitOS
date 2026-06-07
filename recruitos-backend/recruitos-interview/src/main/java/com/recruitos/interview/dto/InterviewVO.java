package com.recruitos.interview.dto;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Interview detail VO - includes interview info + candidate/job/interviewer names + evaluation
 */
public class InterviewVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private Long candidateId;
    private String candidateName;
    private Long jobId;
    private String jobTitle;
    private Long demandId;
    private String round;
    private Integer roundSeq;
    private Long interviewerId;
    private String interviewerName;
    private LocalDateTime scheduledStartTime;
    private LocalDateTime scheduledEndTime;
    private LocalDateTime actualStartTime;
    private LocalDateTime actualEndTime;
    private String format;
    private String meetingLink;
    private String meetingPlatform;
    private String location;
    private Integer durationMinutes;
    private String status;
    private String recordingUrl;
    private String aiSummary;
    private Boolean candidateAgreedRecord;
    private String cancelReason;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /** Candidate time slots */
    private List<InterviewSlotDTO> slots;

    /** Evaluation (if submitted) */
    private EvaluationSubmitDTO evaluation;

    // Getters and Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getCandidateId() {
        return candidateId;
    }

    public void setCandidateId(Long candidateId) {
        this.candidateId = candidateId;
    }

    public String getCandidateName() {
        return candidateName;
    }

    public void setCandidateName(String candidateName) {
        this.candidateName = candidateName;
    }

    public Long getJobId() {
        return jobId;
    }

    public void setJobId(Long jobId) {
        this.jobId = jobId;
    }

    public String getJobTitle() {
        return jobTitle;
    }

    public void setJobTitle(String jobTitle) {
        this.jobTitle = jobTitle;
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

    public String getInterviewerName() {
        return interviewerName;
    }

    public void setInterviewerName(String interviewerName) {
        this.interviewerName = interviewerName;
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

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public List<InterviewSlotDTO> getSlots() {
        return slots;
    }

    public void setSlots(List<InterviewSlotDTO> slots) {
        this.slots = slots;
    }

    public EvaluationSubmitDTO getEvaluation() {
        return evaluation;
    }

    public void setEvaluation(EvaluationSubmitDTO evaluation) {
        this.evaluation = evaluation;
    }
}
