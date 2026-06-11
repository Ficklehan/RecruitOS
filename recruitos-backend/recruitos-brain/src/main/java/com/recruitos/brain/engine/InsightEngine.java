package com.recruitos.brain.engine;

import com.recruitos.brain.domain.RecruitmentInsight;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * 洞察生成引擎 — 生成推送到AI助手首页的主动建议卡片。
 */
@Component
public class InsightEngine {
    private static final Logger log = LoggerFactory.getLogger(InsightEngine.class);

    /**
     * 为指定租户生成今日洞察列表（最多maxCount条）。
     */
    public List<RecruitmentInsight> generateDailyInsights(Long tenantId,
                                                           Map<String, Object> tenantMetrics,
                                                           int maxCount) {
        List<RecruitmentInsight> insights = new ArrayList<>();

        checkHighPriorityActions(tenantId, tenantMetrics).ifPresent(insights::add);
        checkTrendAlert(tenantId, tenantMetrics).ifPresent(insights::add);
        checkOpportunity(tenantId, tenantMetrics).ifPresent(insights::add);
        checkTeamHealth(tenantId, tenantMetrics).ifPresent(insights::add);

        // 按置信度排序，截取top N
        insights.sort((a, b) -> Double.compare(
            b.getConfidence() != null ? b.getConfidence() : 0,
            a.getConfidence() != null ? a.getConfidence() : 0));
        return insights.size() > maxCount ? insights.subList(0, maxCount) : insights;
    }

    private Optional<RecruitmentInsight> checkHighPriorityActions(Long tenantId,
                                                                   Map<String, Object> metrics) {
        Long pendingConfirm = toLong(metrics.get("pendingConfirmCount"), 0L);
        Long pendingFeedback = toLong(metrics.get("pendingFeedbackCount"), 0L);
        Long pendingApproval = toLong(metrics.get("pendingApprovalCount"), 0L);

        if (pendingConfirm + pendingFeedback + pendingApproval >= 3) {
            return Optional.of(RecruitmentInsight.of(
                "PROCESS_EFFICIENCY",
                pendingConfirm + pendingFeedback + pendingApproval + " 项待你处理",
                "包含 " + pendingConfirm + " 个待确认联系、" + pendingFeedback
                    + " 个待提交面试反馈、" + pendingApproval + " 个待审批",
                "前往收件箱",
                0.95
            ));
        }
        return Optional.empty();
    }

    private Optional<RecruitmentInsight> checkTrendAlert(Long tenantId,
                                                          Map<String, Object> metrics) {
        Double offerAcceptRate = toDouble(metrics.get("offerAcceptRate"));
        Double prevAcceptRate = toDouble(metrics.get("prevOfferAcceptRate"));

        if (offerAcceptRate != null && prevAcceptRate != null
                && prevAcceptRate > 0 && offerAcceptRate / prevAcceptRate < 0.7) {
            return Optional.of(RecruitmentInsight.of(
                "CANDIDATE_QUALITY",
                "Offer接受率下降",
                "最近Offer接受率 " + String.format("%.0f%%", offerAcceptRate * 100)
                    + "（上期 " + String.format("%.0f%%", prevAcceptRate * 100) + "），下降超过30%",
                "查看拒绝原因分析",
                0.70
            ));
        }
        return Optional.empty();
    }

    private Optional<RecruitmentInsight> checkOpportunity(Long tenantId,
                                                           Map<String, Object> metrics) {
        Long talentPoolSize = toLong(metrics.get("talentPoolSize"), 0L);
        Long matchedCandidates = toLong(metrics.get("matchedTalentCount"), 0L);

        if (talentPoolSize > 0 && matchedCandidates > 0
                && talentPoolSize > matchedCandidates * 3) {
            return Optional.of(RecruitmentInsight.of(
                "CANDIDATE_QUALITY",
                "人才库中 " + matchedCandidates + " 人可能匹配在招岗位",
                "人才库共 " + talentPoolSize + " 人，AI检测到 " + matchedCandidates
                    + " 人可能与当前在招岗位存在匹配。建议激活存量人才",
                "查看匹配人才",
                0.60
            ));
        }
        return Optional.empty();
    }

    private Optional<RecruitmentInsight> checkTeamHealth(Long tenantId,
                                                          Map<String, Object> metrics) {
        Long criticalJobs = toLong(metrics.get("criticalJobCount"), 0L);
        Long totalJobs = toLong(metrics.get("totalJobCount"), 0L);

        if (criticalJobs > 0) {
            return Optional.of(RecruitmentInsight.of(
                "PROCESS_EFFICIENCY",
                criticalJobs + "/" + totalJobs + " 个岗位健康异常",
                "存在招聘周期过长、匹配质量下降等风险。建议优先处理",
                "查看健康诊断",
                0.82
            ));
        }
        return Optional.empty();
    }

    private Double toDouble(Object v) {
        if (v instanceof Number) return ((Number) v).doubleValue();
        return null;
    }

    private Long toLong(Object v, Long def) {
        if (v instanceof Number) return ((Number) v).longValue();
        return def;
    }
}
