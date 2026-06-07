package com.recruitos.analytics.dto;

import java.io.Serializable;
import java.util.List;

/**
 * Value Object for ROI (Return on Investment) data
 */
public class RoiVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** Date range start */
    private String dateFrom;

    /** Date range end */
    private String dateTo;

    /** Total cost across all channels */
    private Double totalCost;

    /** Total hires across all channels */
    private Integer totalHires;

    /** Overall cost per hire */
    private Double overallCostPerHire;

    /** Channel breakdown */
    private List<RoiChannelVO> channels;

    // Getters and Setters

    public String getDateFrom() {
        return dateFrom;
    }

    public void setDateFrom(String dateFrom) {
        this.dateFrom = dateFrom;
    }

    public String getDateTo() {
        return dateTo;
    }

    public void setDateTo(String dateTo) {
        this.dateTo = dateTo;
    }

    public Double getTotalCost() {
        return totalCost;
    }

    public void setTotalCost(Double totalCost) {
        this.totalCost = totalCost;
    }

    public Integer getTotalHires() {
        return totalHires;
    }

    public void setTotalHires(Integer totalHires) {
        this.totalHires = totalHires;
    }

    public Double getOverallCostPerHire() {
        return overallCostPerHire;
    }

    public void setOverallCostPerHire(Double overallCostPerHire) {
        this.overallCostPerHire = overallCostPerHire;
    }

    public List<RoiChannelVO> getChannels() {
        return channels;
    }

    public void setChannels(List<RoiChannelVO> channels) {
        this.channels = channels;
    }
}
