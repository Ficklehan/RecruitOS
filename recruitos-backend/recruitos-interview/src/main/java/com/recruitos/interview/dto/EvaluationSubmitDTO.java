package com.recruitos.interview.dto;

import java.io.Serializable;

/**
 * DTO for submitting interview evaluation
 */
public class EvaluationSubmitDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** Interview ID */
    private Long interviewId;

    /** Decision: PASS / FAIL / PENDING */
    private String decision;

    /** Overall score */
    private Integer overallScore;

    /** Dimensions JSON */
    private String dimensions;

    /** Evolution feedback JSON */
    private String evolutionFeedback;

    /** Additional comment */
    private String comment;

    // Getters and Setters

    public Long getInterviewId() {
        return interviewId;
    }

    public void setInterviewId(Long interviewId) {
        this.interviewId = interviewId;
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
}
