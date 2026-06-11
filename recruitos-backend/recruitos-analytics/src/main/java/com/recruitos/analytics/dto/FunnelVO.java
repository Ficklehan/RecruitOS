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

    /** Pipeline funnel stages (筛选→入职) */
    private List<FunnelStageVO> stages;

    /** Campaign sourcing stages (平台检索→纳入候选人) */
    private List<FunnelStageVO> sourcingStages;

    /** Channel comparison: 自招 / 内推 / 猎头 */
    private List<ChannelCompareVO> channels;

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

    public List<FunnelStageVO> getSourcingStages() {
        return sourcingStages;
    }

    public void setSourcingStages(List<FunnelStageVO> sourcingStages) {
        this.sourcingStages = sourcingStages;
    }

    public List<ChannelCompareVO> getChannels() {
        return channels;
    }

    public void setChannels(List<ChannelCompareVO> channels) {
        this.channels = channels;
    }
}
