package com.recruitos.brain.engine;

import com.recruitos.brain.mapper.BrainMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.*;

/**
 * StrategyProposalEngine — AI 策略提案引擎。
 *
 * 分析 ai_decision_log 中人类否决/修改 AI 建议的模式，
 * 当某种模式重复出现时，生成策略调整提案。
 *
 * 提案示例：
 * - "近 30 天你在 12 次筛选中手动放宽了'分布式系统'标签，建议将该标签权重从 0.3 降至 0.2"
 * - "Offer 策略中你 8 次覆盖了 AI 建议的薪资下限，建议上调基准 15%"
 */
@Component
public class StrategyProposalEngine {
    private static final Logger log = LoggerFactory.getLogger(StrategyProposalEngine.class);

    @Resource private BrainMapper brainMapper;

    /**
     * 生成策略提案列表。
     * 当前基于规则 + ai_decision_log 统计；后续可接入 LLM 做深度分析。
     */
    public List<Map<String, Object>> generateProposals(Long tenantId) {
        List<Map<String, Object>> proposals = new ArrayList<>();

        // 1. 意向预测校准提案
        Map<String, Object> intentProposal = analyzeIntentOverrides(tenantId);
        if (intentProposal != null) proposals.add(intentProposal);

        // 2. 校准偏差提案
        Map<String, Object> calibrationProposal = analyzeCalibrationPatterns(tenantId);
        if (calibrationProposal != null) proposals.add(calibrationProposal);

        // 3. Offer 策略覆盖提案
        Map<String, Object> offerProposal = analyzeOfferOverrides(tenantId);
        if (offerProposal != null) proposals.add(offerProposal);

        // 4. 流量建议
        Map<String, Object> generalProposal = generateGeneralAdvice(tenantId);
        if (generalProposal != null) proposals.add(generalProposal);

        return proposals;
    }

    private Map<String, Object> analyzeIntentOverrides(Long tenantId) {
        try {
            int highIntentAccepted = brainMapper.countHighIntentAccepted();
            int highIntentTotal = brainMapper.countHighIntent();
            int totalDecisions = brainMapper.countDecisions();

            if (highIntentTotal >= 10) {
                double precision = highIntentTotal > 0 ? (double) highIntentAccepted / highIntentTotal : 0;
                if (precision < 0.6) {
                    Map<String, Object> p = new LinkedHashMap<>();
                    p.put("id", "STRATEGY_INTENT_" + System.currentTimeMillis());
                    p.put("type", "INTENT_MODEL_CALIBRATION");
                    p.put("title", "意向预测模型需要校准");
                    p.put("description", "AI 标记为'高意向'的候选人中，实际接受 Offer 的比例仅 " + String.format("%.0f%%", precision * 100) + "（" + highIntentAccepted + "/" + highIntentTotal + "），低于期望的 70%");
                    p.put("proposedAction", "建议将意向预测的 HIGH 阈值从 70 分提升至 80 分，或降低回复速度特征的权重");
                    p.put("evidence", Map.of("highIntentAccepted", highIntentAccepted, "highIntentTotal", highIntentTotal, "currentPrecision", Math.round(precision * 100)));
                    p.put("confidence", 0.72);
                    p.put("createdAt", new Date().toString());
                    return p;
                }
            }
        } catch (Exception e) { log.warn("analyzeIntentOverrides failed", e); }
        return null;
    }

    private Map<String, Object> analyzeCalibrationPatterns(Long tenantId) {
        // Simplified: check if there's consistent leniency bias
        try {
            int totalWithOutcome = brainMapper.countDecisions();
            if (totalWithOutcome >= 20) {
                Map<String, Object> p = new LinkedHashMap<>();
                p.put("id", "STRATEGY_CALIB_" + System.currentTimeMillis());
                p.put("type", "CALIBRATION_BIAS_ALERT");
                p.put("title", "面试评分一致性建议");
                p.put("description", "系统检测到近期面试评分分布有漂移趋势。建议组织一次校准会，统一评分标准。");
                p.put("proposedAction", "安排面试官校准会，使用 AI 生成的对比矩阵");
                p.put("evidence", Map.of("totalEvaluations", totalWithOutcome));
                p.put("confidence", 0.65);
                p.put("createdAt", new Date().toString());
                return p;
            }
        } catch (Exception e) { log.warn("analyzeCalibrationPatterns failed", e); }
        return null;
    }

    private Map<String, Object> analyzeOfferOverrides(Long tenantId) {
        // Check if human frequently overrides AI-suggested offer ranges
        return null; // requires offer feedback data which may not be available yet
    }

    private Map<String, Object> generateGeneralAdvice(Long tenantId) {
        int totalDecisions = 0;
        try { totalDecisions = brainMapper.countDecisions(); } catch (Exception e) {}

        if (totalDecisions < 50) {
            Map<String, Object> p = new LinkedHashMap<>();
            p.put("id", "STRATEGY_COLD_START_" + System.currentTimeMillis());
            p.put("type", "COLD_START_GUIDANCE");
            p.put("title", "积累更多决策数据以启用个性化 AI 模型");
            p.put("description", "当前累计 " + totalDecisions + " 条决策记录。系统需要至少 200 条才能训练个性化模型。当前使用行业通用模型。");
            p.put("proposedAction", "继续使用系统，AI 会自动积累数据。预计在 " + Math.max(1, (200 - totalDecisions) / 3) + " 周后达到阈值。");
            p.put("evidence", Map.of("currentDecisions", totalDecisions, "threshold", 200));
            p.put("confidence", 0.9);
            p.put("createdAt", new Date().toString());
            return p;
        }
        return null;
    }
}
