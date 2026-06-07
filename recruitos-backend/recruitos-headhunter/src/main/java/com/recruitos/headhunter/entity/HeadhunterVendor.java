package com.recruitos.headhunter.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.recruitos.common.mybatis.BaseEntity;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Headhunter vendor entity - external headhunter agency records
 */
@TableName("headhunter_vendor")
public class HeadhunterVendor extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @TableField("name")
    private String vendorName;

    @TableField("contact_name")
    private String contactPerson;

    @TableField("contact_phone")
    private String contactPhone;

    @TableField("contact_email")
    private String contactEmail;

    @TableField("fee_rate")
    private BigDecimal commissionRate;

    @TableField("service_scope")
    private String serviceScope;

    @TableField("contract_start")
    private LocalDate contractStart;

    @TableField("contract_end")
    private LocalDate contractEnd;

    @TableField("total_recommended")
    private Integer totalRecommendations;

    @TableField("total_hired")
    private Integer successfulHires;

    @TableField("status")
    private Integer status;

    // Getters and Setters

    public String getVendorName() { return vendorName; }
    public void setVendorName(String vendorName) { this.vendorName = vendorName; }

    public String getContactPerson() { return contactPerson; }
    public void setContactPerson(String contactPerson) { this.contactPerson = contactPerson; }

    public String getContactPhone() { return contactPhone; }
    public void setContactPhone(String contactPhone) { this.contactPhone = contactPhone; }

    public String getContactEmail() { return contactEmail; }
    public void setContactEmail(String contactEmail) { this.contactEmail = contactEmail; }

    public BigDecimal getCommissionRate() { return commissionRate; }
    public void setCommissionRate(BigDecimal commissionRate) { this.commissionRate = commissionRate; }

    public String getServiceScope() { return serviceScope; }
    public void setServiceScope(String serviceScope) { this.serviceScope = serviceScope; }

    public LocalDate getContractStart() { return contractStart; }
    public void setContractStart(LocalDate contractStart) { this.contractStart = contractStart; }

    public LocalDate getContractEnd() { return contractEnd; }
    public void setContractEnd(LocalDate contractEnd) { this.contractEnd = contractEnd; }

    public Integer getTotalRecommendations() { return totalRecommendations; }
    public void setTotalRecommendations(Integer totalRecommendations) { this.totalRecommendations = totalRecommendations; }

    public Integer getSuccessfulHires() { return successfulHires; }
    public void setSuccessfulHires(Integer successfulHires) { this.successfulHires = successfulHires; }

    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
}
