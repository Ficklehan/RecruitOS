package com.recruitos.communication.dto;

import java.io.Serializable;

/**
 * DTO for querying message templates
 */
public class TemplateQueryDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** Filter by template type */
    private String templateType;

    /** Filter by status */
    private String status;

    /** Search by template name */
    private String templateName;

    /** Page number (default 1) */
    private Integer pageNum = 1;

    /** Page size (default 10) */
    private Integer pageSize = 10;

    // Getters and Setters

    public String getTemplateType() {
        return templateType;
    }

    public void setTemplateType(String templateType) {
        this.templateType = templateType;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getTemplateName() {
        return templateName;
    }

    public void setTemplateName(String templateName) {
        this.templateName = templateName;
    }

    public Integer getPageNum() {
        return pageNum;
    }

    public void setPageNum(Integer pageNum) {
        this.pageNum = pageNum;
    }

    public Integer getPageSize() {
        return pageSize;
    }

    public void setPageSize(Integer pageSize) {
        this.pageSize = pageSize;
    }
}
