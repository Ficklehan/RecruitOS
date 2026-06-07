package com.recruitos.tenant.service;

public enum PlanQuotaConfig {

    STARTER(5, 1, 100, 500),
    BASIC(20, 5, 500, 2000),
    PRO(100, 20, 2000, 10000),
    ENTERPRISE(9999, 999, 99999, 999999);

    private final int maxJobs;
    private final int maxAgents;
    private final int resumeQuota;
    private final int messageQuota;

    PlanQuotaConfig(int maxJobs, int maxAgents, int resumeQuota, int messageQuota) {
        this.maxJobs = maxJobs;
        this.maxAgents = maxAgents;
        this.resumeQuota = resumeQuota;
        this.messageQuota = messageQuota;
    }

    public int getMaxJobs() { return maxJobs; }
    public int getMaxAgents() { return maxAgents; }
    public int getResumeQuota() { return resumeQuota; }
    public int getMessageQuota() { return messageQuota; }

    public static PlanQuotaConfig fromPlan(String plan) {
        try {
            return valueOf(plan.toUpperCase());
        } catch (Exception e) {
            return STARTER;
        }
    }
}
