package com.recruitos.agent.dto;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class CampaignPlatformConfigDTO implements Serializable {

    private String platform;
    private Long primaryAccountId;
    private List<Long> auxiliaryAccountIds = new ArrayList<>();

    public String getPlatform() { return platform; }
    public void setPlatform(String platform) { this.platform = platform; }
    public Long getPrimaryAccountId() { return primaryAccountId; }
    public void setPrimaryAccountId(Long primaryAccountId) { this.primaryAccountId = primaryAccountId; }
    public List<Long> getAuxiliaryAccountIds() { return auxiliaryAccountIds; }
    public void setAuxiliaryAccountIds(List<Long> auxiliaryAccountIds) { this.auxiliaryAccountIds = auxiliaryAccountIds; }
}
