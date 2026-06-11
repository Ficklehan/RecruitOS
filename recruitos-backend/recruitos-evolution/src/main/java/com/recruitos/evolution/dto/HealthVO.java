package com.recruitos.evolution.dto;

import java.io.Serializable;
import java.util.List;

/**
 * Value Object for health check response
 */
public class HealthVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** Job ID (null for system health) */
    private Long jobId;

    /** Job title (null for system health) */
    private String jobTitle;

    /** Overall health score (0-100) */
    private Integer overallScore;

    /** Data sufficiency score (0-100) */
    private Integer dataSufficiencyScore;

    /** Weight stability score (0-100) */
    private Integer weightStabilityScore;

    /** Match quality score (0-100) */
    private Integer matchQualityScore;

    /** Evolution freshness score (0-100) */
    private Integer evolutionFreshnessScore;

    /** Evidence/explanation for weight stability score */
    private String weightStabilityEvidence;

    /** Health status: HEALTHY/WARNING/CRITICAL */
    private String status;

    /** Health alerts */
    private List<String> alerts;

    // Getters and Setters

    public Long getJobId() {
        return jobId;
    }

    public void setJobId(Long jobId) {
        this.jobId = jobId;
    }

    public String getJobTitle() {
        return jobTitle;
    }

    public void setJobTitle(String jobTitle) {
        this.jobTitle = jobTitle;
    }

    public Integer getOverallScore() {
        return overallScore;
    }

    public void setOverallScore(Integer overallScore) {
        this.overallScore = overallScore;
    }

    public Integer getDataSufficiencyScore() {
        return dataSufficiencyScore;
    }

    public void setDataSufficiencyScore(Integer dataSufficiencyScore) {
        this.dataSufficiencyScore = dataSufficiencyScore;
    }

    public Integer getWeightStabilityScore() {
        return weightStabilityScore;
    }

    public void setWeightStabilityScore(Integer weightStabilityScore) {
        this.weightStabilityScore = weightStabilityScore;
    }

    public Integer getMatchQualityScore() {
        return matchQualityScore;
    }

    public void setMatchQualityScore(Integer matchQualityScore) {
        this.matchQualityScore = matchQualityScore;
    }

    public Integer getEvolutionFreshnessScore() {
        return evolutionFreshnessScore;
    }

    public void setEvolutionFreshnessScore(Integer evolutionFreshnessScore) {
        this.evolutionFreshnessScore = evolutionFreshnessScore;
    }

    public String getWeightStabilityEvidence() {
        return weightStabilityEvidence;
    }

    public void setWeightStabilityEvidence(String weightStabilityEvidence) {
        this.weightStabilityEvidence = weightStabilityEvidence;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public List<String> getAlerts() {
        return alerts;
    }

    public void setAlerts(List<String> alerts) {
        this.alerts = alerts;
    }
}
