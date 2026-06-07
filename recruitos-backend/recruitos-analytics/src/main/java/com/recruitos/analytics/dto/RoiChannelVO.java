package com.recruitos.analytics.dto;

import java.io.Serializable;

/**
 * Value Object for a single ROI channel
 */
public class RoiChannelVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** Channel name */
    private String channelName;

    /** Total cost */
    private Double cost;

    /** Number of hires */
    private Integer hires;

    /** Cost per hire */
    private Double costPerHire;

    /** Conversion rate (percentage) */
    private Double conversionRate;

    // Getters and Setters

    public String getChannelName() {
        return channelName;
    }

    public void setChannelName(String channelName) {
        this.channelName = channelName;
    }

    public Double getCost() {
        return cost;
    }

    public void setCost(Double cost) {
        this.cost = cost;
    }

    public Integer getHires() {
        return hires;
    }

    public void setHires(Integer hires) {
        this.hires = hires;
    }

    public Double getCostPerHire() {
        return costPerHire;
    }

    public void setCostPerHire(Double costPerHire) {
        this.costPerHire = costPerHire;
    }

    public Double getConversionRate() {
        return conversionRate;
    }

    public void setConversionRate(Double conversionRate) {
        this.conversionRate = conversionRate;
    }
}
