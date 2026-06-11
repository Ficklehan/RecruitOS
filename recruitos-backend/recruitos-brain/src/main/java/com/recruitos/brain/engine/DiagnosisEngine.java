package com.recruitos.brain.engine;

import com.recruitos.brain.domain.DiagnosisResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * 主动诊断引擎 — 每日巡检所有在招岗位，识别问题并生成建议。
 *
 * 诊断类型：
 * - JOB_HEALTH: 岗位健康（信号不足/权重不稳定/匹配质量低）
 * - FUNNEL_BOTTLENECK: 漏斗瓶颈（某个阶段转化率异常低）
 * - INTERVIEWER_BIAS: 面试官偏差（评分偏离群体均值）
 * - CHANNEL_ROI: 渠道效率（某渠道投入产出比异常）
 * - CANDIDATE_EXPERIENCE: 候选人体验（周期过长/卡在某个阶段）
 */
@Component
public class DiagnosisEngine {
    private static final Logger log = LoggerFactory.getLogger(DiagnosisEngine.class);

    /**
     * 对单个岗位运行全量诊断规则。
     */
    public List<DiagnosisResult> diagnoseJob(Long tenantId, Long jobId,
                                              Map<String, Object> jobMetrics) {
        List<DiagnosisResult> results = new ArrayList<>();

        checkDataSufficiency(tenantId, jobId, jobMetrics).ifPresent(results::add);
        checkFunnelBottleneck(tenantId, jobId, jobMetrics).ifPresent(results::add);
        checkStaleCampaign(tenantId, jobId, jobMetrics).ifPresent(results::add);
        checkConversionDrop(tenantId, jobId, jobMetrics).ifPresent(results::add);

        return results;
    }

    /**
     * 检查数据充足度：信号量是否足够支撑进化。
     */
    private Optional<DiagnosisResult> checkDataSufficiency(Long tenantId, Long jobId,
                                                            Map<String, Object> metrics) {
        Long signalCount = toLong(metrics.get("signalCount"), 0L);
        if (signalCount < 10) {
            return Optional.of(DiagnosisResult.of(
                "JOB_HEALTH", "WARNING",
                "进化信号不足",
                "该岗位仅积累 " + signalCount + " 条信号，进化引擎无法可靠调整权重（需要至少50条）",
                "继续运行Campaign积累信号；可考虑降低筛选门槛以扩大样本量",
                0.85
            ));
        }
        return Optional.empty();
    }

    /**
     * 检查漏斗瓶颈：哪个阶段流失最严重。
     */
    private Optional<DiagnosisResult> checkFunnelBottleneck(Long tenantId, Long jobId,
                                                             Map<String, Object> metrics) {
        Double screenRate = toDouble(metrics.get("screenPassRate"));
        Double interviewRate = toDouble(metrics.get("interviewPassRate"));
        Double offerRate = toDouble(metrics.get("offerAcceptRate"));

        if (screenRate != null && screenRate < 0.30) {
            return Optional.of(DiagnosisResult.of(
                "FUNNEL_BOTTLENECK", "WARNING",
                "筛选通过率偏低（" + String.format("%.0f%%", screenRate * 100) + "）",
                "大量候选人在筛选阶段被淘汰。可能原因：筛选标准过严、关键词不精准、候选人来源质量低",
                "建议：1) 检查筛选规则的必备标签是否合理 2) 对比不同渠道的筛选通过率 3) 考虑放宽非核心标签",
                0.72
            ));
        }
        if (interviewRate != null && interviewRate < 0.25) {
            return Optional.of(DiagnosisResult.of(
                "FUNNEL_BOTTLENECK", "CRITICAL",
                "面试通过率严重偏低（" + String.format("%.0f%%", interviewRate * 100) + "）",
                "面试通过率过低。可能原因：筛选标准与面试标准不匹配、面试官标准过高、候选人画像偏差",
                "建议：1) 对比筛选通过和面试通过的候选人特征差异 2) 检查面试官评分分布 3) 组织校准会",
                0.78
            ));
        }
        return Optional.empty();
    }

