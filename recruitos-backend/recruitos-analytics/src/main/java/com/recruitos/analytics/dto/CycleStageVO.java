package com.recruitos.analytics.dto;

import java.io.Serializable;

/**
 * Value Object for a single cycle stage
 */
public class CycleStageVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** Stage name */
    private String stageName;

    /** Average days at this stage */
    private Double avgDays;

    // Getters and Setters

    public String getStageName() {
        return stageName;
    }

    public void setStageName(String stageName) {
        this.stageName = stageName;
    }

    public Double getAvgDays() {
        return avgDays;
    }

    public void setAvgDays(Double avgDays) {
        this.avgDays = avgDays;
    }
}
