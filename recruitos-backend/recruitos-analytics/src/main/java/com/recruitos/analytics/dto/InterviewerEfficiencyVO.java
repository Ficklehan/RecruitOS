package com.recruitos.analytics.dto;

import java.io.Serializable;

/**
 * Value Object for interviewer efficiency data
 */
public class InterviewerEfficiencyVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** Interviewer name */
    private String interviewerName;

    /** Total interview count */
    private Integer interviewCount;

    /** Pass count */
    private Integer passCount;

    /** Pass rate (percentage) */
    private Double passRate;

    /** Average score */
    private Double avgScore;

    /** Average decision days */
    private Double avgDecisionDays;

    // Getters and Setters

    public String getInterviewerName() {
        return interviewerName;
    }

    public void setInterviewerName(String interviewerName) {
        this.interviewerName = interviewerName;
    }

    public Integer getInterviewCount() {
        return interviewCount;
    }

    public void setInterviewCount(Integer interviewCount) {
        this.interviewCount = interviewCount;
    }

    public Integer getPassCount() {
        return passCount;
    }

    public void setPassCount(Integer passCount) {
        this.passCount = passCount;
    }

    public Double getPassRate() {
        return passRate;
    }

    public void setPassRate(Double passRate) {
        this.passRate = passRate;
    }

    public Double getAvgScore() {
        return avgScore;
    }

    public void setAvgScore(Double avgScore) {
        this.avgScore = avgScore;
    }

    public Double getAvgDecisionDays() {
        return avgDecisionDays;
    }

    public void setAvgDecisionDays(Double avgDecisionDays) {
        this.avgDecisionDays = avgDecisionDays;
    }
}
