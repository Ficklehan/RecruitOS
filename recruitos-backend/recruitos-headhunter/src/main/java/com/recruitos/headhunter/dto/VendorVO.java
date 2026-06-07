package com.recruitos.headhunter.dto;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Value Object for headhunter vendor detail response
 */
public class VendorVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private Long tenantId;
    private String vendorName;
    private String contactPerson;
    private String contactPhone;
    private String contactEmail;
    private LocalDate contractStart;
    private LocalDate contractEnd;
    private Double commissionRate;
    private String status;
    private Integer totalRecommendations;
    private Integer successfulHires;
    private String remark;
    private Long createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

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

    public String getVendorName() {
        return vendorName;
    }

    public void setVendorName(String vendorName) {
        this.vendorName = vendorName;
    }

    public String getContactPerson() {
        return contactPerson;
    }

    public void setContactPerson(String contactPerson) {
        this.contactPerson = contactPerson;
    }

    public String getContactPhone() {
        return contactPhone;
    }

    public void setContactPhone(String contactPhone) {
        this.contactPhone = contactPhone;
    }

    public String getContactEmail() {
        return contactEmail;
    }

    public void setContactEmail(String contactEmail) {
        this.contactEmail = contactEmail;
    }

    public LocalDate getContractStart() {
        return contractStart;
    }

    public void setContractStart(LocalDate contractStart) {
        this.contractStart = contractStart;
    }

    public LocalDate getContractEnd() {
        return contractEnd;
    }

    public void setContractEnd(LocalDate contractEnd) {
        this.contractEnd = contractEnd;
    }

    public Double getCommissionRate() {
        return commissionRate;
    }

    public void setCommissionRate(Double commissionRate) {
        this.commissionRate = commissionRate;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getTotalRecommendations() {
        return totalRecommendations;
    }

    public void setTotalRecommendations(Integer totalRecommendations) {
        this.totalRecommendations = totalRecommendations;
    }

    public Integer getSuccessfulHires() {
        return successfulHires;
    }

    public void setSuccessfulHires(Integer successfulHires) {
        this.successfulHires = successfulHires;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public Long getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(Long createdBy) {
        this.createdBy = createdBy;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
