package com.recruitos.brain.domain;

import java.io.Serializable;
import java.util.List;

/**
 * 触点2：AI面试实时辅助 — 面试官副驾面板数据。
 */
public class InterviewAssist implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long interviewId;
    private String candidateName;
    private String jobTitle;
    private String evaluatorDimension;
    private Double dimensionWeight;
    private List<ResumeSignal> resumeSignals;
    private List<String> suggestedQuestions;
    private List<String> cautions;
    private List<ScoreAnchor> scoreAnchors;
    private List<String> biasReminders;
    private List<DimensionCoverage> dimensionCoverage;

    public static class ResumeSignal implements Serializable {
        private String type, content, detail;
        public String getType() { return type; }
        public void setType(String t) { type = t; }
        public String getContent() { return content; }
        public void setContent(String c) { content = c; }
        public String getDetail() { return detail; }
        public void setDetail(String d) { detail = d; }
    }

    public static class ScoreAnchor implements Serializable {
        private int score;
        private String description;
        public int getScore() { return score; }
        public void setScore(int s) { score = s; }
        public String getDescription() { return description; }
        public void setDescription(String d) { description = d; }
    }

    public static class DimensionCoverage implements Serializable {
        private String dimension;
        private boolean covered;
        private int questionsAsked;
        public String getDimension() { return dimension; }
        public void setDimension(String d) { dimension = d; }
        public boolean isCovered() { return covered; }
        public void setCovered(boolean c) { covered = c; }
        public int getQuestionsAsked() { return questionsAsked; }
        public void setQuestionsAsked(int q) { questionsAsked = q; }
    }

    // Getters
    public Long getInterviewId() { return interviewId; }
    public void setInterviewId(Long i) { interviewId = i; }
    public String getCandidateName() { return candidateName; }
    public void setCandidateName(String n) { candidateName = n; }
    public String getJobTitle() { return jobTitle; }
    public void setJobTitle(String t) { jobTitle = t; }
    public String getEvaluatorDimension() { return evaluatorDimension; }
    public void setEvaluatorDimension(String d) { evaluatorDimension = d; }
    public Double getDimensionWeight() { return dimensionWeight; }
    public void setDimensionWeight(Double w) { dimensionWeight = w; }
    public List<ResumeSignal> getResumeSignals() { return resumeSignals; }
    public void setResumeSignals(List<ResumeSignal> s) { resumeSignals = s; }
    public List<String> getSuggestedQuestions() { return suggestedQuestions; }
    public void setSuggestedQuestions(List<String> q) { suggestedQuestions = q; }
    public List<String> getCautions() { return cautions; }
    public void setCautions(List<String> c) { cautions = c; }
    public List<ScoreAnchor> getScoreAnchors() { return scoreAnchors; }
    public void setScoreAnchors(List<ScoreAnchor> a) { scoreAnchors = a; }
    public List<String> getBiasReminders() { return biasReminders; }
    public void setBiasReminders(List<String> b) { biasReminders = b; }
    public List<DimensionCoverage> getDimensionCoverage() { return dimensionCoverage; }
    public void setDimensionCoverage(List<DimensionCoverage> d) { dimensionCoverage = d; }
}
