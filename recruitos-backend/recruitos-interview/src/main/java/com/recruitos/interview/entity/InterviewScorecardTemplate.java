package com.recruitos.interview.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.recruitos.common.mybatis.BaseEntity;

/**
 * 结构化面试评分卡模板 — 替代旧InterviewEvaluation的裸JSON维度。
 */
@TableName("interview_scorecard_template")
public class InterviewScorecardTemplate extends BaseEntity {
    private Long tenantId;
    private String jobFamily;        // BACKEND/FRONTEND/PM/DESIGN
    private String levelRange;       // P5-P6/P7-P8/P9+
    private String dimensions;       // JSON: [{name, weight, behavioral_anchors:{1,3,5}, suggested_questions:[...]}]
    private Integer version;
    private Boolean isActive;
    private Long createdBy;

    public Long getTenantId() { return tenantId; }
    public void setTenantId(Long tenantId) { this.tenantId = tenantId; }
    public String getJobFamily() { return jobFamily; }
    public void setJobFamily(String jobFamily) { this.jobFamily = jobFamily; }
    public String getLevelRange() { return levelRange; }
    public void setLevelRange(String levelRange) { this.levelRange = levelRange; }
    public String getDimensions() { return dimensions; }
    public void setDimensions(String dimensions) { this.dimensions = dimensions; }
    public Integer getVersion() { return version; }
    public void setVersion(Integer version) { this.version = version; }
    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }
    public Long getCreatedBy() { return createdBy; }
    public void setCreatedBy(Long createdBy) { this.createdBy = createdBy; }
}
