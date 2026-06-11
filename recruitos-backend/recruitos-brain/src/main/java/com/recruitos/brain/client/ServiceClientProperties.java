package com.recruitos.brain.client;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "recruitos.services")
public class ServiceClientProperties {
    private String candidateUrl = "http://localhost:8085";
    private String interviewUrl = "http://localhost:8086";
    private String offerUrl = "http://localhost:8087";
    private String communicationUrl = "http://localhost:8089";
    private String jobUrl = "http://localhost:8084";
    private String demandUrl = "http://localhost:8083";
    private String evolutionUrl = "http://localhost:8090";
    private String analyticsUrl = "http://localhost:8094";
    private int timeoutMs = 3000;

    public String getCandidateUrl() { return candidateUrl; }
    public void setCandidateUrl(String u) { candidateUrl = u; }
    public String getInterviewUrl() { return interviewUrl; }
    public void setInterviewUrl(String u) { interviewUrl = u; }
    public String getOfferUrl() { return offerUrl; }
    public void setOfferUrl(String u) { offerUrl = u; }
    public String getCommunicationUrl() { return communicationUrl; }
    public void setCommunicationUrl(String u) { communicationUrl = u; }
    public String getJobUrl() { return jobUrl; }
    public void setJobUrl(String u) { jobUrl = u; }
    public String getDemandUrl() { return demandUrl; }
    public void setDemandUrl(String u) { demandUrl = u; }
    public String getEvolutionUrl() { return evolutionUrl; }
    public void setEvolutionUrl(String u) { evolutionUrl = u; }
    public String getAnalyticsUrl() { return analyticsUrl; }
    public void setAnalyticsUrl(String u) { analyticsUrl = u; }
    public int getTimeoutMs() { return timeoutMs; }
    public void setTimeoutMs(int t) { timeoutMs = t; }
}
