package com.recruitos.communication.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.io.Serializable;
import java.time.LocalDateTime;

@TableName("communication_profile")
public class CommunicationProfile implements Serializable {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    @TableField("tenant_id")
    private Long tenantId;
    /** NULL = 租户默认 */
    private Long jobId;
    private String persona;
    private String companyBackground;
    private String communicationLogic;
    private String proactiveTriggersJson;
    private String guardrails;
    private Long createdBy;
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
    public String getPersona() { return persona; }
    public void setPersona(String persona) { this.persona = persona; }
    public String getCompanyBackground() { return companyBackground; }
    public void setCompanyBackground(String companyBackground) { this.companyBackground = companyBackground; }
    public String getCommunicationLogic() { return communicationLogic; }
    public void setCommunicationLogic(String communicationLogic) { this.communicationLogic = communicationLogic; }
    public String getProactiveTriggersJson() { return proactiveTriggersJson; }
    public void setProactiveTriggersJson(String proactiveTriggersJson) { this.proactiveTriggersJson = proactiveTriggersJson; }
    public String getGuardrails() { return guardrails; }
    public void setGuardrails(String guardrails) { this.guardrails = guardrails; }
    public Long getCreatedBy() { return createdBy; }
    public void setCreatedBy(Long createdBy) { this.createdBy = createdBy; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
