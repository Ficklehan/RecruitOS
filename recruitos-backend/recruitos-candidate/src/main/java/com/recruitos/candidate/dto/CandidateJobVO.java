package com.recruitos.candidate.dto;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Value Object for candidate-job association, includes candidate info
 */
public class CandidateJobVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private Long candidateId;
    private Long jobId;
    private BigDecimal matchScore;
    private String matchDetail;
    private String screeningStatus;
    private Long screenerId;
    private String screenerComment;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /** Candidate info (embedded) */
    private String candidateName;
    private String candidatePhone;
    private String candidateEmail;
    private String candidateCompany;
    private String candidateTitle;

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

    public Long getJobId() {
        return jobId;
    }

    public void setJobId(Long jobId) {
        this.jobId = jobId;
    }

    public BigDecimal getMatchScore() {
        return matchScore;
    }

    public void setMatchScore(BigDecimal matchScore) {
        this.matchScore = matchScore;
    }

    public String getMatchDetail() {
        return matchDetail;
    }

    public void setMatchDetail(String matchDetail) {
        this.matchDetail = matchDetail;
    }

    public String getScreeningStatus() {
        return screeningStatus;
    }

    public void setScreeningStatus(String screeningStatus) {
        this.screeningStatus = screeningStatus;
    }

    public Long getScreenerId() {
        return screenerId;
    }

    public void setScreenerId(Long screenerId) {
        this.screenerId = screenerId;
    }

    public String getScreenerComment() {
        return screenerComment;
    }

    public void setScreenerComment(String screenerComment) {
        this.screenerComment = screenerComment;
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

    public String getCandidateName() {
        return candidateName;
    }

    public void setCandidateName(String candidateName) {
        this.candidateName = candidateName;
    }

    public String getCandidatePhone() {
        return candidatePhone;
    }

    public void setCandidatePhone(String candidatePhone) {
        this.candidatePhone = candidatePhone;
    }

    public String getCandidateEmail() {
        return candidateEmail;
    }

    public void setCandidateEmail(String candidateEmail) {
        this.candidateEmail = candidateEmail;
    }

    public String getCandidateCompany() {
        return candidateCompany;
    }

    public void setCandidateCompany(String candidateCompany) {
        this.candidateCompany = candidateCompany;
    }

    public String getCandidateTitle() {
        return candidateTitle;
    }

    public void setCandidateTitle(String candidateTitle) {
        this.candidateTitle = candidateTitle;
    }
}
