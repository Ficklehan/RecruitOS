package com.recruitos.brain.domain;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * 触点3：AI校准会主持 — 多评价对比矩阵、偏差检测、主持脚本。
 */
public class CalibrationSession implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long jobId;
    private String jobTitle;
    private Long candidateId;
    private String candidateName;
    private List<EvaluatorComparison> comparisons;
    private List<Dimension> dimensions;
    private List<BiasDetection> biasDetections;
    private List<String> silentDimensions;
    private String moderatorScript;
    private String hireRecommendation;
    private Double consensusScore;
    private Double confidence;

    public static class EvaluatorComparison implements Serializable {
        private String evaluatorName;
        private List<DimensionScore> scores;
        private String overallVerdict;
        public String getEvaluatorName() { return evaluatorName; }
        public void setEvaluatorName(String n) { evaluatorName = n; }
        public List<DimensionScore> getScores() { return scores; }
        public void setScores(List<DimensionScore> s) { scores = s; }
        public String getOverallVerdict() { return overallVerdict; }
        public void setOverallVerdict(String v) { overallVerdict = v; }
    }

    public static class DimensionScore implements Serializable {
        private String dimension;
        private int score;
        private String evidence;
        private String signalStrength;
        public String getDimension() { return dimension; }
        public void setDimension(String d) { dimension = d; }
        public int getScore() { return score; }
        public void setScore(int s) { score = s; }
        public String getEvidence() { return evidence; }
        public void setEvidence(String e) { evidence = e; }
        public String getSignalStrength() { return signalStrength; }
        public void setSignalStrength(String s) { signalStrength = s; }
    }

    public static class Dimension implements Serializable {
        private String name;
        private double weight;
        private int avgScore;
        private int maxGap;
        private boolean disputed;
        private String aiRecommendation;
        public String getName() { return name; }
        public void setName(String n) { name = n; }
        public double getWeight() { return weight; }
        public void setWeight(double w) { weight = w; }
        public int getAvgScore() { return avgScore; }
        public void setAvgScore(int a) { avgScore = a; }
        public int getMaxGap() { return maxGap; }
        public void setMaxGap(int m) { maxGap = m; }
        public boolean isDisputed() { return disputed; }
        public void setDisputed(boolean d) { disputed = d; }
        public String getAiRecommendation() { return aiRecommendation; }
        public void setAiRecommendation(String r) { aiRecommendation = r; }
    }

    public static class BiasDetection implements Serializable {
        private String evaluatorName;
        private String biasType;
        private String description;
        private double confidence;
        public String getEvaluatorName() { return evaluatorName; }
        public void setEvaluatorName(String n) { evaluatorName = n; }
        public String getBiasType() { return biasType; }
        public void setBiasType(String t) { biasType = t; }
        public String getDescription() { return description; }
        public void setDescription(String d) { description = d; }
        public double getConfidence() { return confidence; }
        public void setConfidence(double c) { confidence = c; }
    }

    public Long getJobId() { return jobId; }
    public void setJobId(Long j) { jobId = j; }
    public String getJobTitle() { return jobTitle; }
    public void setJobTitle(String t) { jobTitle = t; }
    public Long getCandidateId() { return candidateId; }
    public void setCandidateId(Long c) { candidateId = c; }
    public String getCandidateName() { return candidateName; }
    public void setCandidateName(String n) { candidateName = n; }
    public List<EvaluatorComparison> getComparisons() { return comparisons; }
    public void setComparisons(List<EvaluatorComparison> c) { comparisons = c; }
    public List<Dimension> getDimensions() { return dimensions; }
    public void setDimensions(List<Dimension> d) { dimensions = d; }
    public List<BiasDetection> getBiasDetections() { return biasDetections; }
    public void setBiasDetections(List<BiasDetection> b) { biasDetections = b; }
    public List<String> getSilentDimensions() { return silentDimensions; }
    public void setSilentDimensions(List<String> s) { silentDimensions = s; }
    public String getModeratorScript() { return moderatorScript; }
    public void setModeratorScript(String s) { moderatorScript = s; }
    public String getHireRecommendation() { return hireRecommendation; }
    public void setHireRecommendation(String h) { hireRecommendation = h; }
    public Double getConsensusScore() { return consensusScore; }
    public void setConsensusScore(Double s) { consensusScore = s; }
    public Double getConfidence() { return confidence; }
    public void setConfidence(Double c) { confidence = c; }
}
