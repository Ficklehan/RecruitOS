package com.recruitos.evolution.dto;

import java.io.Serializable;

public class ProposalQueryDTO implements Serializable {

    private Long jobId;
    private String status;
    private Integer pageNum = 1;
    private Integer pageSize = 10;

    public Long getJobId() { return jobId; }
    public void setJobId(Long jobId) { this.jobId = jobId; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Integer getPageNum() { return pageNum; }
    public void setPageNum(Integer pageNum) { this.pageNum = pageNum; }
    public Integer getPageSize() { return pageSize; }
    public void setPageSize(Integer pageSize) { this.pageSize = pageSize; }
}
