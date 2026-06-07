package com.recruitos.analytics.dto;

import java.io.Serializable;
import java.util.List;

/**
 * Value Object for recruitment funnel data
 */
public class FunnelVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** Date range start */
    private String dateFrom;

    /** Date range end */
    private String dateTo;

    /** Funnel stages */
    private List<FunnelStageVO> stages;

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

    public List<FunnelStageVO> getStages() {
        return stages;
    }

    public void setStages(List<FunnelStageVO> stages) {
        this.stages = stages;
    }
}
