package com.recruitos.analytics.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.recruitos.common.mybatis.BaseEntity;

import java.time.LocalDate;

/**
 * Analytics snapshot entity - for caching analytics data
 */
@TableName("analytics_snapshot")
public class AnalyticsSnapshot extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /** Snapshot type: FUNNEL/ROI/INTERVIEWER/CYCLE */
    private String snapshotType;

    /** Snapshot date */
    private LocalDate snapshotDate;

    /** Data JSON blob */
    private String dataJson;

    // Getters and Setters

    public String getSnapshotType() {
        return snapshotType;
    }

    public void setSnapshotType(String snapshotType) {
        this.snapshotType = snapshotType;
    }

    public LocalDate getSnapshotDate() {
        return snapshotDate;
    }

    public void setSnapshotDate(LocalDate snapshotDate) {
        this.snapshotDate = snapshotDate;
    }

    public String getDataJson() {
        return dataJson;
    }

    public void setDataJson(String dataJson) {
        this.dataJson = dataJson;
    }
}
