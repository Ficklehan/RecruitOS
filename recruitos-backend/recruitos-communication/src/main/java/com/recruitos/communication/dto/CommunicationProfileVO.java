package com.recruitos.communication.dto;

import java.io.Serializable;
import java.util.List;

public class CommunicationProfileVO implements Serializable {

    private Long id;
    private Long jobId;
    private String persona;
    private String companyBackground;
    private String communicationLogic;
    private List<String> proactiveTriggers;
    private String guardrails;
    private boolean jobOverride;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getJobId() { return jobId; }
    public void setJobId(Long jobId) { this.jobId = jobId; }
    public String getPersona() { return persona; }
    public void setPersona(String persona) { this.persona = persona; }
    public String getCompanyBackground() { return companyBackground; }
    public void setCompanyBackground(String companyBackground) { this.companyBackground = companyBackground; }
    public String getCommunicationLogic() { return communicationLogic; }
    public void setCommunicationLogic(String communicationLogic) { this.communicationLogic = communicationLogic; }
    public List<String> getProactiveTriggers() { return proactiveTriggers; }
    public void setProactiveTriggers(List<String> proactiveTriggers) { this.proactiveTriggers = proactiveTriggers; }
    public String getGuardrails() { return guardrails; }
    public void setGuardrails(String guardrails) { this.guardrails = guardrails; }
    public boolean isJobOverride() { return jobOverride; }
    public void setJobOverride(boolean jobOverride) { this.jobOverride = jobOverride; }
}
