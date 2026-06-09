package com.recruitos.evolution.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.recruitos.common.mybatis.BaseEntity;

import java.math.BigDecimal;

@TableName("job_weight_snapshot")
public class JobWeightSnapshot extends BaseEntity {

    private static final long serialVersionUID = 1L;

    private Long jobId;
    private String snapshotType;
    private String tagsSnapshot;
    private BigDecimal healthScore;
    private Long signalId;

    public Long getJobId() {
        return jobId;
    }

    public void setJobId(Long jobId) {
        this.jobId = jobId;
    }

    public String getSnapshotType() {
        return snapshotType;
    }

    public void setSnapshotType(String snapshotType) {
        this.snapshotType = snapshotType;
    }

    public String getTagsSnapshot() {
        return tagsSnapshot;
    }

    public void setTagsSnapshot(String tagsSnapshot) {
        this.tagsSnapshot = tagsSnapshot;
    }

    public BigDecimal getHealthScore() {
        return healthScore;
    }

    public void setHealthScore(BigDecimal healthScore) {
        this.healthScore = healthScore;
    }

    public Long getSignalId() {
        return signalId;
    }

    public void setSignalId(Long signalId) {
        this.signalId = signalId;
    }
}
