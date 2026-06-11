package com.recruitos.brain.domain;

import java.io.Serializable; import java.util.List;

/** 触点8：AI人才密度评估 */
public class TalentDensity implements Serializable {
    private Long orgId; private String orgName;
    private Double densityScore; private String densityLevel;
    private List<CapabilityHeatmap> heatmap;
    private List<RecruitingIncrement> increments;
    private List<AttritionImpact> attritionImpacts;
    private String barRaiserVerdict; private Double confidence;

    public static class CapabilityHeatmap implements Serializable {
        private String capability; private Double currentLevel;
        private Double targetLevel; private String status;
        public String getCapability() { return capability; } public void setCapability(String c) { capability = c; }
        public Double getCurrentLevel() { return currentLevel; } public void setCurrentLevel(Double l) { currentLevel = l; }
        public Double getTargetLevel() { return targetLevel; } public void setTargetLevel(Double t) { targetLevel = t; }
        public String getStatus() { return status; } public void setStatus(String s) { status = s; }
    }
    public static class RecruitingIncrement implements Serializable {
        private String capability; private Double contribution; private String newHireId;
        public String getCapability() { return capability; } public void setCapability(String c) { capability = c; }
        public Double getContribution() { return contribution; } public void setContribution(Double c) { contribution = c; }
        public String getNewHireId() { return newHireId; } public void setNewHireId(String n) { newHireId = n; }
    }
    public static class AttritionImpact implements Serializable {
        private String capability; private Double impact; private boolean critical;
        public String getCapability() { return capability; } public void setCapability(String c) { capability = c; }
        public Double getImpact() { return impact; } public void setImpact(Double i) { impact = i; }
        public boolean isCritical() { return critical; } public void setCritical(boolean c) { critical = c; }
    }

    public Long getOrgId() { return orgId; } public void setOrgId(Long o) { orgId = o; }
    public String getOrgName() { return orgName; } public void setOrgName(String n) { orgName = n; }
    public Double getDensityScore() { return densityScore; } public void setDensityScore(Double s) { densityScore = s; }
    public String getDensityLevel() { return densityLevel; } public void setDensityLevel(String l) { densityLevel = l; }
    public List<CapabilityHeatmap> getHeatmap() { return heatmap; } public void setHeatmap(List<CapabilityHeatmap> h) { heatmap = h; }
    public List<RecruitingIncrement> getIncrements() { return increments; } public void setIncrements(List<RecruitingIncrement> i) { increments = i; }
    public List<AttritionImpact> getAttritionImpacts() { return attritionImpacts; } public void setAttritionImpacts(List<AttritionImpact> a) { attritionImpacts = a; }
    public String getBarRaiserVerdict() { return barRaiserVerdict; } public void setBarRaiserVerdict(String v) { barRaiserVerdict = v; }
    public Double getConfidence() { return confidence; } public void setConfidence(Double c) { confidence = c; }
}
