package com.recruitos.brain.domain;

import java.io.Serializable;
import java.util.List;

/** 触点5：招聘周期预测 + 主动干预 */
public class CyclePrediction implements Serializable {
    private Long jobId; private String jobTitle;
    private int estimatedDays; private int minDays; private int maxDays;
    private String riskLevel;
    private List<Bottleneck> bottlenecks;
    private List<Intervention> interventions;
    private Double confidence;

    public static class Bottleneck implements Serializable {
        private String stage; private String issue; private double impact;
        public String getStage() { return stage; } public void setStage(String s) { stage = s; }
        public String getIssue() { return issue; } public void setIssue(String i) { issue = i; }
        public double getImpact() { return impact; } public void setImpact(double i) { impact = i; }
    }
    public static class Intervention implements Serializable {
        private String action; private String expectedEffect; private int effortDays;
        public String getAction() { return action; } public void setAction(String a) { action = a; }
        public String getExpectedEffect() { return expectedEffect; } public void setExpectedEffect(String e) { expectedEffect = e; }
        public int getEffortDays() { return effortDays; } public void setEffortDays(int d) { effortDays = d; }
    }

    public Long getJobId() { return jobId; } public void setJobId(Long j) { jobId = j; }
    public String getJobTitle() { return jobTitle; } public void setJobTitle(String t) { jobTitle = t; }
    public int getEstimatedDays() { return estimatedDays; } public void setEstimatedDays(int d) { estimatedDays = d; }
    public int getMinDays() { return minDays; } public void setMinDays(int d) { minDays = d; }
    public int getMaxDays() { return maxDays; } public void setMaxDays(int d) { maxDays = d; }
    public String getRiskLevel() { return riskLevel; } public void setRiskLevel(String r) { riskLevel = r; }
    public List<Bottleneck> getBottlenecks() { return bottlenecks; } public void setBottlenecks(List<Bottleneck> b) { bottlenecks = b; }
    public List<Intervention> getInterventions() { return interventions; } public void setInterventions(List<Intervention> i) { interventions = i; }
    public Double getConfidence() { return confidence; } public void setConfidence(Double c) { confidence = c; }
}
