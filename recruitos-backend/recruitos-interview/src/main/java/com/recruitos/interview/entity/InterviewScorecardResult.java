package com.recruitos.interview.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.recruitos.common.mybatis.BaseEntity;

import java.time.LocalDateTime;

/**
 * 结构化面试评分结果 — 每轮面试每个面试官的评分卡。
 */
@TableName("interview_scorecard_result")
public class InterviewScorecardResult extends BaseEntity {
    private Long interviewId;
    private Long templateId;
    private Long evaluatorId;
    private String dimensions;       // JSON: [{name, score:1-5, evidence, confidence}]
    private Double overallScore;
    private String decision;         // STRONG_HIRE/HIRE/LEANING_HIRE/LEANING_NO/NO_HIRE
    private String aiConsistencyCheck; // JSON: AI检查评分与对话一致性
    private LocalDateTime submittedAt;

    public Long getInterviewId() { return interviewId; }
    public void setInterviewId(Long interviewId) { this.interviewId = interviewId; }
    public Long getTemplateId() { return templateId; }
    public void setTemplateId(Long templateId) { this.templateId = templateId; }
    public Long getEvaluatorId() { return evaluatorId; }
    public void setEvaluatorId(Long evaluatorId) { this.evaluatorId = evaluatorId; }
    public String getDimensions() { return dimensions; }
    public void setDimensions(String dimensions) { this.dimensions = dimensions; }
    public Double getOverallScore() { return overallScore; }
    public void setOverallScore(Double overallScore) { this.overallScore = overallScore; }
    public String getDecision() { return decision; }
    public void setDecision(String decision) { this.decision = decision; }
    public String getAiConsistencyCheck() { return aiConsistencyCheck; }
    public void setAiConsistencyCheck(String aiConsistencyCheck) { this.aiConsistencyCheck = aiConsistencyCheck; }
    public LocalDateTime getSubmittedAt() { return submittedAt; }
    public void setSubmittedAt(LocalDateTime submittedAt) { this.submittedAt = submittedAt; }
}
