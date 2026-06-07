package com.recruitos.candidate.entity;

import com.baomidou.mybatisplus.annotation.*;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Resume entity mapped to resume table.
 * Note: tenant_id is NOT auto-filled for resume; it is set explicitly.
 */
@TableName("resume")
public class Resume implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    /** Tenant ID (set explicitly, not auto-filled) */
    @TableField("tenant_id")
    private Long tenantId;

    /** Associated candidate ID */
    @TableField("candidate_id")
    private Long candidateId;

    /** Original file name */
    @TableField("file_name")
    private String fileName;

    /** File storage URL */
    @TableField("file_url")
    private String fileUrl;

    /** File type (pdf/doc/docx) */
    @TableField("file_type")
    private String fileType;

    /** Parsed structured JSON */
    @TableField("parsed_json")
    private String parsedJson;

    /** Raw extracted text */
    @TableField("raw_text")
    private String rawText;

    /** Parse status: PENDING/PARSING/SUCCESS/FAILED */
    @TableField("parse_status")
    private String parseStatus;

    /** Resume version number */
    @TableField("version")
    private Integer version;

    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    // Getters and Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getTenantId() {
        return tenantId;
    }

    public void setTenantId(Long tenantId) {
        this.tenantId = tenantId;
    }

    public Long getCandidateId() {
        return candidateId;
    }

    public void setCandidateId(Long candidateId) {
        this.candidateId = candidateId;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getFileUrl() {
        return fileUrl;
    }

    public void setFileUrl(String fileUrl) {
        this.fileUrl = fileUrl;
    }

    public String getFileType() {
        return fileType;
    }

    public void setFileType(String fileType) {
        this.fileType = fileType;
    }

    public String getParsedJson() {
        return parsedJson;
    }

    public void setParsedJson(String parsedJson) {
        this.parsedJson = parsedJson;
    }

    public String getRawText() {
        return rawText;
    }

    public void setRawText(String rawText) {
        this.rawText = rawText;
    }

    public String getParseStatus() {
        return parseStatus;
    }

    public void setParseStatus(String parseStatus) {
        this.parseStatus = parseStatus;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
