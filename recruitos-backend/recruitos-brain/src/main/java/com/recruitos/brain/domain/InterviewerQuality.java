package com.recruitos.brain.domain;

import java.io.Serializable; import java.util.List; import java.util.Map;

/** 触点7：AI面试官质量治理 */
public class InterviewerQuality implements Serializable {
    private Long interviewerId; private String interviewerName;
    private Double qualityScore; private String qualityLevel;
    private int totalEvaluations; private Double leniencyIndex;
    private Double avgScore; private Double globalAvgScore;
    private List<BiasTag> biasTags; private List<TrendPoint> trend;
    private Double predictionAccuracy; private boolean needsRecertification;
    private List<String> coachingSuggestions;

    public static class BiasTag implements Serializable {
        private String tag; private String description; private double severity;
        public String getTag() { return tag; } public void setTag(String t) { tag = t; }
        public String getDescription() { return description; } public void setDescription(String d) { description = d; }
        public double getSeverity() { return severity; } public void setSeverity(double s) { severity = s; }
    }
    public static class TrendPoint implements Serializable {
        private String period; private double avgScore; private double leniency;
        public String getPeriod() { return period; } public void setPeriod(String p) { period = p; }
        public double getAvgScore() { return avgScore; } public void setAvgScore(double a) { avgScore = a; }
        public double getLeniency() { return leniency; } public void setLeniency(double l) { leniency = l; }
    }

    public Long getInterviewerId() { return interviewerId; } public void setInterviewerId(Long i) { interviewerId = i; }
    public String getInterviewerName() { return interviewerName; } public void setInterviewerName(String n) { interviewerName = n; }
    public Double getQualityScore() { return qualityScore; } public void setQualityScore(Double s) { qualityScore = s; }
    public String getQualityLevel() { return qualityLevel; } public void setQualityLevel(String l) { qualityLevel = l; }
    public int getTotalEvaluations() { return totalEvaluations; } public void setTotalEvaluations(int t) { totalEvaluations = t; }
    public Double getLeniencyIndex() { return leniencyIndex; } public void setLeniencyIndex(Double l) { leniencyIndex = l; }
    public Double getAvgScore() { return avgScore; } public void setAvgScore(Double a) { avgScore = a; }
    public Double getGlobalAvgScore() { return globalAvgScore; } public void setGlobalAvgScore(Double g) { globalAvgScore = g; }
    public List<BiasTag> getBiasTags() { return biasTags; } public void setBiasTags(List<BiasTag> b) { biasTags = b; }
    public List<TrendPoint> getTrend() { return trend; } public void setTrend(List<TrendPoint> t) { trend = t; }
    public Double getPredictionAccuracy() { return predictionAccuracy; } public void setPredictionAccuracy(Double p) { predictionAccuracy = p; }
    public boolean isNeedsRecertification() { return needsRecertification; } public void setNeedsRecertification(boolean n) { needsRecertification = n; }
    public List<String> getCoachingSuggestions() { return coachingSuggestions; } public void setCoachingSuggestions(List<String> c) { coachingSuggestions = c; }
}
