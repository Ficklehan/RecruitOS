package com.recruitos.communication.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.recruitos.common.mybatis.BaseEntity;

import java.math.BigDecimal;

/**
 * Message template entity
 */
@TableName("message_template")
public class MessageTemplate extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /** Template name */
    private String templateName;

    /** Template type: SMS / EMAIL / WECHAT / FEISHU */
    private String templateType;

    /** Template content */
    private String content;

    /** Variables - JSON array of variable names */
    private String variables;

    /** Status: ACTIVE / DISABLED */
    private String status;

    /** A/B test group variant label */
    private String abTestGroup;

    /** Usage count */
    private Integer usageCount;

    /** Success rate */
    private BigDecimal successRate;

    /** Created by user ID */
    private Long createdBy;

    // Getters and Setters

    public String getTemplateName() {
        return templateName;
    }

    public void setTemplateName(String templateName) {
        this.templateName = templateName;
    }

    public String getTemplateType() {
        return templateType;
    }

    public void setTemplateType(String templateType) {
        this.templateType = templateType;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getVariables() {
        return variables;
    }

    public void setVariables(String variables) {
        this.variables = variables;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getAbTestGroup() {
        return abTestGroup;
    }

    public void setAbTestGroup(String abTestGroup) {
        this.abTestGroup = abTestGroup;
    }

    public Integer getUsageCount() {
        return usageCount;
    }

    public void setUsageCount(Integer usageCount) {
        this.usageCount = usageCount;
    }

    public BigDecimal getSuccessRate() {
        return successRate;
    }

    public void setSuccessRate(BigDecimal successRate) {
        this.successRate = successRate;
    }

    public Long getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(Long createdBy) {
        this.createdBy = createdBy;
    }
}