    /**
     * 检查Campaign停滞：长期无进展。
     */
    private Optional<DiagnosisResult> checkStaleCampaign(Long tenantId, Long jobId,
                                                          Map<String, Object> metrics) {
        Boolean isStale = toBoolean(metrics.get("campaignStale"));
        Long staleDays = toLong(metrics.get("campaignStaleDays"), 0L);
        if (Boolean.TRUE.equals(isStale) && staleDays >= 7) {
            return Optional.of(DiagnosisResult.of(
                "JOB_HEALTH", "WARNING",
                "Campaign " + staleDays + " 天无新候选人纳入",
                "平台招人任务长时间无产出。可能原因：关键词搜索无结果、打招呼无回复、平台账号异常",
                "建议：1) 检查平台账号是否正常 2) 尝试调整搜索关键词 3) 考虑切换平台或渠道组合",
                0.80
            ));
        }
        return Optional.empty();
    }

    /**
     * 检查转化率骤降：对比历史趋势。
     */
    private Optional<DiagnosisResult> checkConversionDrop(Long tenantId, Long jobId,
                                                           Map<String, Object> metrics) {
        Double currentRate = toDouble(metrics.get("greetReplyRate"));
        Double prevRate = toDouble(metrics.get("prevGreetReplyRate"));
        if (currentRate != null && prevRate != null && prevRate > 0 && currentRate / prevRate < 0.5) {
            return Optional.of(DiagnosisResult.of(
                "FUNNEL_BOTTLENECK", "WARNING",
                "打招呼回复率骤降：从 " + String.format("%.0f%%", prevRate * 100)
                    + " → " + String.format("%.0f%%", currentRate * 100),
                "回复率相比上一周期下降超过50%。可能原因：话术疲劳、搜索人群质量变化、平台算法调整",
                "建议：1) 更新打招呼话术 2) 检查最近调整过的搜索关键词 3) 对比同期其他岗位回复率",
                0.65
            ));
        }
        return Optional.empty();
    }

    /**
     * 对面试官进行偏差诊断。
     */
    public Optional<DiagnosisResult> diagnoseInterviewer(Long tenantId, Long interviewerId,
                                                          Map<String, Object> metrics) {
        Double leniency = toDouble(metrics.get("leniencyIndex"));
        Double avgScore = toDouble(metrics.get("avgScore"));
        Double globalAvg = toDouble(metrics.get("globalAvgScore"));
        Long evalCount = toLong(metrics.get("totalEvaluations"), 0L);

        if (evalCount < 5) {
            return Optional.empty(); // 样本不足
        }

        if (leniency != null && Math.abs(leniency - 1.0) > 0.25) {
            String bias = leniency > 1.0 ? "偏松" : "偏严";
            return Optional.of(DiagnosisResult.of(
                "INTERVIEWER_BIAS", "INFO",
                "面试官评分" + bias + "（宽松指数 " + String.format("%.2f", leniency)
                    + "，均值 " + String.format("%.2f", avgScore) + " vs 全局 " + String.format("%.2f", globalAvg) + "）",
                "该面试官评分系统性偏离群体均值。建议在下次校准会上关注其评分标准",
                "建议：1) 在校准会上讨论评分差异 2) 安排旁听校准 3) 检查其评分的具体维度分布",
                0.70
            ));
        }
        return Optional.empty();
    }

    // --- helpers ---
    private Double toDouble(Object v) {
        if (v instanceof Number) return ((Number) v).doubleValue();
        return null;
    }

    private Long toLong(Object v, Long def) {
        if (v instanceof Number) return ((Number) v).longValue();
        return def;
    }

    private Boolean toBoolean(Object v) {
        if (v instanceof Boolean) return (Boolean) v;
        return false;
    }
}
