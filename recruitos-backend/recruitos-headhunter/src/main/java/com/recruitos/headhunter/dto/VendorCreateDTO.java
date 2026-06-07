package com.recruitos.headhunter.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.time.LocalDate;

/**
 * DTO for creating a headhunter vendor
 */
public class VendorCreateDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotBlank(message = "Vendor name is required")
    private String vendorName;

    @NotBlank(message = "Contact person is required")
    private String contactPerson;

    private String contactPhone;

    private String contactEmail;

    @NotNull(message = "Contract start date is required")
    private LocalDate contractStart;

    @NotNull(message = "Contract end date is required")
    private LocalDate contractEnd;

    @NotNull(message = "Commission rate is required")
    private Double commissionRate;

    private String remark;

    // Getters and Setters

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

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }
}
