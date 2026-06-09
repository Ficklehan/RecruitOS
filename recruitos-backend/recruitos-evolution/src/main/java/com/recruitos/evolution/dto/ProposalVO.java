package com.recruitos.evolution.dto;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Map;

public class ProposalVO implements Serializable {

    private Long id;
    private Long jobId;
    private String proposalType;
    private String status;
    private String title;
    private Map<String, Object> diff;
    private Map<String, Object> evidence;
    private Map<String, Object> proposedOpsPack;
    private Integer baseOpsPackVersion;
    private String rejectReason;
    private LocalDateTime expiresAt;
    private LocalDateTime createdAt;
    private LocalDateTime reviewedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getJobId() { return jobId; }
    public void setJobId(Long jobId) { this.jobId = jobId; }
    public String getProposalType() { return proposalType; }
    public void setProposalType(String proposalType) { this.proposalType = proposalType; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public Map<String, Object> getDiff() { return diff; }
    public void setDiff(Map<String, Object> diff) { this.diff = diff; }
    public Map<String, Object> getEvidence() { return evidence; }
    public void setEvidence(Map<String, Object> evidence) { this.evidence = evidence; }
    public Map<String, Object> getProposedOpsPack() { return proposedOpsPack; }
    public void setProposedOpsPack(Map<String, Object> proposedOpsPack) { this.proposedOpsPack = proposedOpsPack; }
    public Integer getBaseOpsPackVersion() { return baseOpsPackVersion; }
    public void setBaseOpsPackVersion(Integer baseOpsPackVersion) { this.baseOpsPackVersion = baseOpsPackVersion; }
    public String getRejectReason() { return rejectReason; }
    public void setRejectReason(String rejectReason) { this.rejectReason = rejectReason; }
    public LocalDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getReviewedAt() { return reviewedAt; }
    public void setReviewedAt(LocalDateTime reviewedAt) { this.reviewedAt = reviewedAt; }
}
