package com.recruitos.offer.dto;

import java.io.Serializable;

/**
 * DTO for creating an offer
 */
public class OfferCreateDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** Candidate ID */
    private Long candidateId;

    /** Candidate name */
    private String candidateName;

    /** Job ID */
    private Long jobId;

    /** Job title */
    private String jobTitle;

    /** Department */
    private String department;

    /** Salary */
    private String salary;

    /** Remark */
    private String remark;

    // Getters and Setters

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

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public String getSalary() {
        return salary;
    }

    public void setSalary(String salary) {
        this.salary = salary;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }
}
