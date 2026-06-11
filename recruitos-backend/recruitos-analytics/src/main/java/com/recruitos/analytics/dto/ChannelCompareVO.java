package com.recruitos.analytics.dto;

import java.io.Serializable;

public class ChannelCompareVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String channelName;
    private String source;
    private int candidates;
    private int hires;
    private double conversionRate;

    public String getChannelName() {
        return channelName;
    }

    public void setChannelName(String channelName) {
        this.channelName = channelName;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public int getCandidates() {
        return candidates;
    }

    public void setCandidates(int candidates) {
        this.candidates = candidates;
    }

    public int getHires() {
        return hires;
    }

    public void setHires(int hires) {
        this.hires = hires;
    }

    public double getConversionRate() {
        return conversionRate;
    }

    public void setConversionRate(double conversionRate) {
        this.conversionRate = conversionRate;
    }
}
