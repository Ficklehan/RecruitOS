package com.recruitos.brain.domain;

import java.io.Serializable;
import java.util.List;

/**
 * 触点4：AI候选人意向预测 — 意向评分、流失风险、干预建议。
 */
public class CandidateIntent implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long candidateId;
    private String candidateName;
    private Long jobId;
    private String jobTitle;
    private Double intentScore;
    private String intentLevel; // HIGH / MEDIUM / LOW
    private Double confidence;
    private List<RiskFactor> riskFactors;
    private List<String> interventionSuggestions;
    private String updatedAt;

    public static class RiskFactor implements Serializable {
        private String factor;
        private String level;
        private String detail;
        public String getFactor() { return factor; }
        public void setFactor(String f) { factor = f; }
        public String getLevel() { return level; }
        public void setLevel(String l) { level = l; }
        public String getDetail() { return detail; }
        public void setDetail(String d) { detail = d; }
    }

    public Long getCandidateId() { return candidateId; }
    public void setCandidateId(Long c) { candidateId = c; }
    public String getCandidateName() { return candidateName; }
    public void setCandidateName(String n) { candidateName = n; }
    public Long getJobId() { return jobId; }
    public void setJobId(Long j) { jobId = j; }
    public String getJobTitle() { return jobTitle; }
    public void setJobTitle(String t) { jobTitle = t; }
    public Double getIntentScore() { return intentScore; }
    public void setIntentScore(Double s) { intentScore = s; }
    public String getIntentLevel() { return intentLevel; }
    public void setIntentLevel(String l) { intentLevel = l; }
    public Double getConfidence() { return confidence; }
    public void setConfidence(Double c) { confidence = c; }
    public List<RiskFactor> getRiskFactors() { return riskFactors; }
    public void setRiskFactors(List<RiskFactor> r) { riskFactors = r; }
    public List<String> getInterventionSuggestions() { return interventionSuggestions; }
    public void setInterventionSuggestions(List<String> s) { interventionSuggestions = s; }
    public String getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(String u) { updatedAt = u; }
}
