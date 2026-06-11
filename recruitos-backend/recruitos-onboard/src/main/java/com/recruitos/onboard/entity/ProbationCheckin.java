package com.recruitos.onboard.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.recruitos.common.mybatis.BaseEntity;

import java.time.LocalDateTime;

/**
 * 试用期验证 — L6进化信号的真实来源。
 */
@TableName("probation_checkin")
public class ProbationCheckin extends BaseEntity {
    private Long tenantId;
    private Long candidateId;
    private Long jobId;
    private Integer checkinDay;        // 30/60/90
    private Long evaluatorId;          // Hiring Manager
    private String dimensions;          // JSON: [{dimension, interviewScore, actualScore, gapAnalysis}]
    private Double overallRating;
    private Boolean wouldHireAgain;
    private String strengths;
    private String gaps;
    private String recommendation;      // CONFIRM/EXTEND_PROBATION/TERMINATE
    private String aiAnalysis;          // JSON: AI分析面试vs实际差距
    private LocalDateTime submittedAt;
    private Boolean l6SignalEmitted;

    public Long getTenantId() { return tenantId; }
    public void setTenantId(Long tenantId) { this.tenantId = tenantId; }
    public Long getCandidateId() { return candidateId; }
    public void setCandidateId(Long candidateId) { this.candidateId = candidateId; }
    public Long getJobId() { return jobId; }
    public void setJobId(Long jobId) { this.jobId = jobId; }
    public Integer getCheckinDay() { return checkinDay; }
    public void setCheckinDay(Integer checkinDay) { this.checkinDay = checkinDay; }
    public Long getEvaluatorId() { return evaluatorId; }
    public void setEvaluatorId(Long evaluatorId) { this.evaluatorId = evaluatorId; }
    public String getDimensions() { return dimensions; }
    public void setDimensions(String dimensions) { this.dimensions = dimensions; }
    public Double getOverallRating() { return overallRating; }
    public void setOverallRating(Double overallRating) { this.overallRating = overallRating; }
    public Boolean getWouldHireAgain() { return wouldHireAgain; }
    public void setWouldHireAgain(Boolean wouldHireAgain) { this.wouldHireAgain = wouldHireAgain; }
    public String getStrengths() { return strengths; }
    public void setStrengths(String strengths) { this.strengths = strengths; }
    public String getGaps() { return gaps; }
    public void setGaps(String gaps) { this.gaps = gaps; }
    public String getRecommendation() { return recommendation; }
    public void setRecommendation(String recommendation) { this.recommendation = recommendation; }
    public String getAiAnalysis() { return aiAnalysis; }
    public void setAiAnalysis(String aiAnalysis) { this.aiAnalysis = aiAnalysis; }
    public LocalDateTime getSubmittedAt() { return submittedAt; }
    public void setSubmittedAt(LocalDateTime submittedAt) { this.submittedAt = submittedAt; }
    public Boolean getL6SignalEmitted() { return l6SignalEmitted; }
    public void setL6SignalEmitted(Boolean l6SignalEmitted) { this.l6SignalEmitted = l6SignalEmitted; }
}
