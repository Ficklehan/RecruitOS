package com.recruitos.headhunter.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.io.Serializable;

/**
 * DTO for creating a headhunter recommendation
 */
public class RecommendationCreateDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotNull(message = "Vendor ID is required")
    private Long vendorId;

    @NotBlank(message = "Vendor name is required")
    private String vendorName;

    @NotNull(message = "Candidate ID is required")
    private Long candidateId;

    @NotBlank(message = "Candidate name is required")
    private String candidateName;

    @NotNull(message = "Job ID is required")
    private Long jobId;

    @NotBlank(message = "Job title is required")
    private String jobTitle;

    private Double commissionAmount;

    private String remark;

    // Getters and Setters

    public Long getVendorId() {
        return vendorId;
    }

    public void setVendorId(Long vendorId) {
        this.vendorId = vendorId;
    }

    public String getVendorName() {
        return vendorName;
    }

    public void setVendorName(String vendorName) {
        this.vendorName = vendorName;
    }

    public Long getCandidateId() {
        return candidateId;
    }

    public void setCandidateId(Long candidateId) {
        this.candidateId = candidateId;
    }

    public String getCandidateName() {
        return candidateName;
    }

    public void setCandidateName(String candidateName) {
        this.candidateName = candidateName;
    }

    public Long getJobId() {
        return jobId;
    }

    public void setJobId(Long jobId) {
        this.jobId = jobId;
    }

    public String getJobTitle() {
        return jobTitle;
    }

    public void setJobTitle(String jobTitle) {
        this.jobTitle = jobTitle;
    }

    public Double getCommissionAmount() {
        return commissionAmount;
    }

    public void setCommissionAmount(Double commissionAmount) {
        this.commissionAmount = commissionAmount;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }
}
