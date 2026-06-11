package com.recruitos.brain.engine;

import com.recruitos.brain.mapper.BrainMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Offer 结果 → 意向模型反馈回路 (T3.4)。
 * 
 * <p>当 Offer 被接受或拒绝时，将实际结果回流到意向预测模型：
 * - Offer 接受 → label=1 (正样本)
 * - Offer 拒绝 → label=0 (负样本)
 * - 回流的数据作为后续模型重训练的标注样本
 */
@Service
public class OfferFeedbackService {
    private static final Logger log = LoggerFactory.getLogger(OfferFeedbackService.class);

    @Resource private BrainMapper brainMapper;

    /**
     * Offer 结果回流。
     * 在 Offer 接受/拒绝后调用，将实际结果与之前的意向预测关联。
     * 
     * @param candidateId 候选人ID
     * @param jobId 岗位ID
     * @param accepted Offer是否被接受
     */
    @Transactional
    public void onOfferDecision(Long candidateId, Long jobId, boolean accepted) {
        log.info("Offer feedback: candidate={}, job={}, accepted={}", candidateId, jobId, accepted);

        try {
            // 1. 查询之前的意向预测
            Map<String, Object> lastIntent = brainMapper.findLatestIntent(candidateId, jobId);
            if (lastIntent == null || lastIntent.isEmpty()) {
                log.info("No prior intent prediction found for candidate={}, job={}, skipping feedback", candidateId, jobId);
                return;
            }

            // 2. 回流标签
            Map<String, Object> feedback = new LinkedHashMap<>();
            feedback.put("id", brainMapper.nextId("ai_decision_log"));
            feedback.put("tenantId", 0L);
            feedback.put("decisionType", "OFFER_OUTCOME");
            feedback.put("targetId", candidateId);
            feedback.put("targetType", "CANDIDATE");
            feedback.put("decisionDetail", buildFeedbackDetail(lastIntent, accepted));
            feedback.put("confidence", 1.0);
            feedback.put("autoExecuted", false);
            feedback.put("humanConfirmed", true);
            feedback.put("confirmedBy", null);

            brainMapper.insertDecisionLog(feedback);

            // 3. 更新意向预测表中的实际结果
            brainMapper.updateIntentOutcome(
                (Long) lastIntent.get("id"),
                accepted ? "ACCEPTED" : "REJECTED"
            );

            log.info("Offer feedback recorded: intent_id={}, outcome={}",
                lastIntent.get("id"), accepted ? "ACCEPTED" : "REJECTED");

        } catch (Exception e) {
            log.error("Failed to record offer feedback for candidate={}, job={}", candidateId, jobId, e);
        }
    }

    /**
     * 获取模型反馈统计。
     * @return { totalFeedback, acceptedCount, rejectedCount, predictionAccuracy }
     */
    public Map<String, Object> getFeedbackStats() {
        Map<String, Object> stats = new LinkedHashMap<>();
        try {
            int accepted = brainMapper.countIntentOutcomes("ACCEPTED");
            int rejected = brainMapper.countIntentOutcomes("REJECTED");
            int total = accepted + rejected;

            stats.put("totalFeedback", total);
            stats.put("acceptedCount", accepted);
            stats.put("rejectedCount", rejected);

            // 计算意向预测准确度：HIGH意向→接受 的比例
            int highIntentAccepted = brainMapper.countHighIntentAccepted();
            int totalHighIntent = brainMapper.countHighIntent();
            double accuracy = totalHighIntent > 0
                ? (double) highIntentAccepted / totalHighIntent : 0;
            stats.put("predictionAccuracy", Math.round(accuracy * 10000.0) / 10000.0);

        } catch (Exception e) {
            stats.put("totalFeedback", 0);
            stats.put("error", e.getMessage());
        }
        return stats;
    }

    private String buildFeedbackDetail(Map<String, Object> lastIntent, boolean accepted) {
        try {
            Map<String, Object> detail = new LinkedHashMap<>();
            detail.put("intentId", lastIntent.get("id"));
            detail.put("predictedScore", lastIntent.get("intentScore"));
            detail.put("predictedLevel", lastIntent.get("intentLevel"));
            detail.put("actualOutcome", accepted ? "ACCEPTED" : "REJECTED");
            detail.put("feedbackTime", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

            // 判断预测是否正确
            String level = (String) lastIntent.get("intentLevel");
            boolean predictedCorrectly = ("HIGH".equals(level) && accepted) || ("LOW".equals(level) && !accepted);
            detail.put("predictionCorrect", predictedCorrectly);

            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            return mapper.writeValueAsString(detail);
        } catch (Exception e) {
            return "{}";
        }
    }
}
