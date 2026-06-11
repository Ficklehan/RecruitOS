package com.recruitos.brain.domain;

import java.io.Serializable;
import java.util.*;

/** A/B实验配置 */
public class AbExperiment implements Serializable {
    private Long id; private Long tenantId;
    private String touchpoint;          // INTENT / OFFER_STRATEGY / SCREENING
    private String name;
    private double trafficSplit;        // 0.0-1.0 实验组流量比例
    private String status;              // DRAFT / RUNNING / COMPLETED
    private Map<String, Object> controlConfig;   // 对照组配置
    private Map<String, Object> experimentConfig; // 实验组配置
    private Date startTime, endTime;
    private Map<String, Double> results; // 结果指标

    public Long getId() { return id; } public void setId(Long i) { id = i; }
    public Long getTenantId() { return tenantId; } public void setTenantId(Long t) { tenantId = t; }
    public String getTouchpoint() { return touchpoint; } public void setTouchpoint(String t) { touchpoint = t; }
    public String getName() { return name; } public void setName(String n) { name = n; }
    public double getTrafficSplit() { return trafficSplit; } public void setTrafficSplit(double t) { trafficSplit = t; }
    public String getStatus() { return status; } public void setStatus(String s) { status = s; }
    public Map<String, Object> getControlConfig() { return controlConfig; } public void setControlConfig(Map<String, Object> c) { controlConfig = c; }
    public Map<String, Object> getExperimentConfig() { return experimentConfig; } public void setExperimentConfig(Map<String, Object> e) { experimentConfig = e; }
    public Date getStartTime() { return startTime; } public void setStartTime(Date s) { startTime = s; }
    public Date getEndTime() { return endTime; } public void setEndTime(Date e) { endTime = e; }
    public Map<String, Double> getResults() { return results; } public void setResults(Map<String, Double> r) { results = r; }
}
