package com.recruitos.agent.dto;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class WorkflowCreateDTO implements Serializable {

    private String name;
    private Long jobId;
    /** FULL_AUTO / SEMI_AUTO / PUBLISH_SEARCH_ONLY */
    private String mode;
    /** 兼容旧前端：平台代码列表 */
    private List<String> platforms = new ArrayList<>();
    /** 新：每平台主/辅账号 */
    private List<CampaignPlatformConfigDTO> platformConfigs = new ArrayList<>();
    private List<String> keywords = new ArrayList<>();
    private Integer dailyLimit;
    private Long templateId;
    private Boolean resumeConfirmRequired;
    private Boolean publishConfirmRequired;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Long getJobId() { return jobId; }
    public void setJobId(Long jobId) { this.jobId = jobId; }
    public String getMode() { return mode; }
    public void setMode(String mode) { this.mode = mode; }
    public List<String> getPlatforms() { return platforms; }
    public void setPlatforms(List<String> platforms) { this.platforms = platforms; }
    public List<CampaignPlatformConfigDTO> getPlatformConfigs() { return platformConfigs; }
    public void setPlatformConfigs(List<CampaignPlatformConfigDTO> platformConfigs) { this.platformConfigs = platformConfigs; }
    public List<String> getKeywords() { return keywords; }
    public void setKeywords(List<String> keywords) { this.keywords = keywords; }
    public Integer getDailyLimit() { return dailyLimit; }
    public void setDailyLimit(Integer dailyLimit) { this.dailyLimit = dailyLimit; }
    public Long getTemplateId() { return templateId; }
    public void setTemplateId(Long templateId) { this.templateId = templateId; }
    public Boolean getResumeConfirmRequired() { return resumeConfirmRequired; }
    public void setResumeConfirmRequired(Boolean resumeConfirmRequired) { this.resumeConfirmRequired = resumeConfirmRequired; }
    public Boolean getPublishConfirmRequired() { return publishConfirmRequired; }
    public void setPublishConfirmRequired(Boolean publishConfirmRequired) { this.publishConfirmRequired = publishConfirmRequired; }
}
