package com.recruitos.agent.dto;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class WorkflowCreateDTO implements Serializable {

    private String name;
    private Long jobId;
    /** 绑定的已确认运营包 ID；为空则自动取 ACTIVE */
    private Long opsPackId;
    private Integer opsPackVersion;
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
    /** 覆盖运营包：SCREEN_THEN_GREET / COLLECT_ONLY / CARD_GREET */
    private String greetStrategy;
    /** RECOMMEND / SEARCH / LATEST */
    private String searchSource;
    private Map<String, Integer> platformQuotas;
    /** CARD_GREET 须勾选风险提示 */
    private Boolean cardGreetRiskAccepted;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Long getJobId() { return jobId; }
    public void setJobId(Long jobId) { this.jobId = jobId; }
    public Long getOpsPackId() { return opsPackId; }
    public void setOpsPackId(Long opsPackId) { this.opsPackId = opsPackId; }
    public Integer getOpsPackVersion() { return opsPackVersion; }
    public void setOpsPackVersion(Integer opsPackVersion) { this.opsPackVersion = opsPackVersion; }
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
    public String getGreetStrategy() { return greetStrategy; }
    public void setGreetStrategy(String greetStrategy) { this.greetStrategy = greetStrategy; }
    public String getSearchSource() { return searchSource; }
    public void setSearchSource(String searchSource) { this.searchSource = searchSource; }
    public Map<String, Integer> getPlatformQuotas() { return platformQuotas; }
    public void setPlatformQuotas(Map<String, Integer> platformQuotas) { this.platformQuotas = platformQuotas; }
    public Boolean getCardGreetRiskAccepted() { return cardGreetRiskAccepted; }
    public void setCardGreetRiskAccepted(Boolean cardGreetRiskAccepted) { this.cardGreetRiskAccepted = cardGreetRiskAccepted; }
}
