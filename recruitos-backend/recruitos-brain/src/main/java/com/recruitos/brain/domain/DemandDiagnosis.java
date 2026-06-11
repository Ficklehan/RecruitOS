package com.recruitos.brain.domain;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * 触点1：AI需求诊断 — 业务目标→能力缺口→人才方案 的完整诊断链路。
 */
public class DemandDiagnosis implements Serializable {
    private static final long serialVersionUID = 1L;

    private String businessObjective;
    private List<TeamMemberBrief> currentTeam;
    private List<CapabilityGap> capabilityGaps;
    private HireRecommendation recommendation;
    private Double confidence;
    private List<String> riskWarnings;
    private String suggestedJobTitle;
    private String suggestedLevel;
    private String budgetRange;
    private List<String> suggestedChannels;

    public static class TeamMemberBrief implements Serializable {
        private String name, level;
        private List<String> skills;
        private boolean hasGapSkill;
        public String getName() { return name; }
        public void setName(String n) { name = n; }
        public String getLevel() { return level; }
        public void setLevel(String l) { level = l; }
        public List<String> getSkills() { return skills; }
        public void setSkills(List<String> s) { skills = s; }
        public boolean isHasGapSkill() { return hasGapSkill; }
        public void setHasGapSkill(boolean h) { hasGapSkill = h; }
    }

    public static class CapabilityGap implements Serializable {
        private String skill, required, current;
        private double coverage;
        private String severity;
        public String getSkill() { return skill; }
        public void setSkill(String s) { skill = s; }
        public String getRequired() { return required; }
        public void setRequired(String r) { required = r; }
        public String getCurrent() { return current; }
        public void setCurrent(String c) { current = c; }
        public double getCoverage() { return coverage; }
        public void setCoverage(double c) { coverage = c; }
        public String getSeverity() { return severity; }
        public void setSeverity(String s) { severity = s; }
    }

    public static class HireRecommendation implements Serializable {
        private String suggestedTitle, suggestedLevel, reasoning, budgetRange;
        private List<String> mustHaveSkills, niceToHaveSkills, suggestedChannels;
        private List<InterviewDimension> interviewDimensions;
        public String getSuggestedTitle() { return suggestedTitle; }
        public void setSuggestedTitle(String s) { suggestedTitle = s; }
        public String getSuggestedLevel() { return suggestedLevel; }
        public void setSuggestedLevel(String s) { suggestedLevel = s; }
        public String getReasoning() { return reasoning; }
        public void setReasoning(String r) { reasoning = r; }
        public String getBudgetRange() { return budgetRange; }
        public void setBudgetRange(String b) { budgetRange = b; }
        public List<String> getMustHaveSkills() { return mustHaveSkills; }
        public void setMustHaveSkills(List<String> s) { mustHaveSkills = s; }
        public List<String> getNiceToHaveSkills() { return niceToHaveSkills; }
        public void setNiceToHaveSkills(List<String> s) { niceToHaveSkills = s; }
        public List<String> getSuggestedChannels() { return suggestedChannels; }
        public void setSuggestedChannels(List<String> s) { suggestedChannels = s; }
        public List<InterviewDimension> getInterviewDimensions() { return interviewDimensions; }
        public void setInterviewDimensions(List<InterviewDimension> d) { interviewDimensions = d; }
    }

    public static class InterviewDimension implements Serializable {
        private String name, focus;
        private double weight;
        public String getName() { return name; }
        public void setName(String n) { name = n; }
        public String getFocus() { return focus; }
        public void setFocus(String f) { focus = f; }
        public double getWeight() { return weight; }
        public void setWeight(double w) { weight = w; }
    }

    // Getters & Setters
    public String getBusinessObjective() { return businessObjective; }
    public void setBusinessObjective(String b) { businessObjective = b; }
    public List<TeamMemberBrief> getCurrentTeam() { return currentTeam; }
    public void setCurrentTeam(List<TeamMemberBrief> t) { currentTeam = t; }
    public List<CapabilityGap> getCapabilityGaps() { return capabilityGaps; }
    public void setCapabilityGaps(List<CapabilityGap> g) { capabilityGaps = g; }
    public HireRecommendation getRecommendation() { return recommendation; }
    public void setRecommendation(HireRecommendation r) { recommendation = r; }
    public Double getConfidence() { return confidence; }
    public void setConfidence(Double c) { confidence = c; }
    public List<String> getRiskWarnings() { return riskWarnings; }
    public void setRiskWarnings(List<String> w) { riskWarnings = w; }
    public String getSuggestedJobTitle() { return suggestedJobTitle; }
    public void setSuggestedJobTitle(String s) { suggestedJobTitle = s; }
    public String getSuggestedLevel() { return suggestedLevel; }
    public void setSuggestedLevel(String s) { suggestedLevel = s; }
    public String getBudgetRange() { return budgetRange; }
    public void setBudgetRange(String b) { budgetRange = b; }
    public List<String> getSuggestedChannels() { return suggestedChannels; }
    public void setSuggestedChannels(List<String> c) { suggestedChannels = c; }
}
