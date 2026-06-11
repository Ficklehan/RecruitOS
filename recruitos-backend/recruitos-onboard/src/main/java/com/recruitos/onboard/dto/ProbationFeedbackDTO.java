package com.recruitos.onboard.dto;

import java.io.Serializable;

/**
 * 试用期考核结果（HRone 回流 / Demo Webhook）
 */
public class ProbationFeedbackDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 是否通过试用期 */
    private Boolean passed;

    /** 综合评分 1-5 */
    private Integer overallScore;

    /** 备注 */
    private String comment;

    public Boolean getPassed() {
        return passed;
    }

    public void setPassed(Boolean passed) {
        this.passed = passed;
    }

    public Integer getOverallScore() {
        return overallScore;
    }

    public void setOverallScore(Integer overallScore) {
        this.overallScore = overallScore;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }
}
