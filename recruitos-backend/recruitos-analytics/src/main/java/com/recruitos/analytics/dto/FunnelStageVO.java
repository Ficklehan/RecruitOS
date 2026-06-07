package com.recruitos.analytics.dto;

import java.io.Serializable;

/**
 * Value Object for a single funnel stage
 */
public class FunnelStageVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** Stage name */
    private String stageName;

    /** Count of candidates at this stage */
    private Integer count;

    /** Conversion rate from previous stage (percentage) */
    private Double conversionRate;

    // Getters and Setters

    public String getStageName() {
        return stageName;
    }

    public void setStageName(String stageName) {
        this.stageName = stageName;
    }

    public Integer getCount() {
        return count;
    }

    public void setCount(Integer count) {
        this.count = count;
    }

    public Double getConversionRate() {
        return conversionRate;
    }

    public void setConversionRate(Double conversionRate) {
        this.conversionRate = conversionRate;
    }
}
