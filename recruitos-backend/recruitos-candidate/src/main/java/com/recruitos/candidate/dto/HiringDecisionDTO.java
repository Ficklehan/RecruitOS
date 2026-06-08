package com.recruitos.candidate.dto;

import java.io.Serializable;

public class HiringDecisionDTO implements Serializable {

    private Long candidateJobId;
    private Long candidateId;
    private Long jobId;
    private String decision;
    private String summary;

    public Long getCandidateJobId() { return candidateJobId; }
    public void setCandidateJobId(Long candidateJobId) { this.candidateJobId = candidateJobId; }
    public Long getCandidateId() { return candidateId; }
    public void setCandidateId(Long candidateId) { this.candidateId = candidateId; }
    public Long getJobId() { return jobId; }
    public void setJobId(Long jobId) { this.jobId = jobId; }
    public String getDecision() { return decision; }
    public void setDecision(String decision) { this.decision = decision; }
    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }
}
