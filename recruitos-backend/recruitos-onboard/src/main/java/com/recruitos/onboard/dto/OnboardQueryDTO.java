package com.recruitos.onboard.dto;

import java.io.Serializable;

/**
 * DTO for querying onboard records
 */
public class OnboardQueryDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** Filter by candidate ID */
    private Long candidateId;

    /** Filter by job ID */
    private Long jobId;

    /** Filter by onboard status */
    private String onboardStatus;

    /** Filter by HR ID */
    private Long hrId;

    /** Filter by candidate name (fuzzy) */
    private String candidateName;

    /** Page number (default 1) */
    private Integer pageNum = 1;

    /** Page size (default 10) */
    private Integer pageSize = 10;

    // Getters and Setters

    public Long getCandidateId() {
        return candidateId;
    }

    public void setCandidateId(Long candidateId) {
        this.candidateId = candidateId;
    }

    public Long getJobId() {
        return jobId;
    }

    public void setJobId(Long jobId) {
        this.jobId = jobId;
    }

    public String getOnboardStatus() {
        return onboardStatus;
    }

    public void setOnboardStatus(String onboardStatus) {
        this.onboardStatus = onboardStatus;
    }

    public Long getHrId() {
        return hrId;
    }

    public void setHrId(Long hrId) {
        this.hrId = hrId;
    }

    public String getCandidateName() {
        return candidateName;
    }

    public void setCandidateName(String candidateName) {
        this.candidateName = candidateName;
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
