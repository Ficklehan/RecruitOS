package com.recruitos.offer.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.recruitos.common.mybatis.BaseEntity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Offer entity
 */
@TableName("offer")
public class Offer extends BaseEntity {

    private static final long serialVersionUID = 1L;

    private Long candidateId;
    private String candidateName;
    private Long jobId;
    private String jobTitle;
    private String department;
    private Long interviewId;
    private BigDecimal salary;
    private BigDecimal bonus;
    private String equity;
    private String level;
    private LocalDate onboardDate;
    private Long offerTemplateId;
    private String status;
    private Long approverId;
    private LocalDateTime approvedAt;
    private String eSignUrl;
    private String eSignStatus;
    private String bgCheckStatus;
    private String rejectReason;
    private LocalDateTime sentAt;
    private LocalDateTime acceptedAt;
    private String remark;
    private Long createdBy;

    // Getters and Setters

    public Long getCandidateId() { return candidateId; }
    public void setCandidateId(Long candidateId) { this.candidateId = candidateId; }

    public Long getJobId() { return jobId; }
    public void setJobId(Long jobId) { this.jobId = jobId; }

    public Long getInterviewId() { return interviewId; }
    public void setInterviewId(Long interviewId) { this.interviewId = interviewId; }

    public BigDecimal getSalary() { return salary; }
    public void setSalary(BigDecimal salary) { this.salary = salary; }

    public BigDecimal getBonus() { return bonus; }
    public void setBonus(BigDecimal bonus) { this.bonus = bonus; }

    public String getEquity() { return equity; }
    public void setEquity(String equity) { this.equity = equity; }

    public String getLevel() { return level; }
    public void setLevel(String level) { this.level = level; }

    public LocalDate getOnboardDate() { return onboardDate; }
    public void setOnboardDate(LocalDate onboardDate) { this.onboardDate = onboardDate; }

    public Long getOfferTemplateId() { return offerTemplateId; }
    public void setOfferTemplateId(Long offerTemplateId) { this.offerTemplateId = offerTemplateId; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getESignUrl() { return eSignUrl; }
    public void setESignUrl(String eSignUrl) { this.eSignUrl = eSignUrl; }

    public String getESignStatus() { return eSignStatus; }
    public void setESignStatus(String eSignStatus) { this.eSignStatus = eSignStatus; }

    public String getBgCheckStatus() { return bgCheckStatus; }
    public void setBgCheckStatus(String bgCheckStatus) { this.bgCheckStatus = bgCheckStatus; }

    public String getRejectReason() { return rejectReason; }
    public void setRejectReason(String rejectReason) { this.rejectReason = rejectReason; }

    public LocalDateTime getSentAt() { return sentAt; }
    public void setSentAt(LocalDateTime sentAt) { this.sentAt = sentAt; }

    public LocalDateTime getAcceptedAt() { return acceptedAt; }
    public void setAcceptedAt(LocalDateTime acceptedAt) { this.acceptedAt = acceptedAt; }

    public Long getCreatedBy() { return createdBy; }
    public void setCreatedBy(Long createdBy) { this.createdBy = createdBy; }

    public String getCandidateName() { return candidateName; }
    public void setCandidateName(String candidateName) { this.candidateName = candidateName; }

    public String getJobTitle() { return jobTitle; }
    public void setJobTitle(String jobTitle) { this.jobTitle = jobTitle; }

    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }

    public Long getApproverId() { return approverId; }
    public void setApproverId(Long approverId) { this.approverId = approverId; }

    public LocalDateTime getApprovedAt() { return approvedAt; }
    public void setApprovedAt(LocalDateTime approvedAt) { this.approvedAt = approvedAt; }

    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
}
