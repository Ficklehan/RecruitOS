package com.recruitos.candidate.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.io.Serializable;
import java.time.LocalDateTime;

@TableName("pipeline_stage_log")
public class PipelineStageLog implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long tenantId;
    private Long candidateJobId;
    private String fromStage;
    private String toStage;
    private String fromSubStage;
    private String toSubStage;
    private Long operatorId;
    private String reasonCode;
    private String comment;
    private LocalDateTime createdAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getTenantId() { return tenantId; }
    public void setTenantId(Long tenantId) { this.tenantId = tenantId; }
    public Long getCandidateJobId() { return candidateJobId; }
    public void setCandidateJobId(Long candidateJobId) { this.candidateJobId = candidateJobId; }
    public String getFromStage() { return fromStage; }
    public void setFromStage(String fromStage) { this.fromStage = fromStage; }
    public String getToStage() { return toStage; }
    public void setToStage(String toStage) { this.toStage = toStage; }
    public String getFromSubStage() { return fromSubStage; }
    public void setFromSubStage(String fromSubStage) { this.fromSubStage = fromSubStage; }
    public String getToSubStage() { return toSubStage; }
    public void setToSubStage(String toSubStage) { this.toSubStage = toSubStage; }
    public Long getOperatorId() { return operatorId; }
    public void setOperatorId(Long operatorId) { this.operatorId = operatorId; }
    public String getReasonCode() { return reasonCode; }
    public void setReasonCode(String reasonCode) { this.reasonCode = reasonCode; }
    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
