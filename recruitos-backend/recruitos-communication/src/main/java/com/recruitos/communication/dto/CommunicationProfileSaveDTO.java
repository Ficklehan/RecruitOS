package com.recruitos.communication.dto;

import java.io.Serializable;
import java.util.List;

public class CommunicationProfileSaveDTO implements Serializable {

    private String persona;
    private String companyBackground;
    private String communicationLogic;
    private List<String> proactiveTriggers;
    private String guardrails;

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
}
