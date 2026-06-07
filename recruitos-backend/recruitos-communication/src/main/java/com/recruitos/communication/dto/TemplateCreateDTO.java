package com.recruitos.communication.dto;

import java.io.Serializable;

/**
 * DTO for creating or updating a message template
 */
public class TemplateCreateDTO implements Serializable {

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
}
