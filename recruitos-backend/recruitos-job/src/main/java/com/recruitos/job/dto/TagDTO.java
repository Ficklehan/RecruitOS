package com.recruitos.job.dto;

import java.io.Serializable;

/**
 * DTO representing a tag with tri-weight scoring
 */
public class TagDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** Tag name / keyword */
    private String tag;

    /** Match weight: how closely the candidate matches this tag (0.0 ~ 1.0) */
    private Double matchWeight;

    /** Search weight: importance for search/recall (0.0 ~ 1.0) */
    private Double searchWeight;

    /** Decision weight: importance for final decision (0.0 ~ 1.0) */
    private Double decisionWeight;

    /** Whether this tag is manually locked (cannot be overwritten by auto-parse) */
    private Boolean locked;

    // Getters and Setters

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    public Double getMatchWeight() {
        return matchWeight;
    }

    public void setMatchWeight(Double matchWeight) {
        this.matchWeight = matchWeight;
    }

    public Double getSearchWeight() {
        return searchWeight;
    }

    public void setSearchWeight(Double searchWeight) {
        this.searchWeight = searchWeight;
    }

    public Double getDecisionWeight() {
        return decisionWeight;
    }

    public void setDecisionWeight(Double decisionWeight) {
        this.decisionWeight = decisionWeight;
    }

    public Boolean getLocked() {
        return locked;
    }

    public void setLocked(Boolean locked) {
        this.locked = locked;
    }
}
