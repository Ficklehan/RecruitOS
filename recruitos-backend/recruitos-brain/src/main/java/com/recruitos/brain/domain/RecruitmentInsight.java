package com.recruitos.brain.domain;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * AI洞察 — 推送到首页的主动建议卡片。
 */
public class RecruitmentInsight implements Serializable {
    private static final long serialVersionUID = 1L;

    private String id;
    private Long tenantId;
    private String category;         // CANDIDATE_QUALITY / PROCESS_EFFICIENCY / INTERVIEWER_PERFORMANCE / CHANNEL_ROI
    private String title;
    private String description;
    private String suggestedAction;
    private String actionPath;       // 前端路由
    private Double confidence;
    private Map<String, Object> data; // 支撑数据
    private LocalDateTime generatedAt;

    public static RecruitmentInsight of(String category, String title, String description,
                                         String suggestedAction, Double confidence) {
        RecruitmentInsight i = new RecruitmentInsight();
        i.category = category;
        i.title = title;
        i.description = description;
        i.suggestedAction = suggestedAction;
        i.confidence = confidence;
        i.generatedAt = LocalDateTime.now();
        return i;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public Long getTenantId() { return tenantId; }
    public void setTenantId(Long tenantId) { this.tenantId = tenantId; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getSuggestedAction() { return suggestedAction; }
    public void setSuggestedAction(String suggestedAction) { this.suggestedAction = suggestedAction; }
    public String getActionPath() { return actionPath; }
    public void setActionPath(String actionPath) { this.actionPath = actionPath; }
    public Double getConfidence() { return confidence; }
    public void setConfidence(Double confidence) { this.confidence = confidence; }
    public Map<String, Object> getData() { return data; }
    public void setData(Map<String, Object> data) { this.data = data; }
    public LocalDateTime getGeneratedAt() { return generatedAt; }
    public void setGeneratedAt(LocalDateTime generatedAt) { this.generatedAt = generatedAt; }
}
