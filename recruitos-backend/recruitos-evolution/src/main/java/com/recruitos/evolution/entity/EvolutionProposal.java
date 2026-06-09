package com.recruitos.evolution.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.io.Serializable;
import java.time.LocalDateTime;

@TableName("evolution_proposal")
public class EvolutionProposal implements Serializable {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    @TableField("tenant_id")
    private Long tenantId;
    private Long jobId;
    private String proposalType;
    private String status;
    private String title;
    private String diffJson;
    private String evidenceJson;
    private String abResultJson;
    private String proposedOpsPackJson;
    private Integer baseOpsPackVersion;
    private Long reviewedBy;
    private LocalDateTime reviewedAt;
    private String rejectReason;
    private LocalDateTime expiresAt;
    @TableField("created_at")
    private LocalDateTime createdAt;
    @TableField("updated_at")
    private LocalDateTime updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getTenantId() { return tenantId; }
    public void setTenantId(Long tenantId) { this.tenantId = tenantId; }
    public Long getJobId() { return jobId; }
    public void setJobId(Long jobId) { this.jobId = jobId; }
    public String getProposalType() { return proposalType; }
    public void setProposalType(String proposalType) { this.proposalType = proposalType; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDiffJson() { return diffJson; }
    public void setDiffJson(String diffJson) { this.diffJson = diffJson; }
    public String getEvidenceJson() { return evidenceJson; }
    public void setEvidenceJson(String evidenceJson) { this.evidenceJson = evidenceJson; }
    public String getAbResultJson() { return abResultJson; }
    public void setAbResultJson(String abResultJson) { this.abResultJson = abResultJson; }
    public String getProposedOpsPackJson() { return proposedOpsPackJson; }
    public void setProposedOpsPackJson(String proposedOpsPackJson) { this.proposedOpsPackJson = proposedOpsPackJson; }
    public Integer getBaseOpsPackVersion() { return baseOpsPackVersion; }
    public void setBaseOpsPackVersion(Integer baseOpsPackVersion) { this.baseOpsPackVersion = baseOpsPackVersion; }
    public Long getReviewedBy() { return reviewedBy; }
    public void setReviewedBy(Long reviewedBy) { this.reviewedBy = reviewedBy; }
    public LocalDateTime getReviewedAt() { return reviewedAt; }
    public void setReviewedAt(LocalDateTime reviewedAt) { this.reviewedAt = reviewedAt; }
    public String getRejectReason() { return rejectReason; }
    public void setRejectReason(String rejectReason) { this.rejectReason = rejectReason; }
    public LocalDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
