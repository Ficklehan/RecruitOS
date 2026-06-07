package com.recruitos.headhunter.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.recruitos.common.mybatis.BaseEntity;

import java.math.BigDecimal;

/**
 * Headhunter recommendation entity - candidate recommendations from headhunters
 */
@TableName("headhunter_recommendation")
public class HeadhunterRecommendation extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @TableField("vendor_id")
    private Long vendorId;

    @TableField("candidate_id")
    private Long candidateId;

    @TableField("job_id")
    private Long jobId;

    @TableField("status")
    private String status;

    @TableField("fee_amount")
    private BigDecimal feeAmount;

    @TableField("fee_status")
    private String feeStatus;

    // Non-DB fields for display
    @TableField(exist = false)
    private String vendorName;

    @TableField(exist = false)
    private String candidateName;

    @TableField(exist = false)
    private String jobTitle;

    // Getters and Setters

    public Long getVendorId() { return vendorId; }
    public void setVendorId(Long vendorId) { this.vendorId = vendorId; }

    public Long getCandidateId() { return candidateId; }
    public void setCandidateId(Long candidateId) { this.candidateId = candidateId; }

    public Long getJobId() { return jobId; }
    public void setJobId(Long jobId) { this.jobId = jobId; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public BigDecimal getFeeAmount() { return feeAmount; }
    public void setFeeAmount(BigDecimal feeAmount) { this.feeAmount = feeAmount; }

    public String getFeeStatus() { return feeStatus; }
    public void setFeeStatus(String feeStatus) { this.feeStatus = feeStatus; }

    public String getVendorName() { return vendorName; }
    public void setVendorName(String vendorName) { this.vendorName = vendorName; }

    public String getCandidateName() { return candidateName; }
    public void setCandidateName(String candidateName) { this.candidateName = candidateName; }

    public String getJobTitle() { return jobTitle; }
    public void setJobTitle(String jobTitle) { this.jobTitle = jobTitle; }
}
