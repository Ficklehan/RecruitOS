package com.recruitos.interview.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.recruitos.common.mybatis.BaseEntity;

import java.time.LocalDateTime;

/**
 * Interview evaluation entity
 */
@TableName("interview_evaluation")
public class InterviewEvaluation extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /** Interview ID */
    private Long interviewId;

    /** Round: INITIAL / FINAL */
    private String round;

    /** Decision: PASS / FAIL / PENDING */
    private String decision;

    /** Overall score */
    private Integer overallScore;

    /** Dimensions JSON (e.g. [{"name":"technical","score":8,"comment":"good"}]) */
    private String dimensions;

    /** Evolution feedback JSON */
    private String evolutionFeedback;

    /** Additional comment */
    private String comment;

    /** Submitted at */
    private LocalDateTime submittedAt;

    // Getters and Setters

    public Long getInterviewId() {
        return interviewId;
    }

    public void setInterviewId(Long interviewId) {
        this.interviewId = interviewId;
    }

    public String getRound() {
        return round;
    }

    public void setRound(String round) {
        this.round = round;
    }

    public String getDecision() {
        return decision;
    }

    public void setDecision(String decision) {
        this.decision = decision;
    }

    public Integer getOverallScore() {
        return overallScore;
    }

    public void setOverallScore(Integer overallScore) {
        this.overallScore = overallScore;
    }

    public String getDimensions() {
        return dimensions;
    }

    public void setDimensions(String dimensions) {
        this.dimensions = dimensions;
    }

    public String getEvolutionFeedback() {
        return evolutionFeedback;
    }

    public void setEvolutionFeedback(String evolutionFeedback) {
        this.evolutionFeedback = evolutionFeedback;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public LocalDateTime getSubmittedAt() {
        return submittedAt;
    }

    public void setSubmittedAt(LocalDateTime submittedAt) {
        this.submittedAt = submittedAt;
    }
}
