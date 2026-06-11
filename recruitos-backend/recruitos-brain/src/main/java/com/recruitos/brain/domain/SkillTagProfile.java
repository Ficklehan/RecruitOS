package com.recruitos.brain.domain;

import java.util.List;
import java.util.Map;

/**
 * 技能标签画像 — 团队成员的技能标签集合，用于人才密度评估。
 */
public class SkillTagProfile {
    private Long memberId;
    private String memberName;
    private String memberLevel;
    private List<SkillTag> tags;

    public Long getMemberId() { return memberId; }
    public void setMemberId(Long v) { this.memberId = v; }
    public String getMemberName() { return memberName; }
    public void setMemberName(String v) { this.memberName = v; }
    public String getMemberLevel() { return memberLevel; }
    public void setMemberLevel(String v) { this.memberLevel = v; }
    public List<SkillTag> getTags() { return tags; }
    public void setTags(List<SkillTag> v) { this.tags = v; }

    public static class SkillTag {
        private String name;
        private String category;   // 技术/产品/管理/业务
        private int proficiency;   // 1-5
        private int yearsOfExperience;
        private String lastUsed;   // ISO date

        public String getName() { return name; }
        public void setName(String v) { this.name = v; }
        public String getCategory() { return category; }
        public void setCategory(String v) { this.category = v; }
        public int getProficiency() { return proficiency; }
        public void setProficiency(int v) { this.proficiency = v; }
        public int getYearsOfExperience() { return yearsOfExperience; }
        public void setYearsOfExperience(int v) { this.yearsOfExperience = v; }
        public String getLastUsed() { return lastUsed; }
        public void setLastUsed(String v) { this.lastUsed = v; }
    }

    /**
     * 目标能力需求 — 组织/岗位需要的能力。
     */
    public static class CapabilityTarget {
        private String capability;
        private String category;
        private int targetProficiency;  // 1-5
        private int targetHeadcount;    // 需要的人数
        private int currentHeadcount;   // 已有达到目标熟练度的人数
        private double weight;          // 重要性权重

        public String getCapability() { return capability; }
        public void setCapability(String v) { this.capability = v; }
        public String getCategory() { return category; }
        public void setCategory(String v) { this.category = v; }
        public int getTargetProficiency() { return targetProficiency; }
        public void setTargetProficiency(int v) { this.targetProficiency = v; }
        public int getTargetHeadcount() { return targetHeadcount; }
        public void setTargetHeadcount(int v) { this.targetHeadcount = v; }
        public int getCurrentHeadcount() { return currentHeadcount; }
        public void setCurrentHeadcount(int v) { this.currentHeadcount = v; }
        public double getWeight() { return weight; }
        public void setWeight(double v) { this.weight = v; }
    }
}
