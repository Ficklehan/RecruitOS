package com.recruitos.analytics.dto;

import java.io.Serializable;
import java.util.List;

/**
 * Value Object for recruitment cycle time data
 */
public class CycleVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** Date range start */
    private String dateFrom;

    /** Date range end */
    private String dateTo;

    /** Total average cycle time in days */
    private Double totalAvgCycleDays;

    /** Cycle stages breakdown */
    private List<CycleStageVO> stages;

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

    public Double getTotalAvgCycleDays() {
        return totalAvgCycleDays;
    }

    public void setTotalAvgCycleDays(Double totalAvgCycleDays) {
        this.totalAvgCycleDays = totalAvgCycleDays;
    }

    public List<CycleStageVO> getStages() {
        return stages;
    }

    public void setStages(List<CycleStageVO> stages) {
        this.stages = stages;
    }
}
