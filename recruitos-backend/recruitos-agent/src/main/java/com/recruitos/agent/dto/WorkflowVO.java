package com.recruitos.agent.dto;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class WorkflowVO implements Serializable {

    private Long id;
    private String name;
    private Long jobId;
    private String jobTitle;
    private String mode;
    private String status;
    private List<String> platforms = new ArrayList<>();
    private List<CampaignPlatformConfigDTO> platformConfigs = new ArrayList<>();
    private WorkflowStatsVO stats = new WorkflowStatsVO();
    private List<CampaignPlatformRunVO> platformRuns = new ArrayList<>();
    private List<CampaignCandidateTraceVO> pendingActions = new ArrayList<>();
    private LocalDateTime startedAt;
    private LocalDateTime createdAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Long getJobId() { return jobId; }
    public void setJobId(Long jobId) { this.jobId = jobId; }
    public String getJobTitle() { return jobTitle; }
    public void setJobTitle(String jobTitle) { this.jobTitle = jobTitle; }
    public String getMode() { return mode; }
    public void setMode(String mode) { this.mode = mode; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public List<String> getPlatforms() { return platforms; }
    public void setPlatforms(List<String> platforms) { this.platforms = platforms; }
    public List<CampaignPlatformConfigDTO> getPlatformConfigs() { return platformConfigs; }
    public void setPlatformConfigs(List<CampaignPlatformConfigDTO> platformConfigs) { this.platformConfigs = platformConfigs; }
    public WorkflowStatsVO getStats() { return stats; }
    public void setStats(WorkflowStatsVO stats) { this.stats = stats; }
    public List<CampaignPlatformRunVO> getPlatformRuns() { return platformRuns; }
    public void setPlatformRuns(List<CampaignPlatformRunVO> platformRuns) { this.platformRuns = platformRuns; }
    public List<CampaignCandidateTraceVO> getPendingActions() { return pendingActions; }
    public void setPendingActions(List<CampaignCandidateTraceVO> pendingActions) { this.pendingActions = pendingActions; }
    public LocalDateTime getStartedAt() { return startedAt; }
    public void setStartedAt(LocalDateTime startedAt) { this.startedAt = startedAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public static class WorkflowStatsVO implements Serializable {
        private int published;
        private int searched;
        private int locked;
        private int greeted;
        private int replied;
        private int resumes;
        private int imported;
        private int pendingScreening;
        private int duplicatesSkipped;
        private int alerts;

        public int getPublished() { return published; }
        public void setPublished(int published) { this.published = published; }
        public int getSearched() { return searched; }
        public void setSearched(int searched) { this.searched = searched; }
        public int getLocked() { return locked; }
        public void setLocked(int locked) { this.locked = locked; }
        public int getGreeted() { return greeted; }
        public void setGreeted(int greeted) { this.greeted = greeted; }
        public int getReplied() { return replied; }
        public void setReplied(int replied) { this.replied = replied; }
        public int getResumes() { return resumes; }
        public void setResumes(int resumes) { this.resumes = resumes; }
        public int getImported() { return imported; }
        public void setImported(int imported) { this.imported = imported; }
        public int getPendingScreening() { return pendingScreening; }
        public void setPendingScreening(int pendingScreening) { this.pendingScreening = pendingScreening; }
        public int getDuplicatesSkipped() { return duplicatesSkipped; }
        public void setDuplicatesSkipped(int duplicatesSkipped) { this.duplicatesSkipped = duplicatesSkipped; }
        public int getAlerts() { return alerts; }
        public void setAlerts(int alerts) { this.alerts = alerts; }

        /** 兼容 AgentWorkflow.vue */
        public int getGreetedAlias() { return greeted; }
        public Map<String, Integer> asLegacyStats() {
            java.util.Map<String, Integer> m = new java.util.HashMap<>();
            m.put("searched", searched);
            m.put("greeted", greeted);
            m.put("replied", replied);
            m.put("resumes", resumes);
            m.put("alerts", alerts);
            return m;
        }
    }
}
