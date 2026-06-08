package com.recruitos.common.match;

/**
 * 匹配结论计算所需的候选人快照（跨模块复用）
 */
public class MatchVerdictCandidateSnapshot {

    private String education;
    private String tags;
    private String currentCompany;
    private String currentTitle;
    private Integer workYears;
    private boolean hasParsedResume;

    public String getEducation() {
        return education;
    }

    public void setEducation(String education) {
        this.education = education;
    }

    public String getTags() {
        return tags;
    }

    public void setTags(String tags) {
        this.tags = tags;
    }

    public String getCurrentCompany() {
        return currentCompany;
    }

    public void setCurrentCompany(String currentCompany) {
        this.currentCompany = currentCompany;
    }

    public String getCurrentTitle() {
        return currentTitle;
    }

    public void setCurrentTitle(String currentTitle) {
        this.currentTitle = currentTitle;
    }

    public Integer getWorkYears() {
        return workYears;
    }

    public void setWorkYears(Integer workYears) {
        this.workYears = workYears;
    }

    public boolean isHasParsedResume() {
        return hasParsedResume;
    }

    public void setHasParsedResume(boolean hasParsedResume) {
        this.hasParsedResume = hasParsedResume;
    }
}
