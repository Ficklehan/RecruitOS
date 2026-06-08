package com.recruitos.candidate.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.recruitos.common.mybatis.BaseEntity;

import java.math.BigDecimal;

/**
 * Candidate-Job association entity mapped to candidate_job table
 */
@TableName("candidate_job")
public class CandidateJob extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /** Associated candidate ID */
    private Long candidateId;

    /** Associated job ID */
    private Long jobId;

    /** Match score (0-100) */
    private BigDecimal matchScore;

    /** Match detail (JSON string with breakdown) */
    private String matchDetail;

    /** Screening status: PENDING/PASSED/REJECTED/RESERVE */
    private String screeningStatus;

    /** Screener user ID */
    private Long screenerId;

    /** Screener comment */
    private String screenerComment;

    /** Pipeline stage */
    private String pipelineStage;

    /** Interview sub-stage within INTERVIEWING */
    private String interviewSubStage;

    private String rejectionReasonCode;

    private String rejectionComment;

    private Integer archivedToPool;

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

    public String getPipelineStage() {
        return pipelineStage;
    }

    public void setPipelineStage(String pipelineStage) {
        this.pipelineStage = pipelineStage;
    }

    public String getInterviewSubStage() {
        return interviewSubStage;
    }

    public void setInterviewSubStage(String interviewSubStage) {
        this.interviewSubStage = interviewSubStage;
    }

    public String getRejectionReasonCode() {
        return rejectionReasonCode;
    }

    public void setRejectionReasonCode(String rejectionReasonCode) {
        this.rejectionReasonCode = rejectionReasonCode;
    }

    public String getRejectionComment() {
        return rejectionComment;
    }

    public void setRejectionComment(String rejectionComment) {
        this.rejectionComment = rejectionComment;
    }

    public Integer getArchivedToPool() {
        return archivedToPool;
    }

    public void setArchivedToPool(Integer archivedToPool) {
        this.archivedToPool = archivedToPool;
    }
}
