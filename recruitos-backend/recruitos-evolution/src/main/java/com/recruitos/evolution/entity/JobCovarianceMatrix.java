package com.recruitos.evolution.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.recruitos.common.mybatis.BaseEntity;

/**
 * Job covariance matrix entity - stores covariance matrices for job weight calculations
 */
@TableName("job_covariance_matrix")
public class JobCovarianceMatrix extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /** Job ID */
    private Long jobId;

    /** Matrix data as JSON string */
    private String matrixData;

    /** Matrix dimension */
    private Integer dimension;

    /** Version number */
    private Integer version;

    // Getters and Setters

    public Long getJobId() {
        return jobId;
    }

    public void setJobId(Long jobId) {
        this.jobId = jobId;
    }

    public String getMatrixData() {
        return matrixData;
    }

    public void setMatrixData(String matrixData) {
        this.matrixData = matrixData;
    }

    public Integer getDimension() {
        return dimension;
    }

    public void setDimension(Integer dimension) {
        this.dimension = dimension;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }
}
