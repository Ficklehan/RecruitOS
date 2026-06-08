package com.recruitos.agent.dto;

import java.io.Serializable;

public class CampaignPlatformRunVO implements Serializable {

    private Long id;
    private String platform;
    private Long primaryAccountId;
    private String primaryAccountName;
    private String currentStep;
    private String status;
    private String platformJobUrl;
    private String errorMessage;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getPlatform() { return platform; }
    public void setPlatform(String platform) { this.platform = platform; }
    public Long getPrimaryAccountId() { return primaryAccountId; }
    public void setPrimaryAccountId(Long primaryAccountId) { this.primaryAccountId = primaryAccountId; }
    public String getPrimaryAccountName() { return primaryAccountName; }
    public void setPrimaryAccountName(String primaryAccountName) { this.primaryAccountName = primaryAccountName; }
    public String getCurrentStep() { return currentStep; }
    public void setCurrentStep(String currentStep) { this.currentStep = currentStep; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getPlatformJobUrl() { return platformJobUrl; }
    public void setPlatformJobUrl(String platformJobUrl) { this.platformJobUrl = platformJobUrl; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
}
