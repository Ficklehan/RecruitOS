package com.recruitos.brain.controller;

import com.recruitos.brain.TenantResolver;
import com.recruitos.brain.service.FeishuIntegrationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.*;

/**
 * 飞书/企微集成配置 & 消息发送 API。
 */
@RestController
@RequestMapping("/api/integration")
public class IntegrationController {

    private static final Logger log = LoggerFactory.getLogger(IntegrationController.class);

    @Resource
    private FeishuIntegrationService feishuService;

    @Resource
    private TenantResolver tenantResolver;

    // ─────────────── 配置 ───────────────

    @GetMapping("/configs")
    public List<Map<String, Object>> getConfigs() {
        return feishuService.getConfigs(currentTenantId());
    }

    @PostMapping("/configs")
    public Map<String, Object> saveConfig(@RequestBody Map<String, Object> body) {
        Map<String, Object> result = new LinkedHashMap<>();
        try {
            String platform = (String) body.get("platform");
            String key = (String) body.get("configKey");
            String value = (String) body.get("configValue");
            feishuService.saveConfig(currentTenantId(), platform, key, value);
            result.put("status", "OK");
            result.put("message", "配置已保存");
        } catch (Exception e) {
            log.error("saveConfig failed", e);
            result.put("status", "FAILED");
            result.put("message", e.getMessage());
        }
        return result;
    }

    // ─────────────── 消息模板 ───────────────

    @GetMapping("/templates")
    public List<Map<String, Object>> getTemplates(@RequestParam(defaultValue = "FEISHU") String platform) {
        return feishuService.getTemplates(1L, platform);
    }

    // ─────────────── 消息发送（测试/手动触发） ───────────────

    @PostMapping("/send/interview-invite")
    public Map<String, Object> sendInterviewInvite(@RequestBody Map<String, Object> body) {
        return feishuService.sendInterviewInvite(
            currentTenantId(),
            (String) body.getOrDefault("candidateName", "候选人"),
            (String) body.getOrDefault("jobTitle", "未知岗位"),
            (String) body.getOrDefault("interviewTime", "2026-06-15 14:00"),
            (String) body.getOrDefault("interviewType", "视频面试"),
            (String) body.getOrDefault("interviewer", "面试官"),
            (String) body.getOrDefault("prepNotes", "请准备项目分享"),
            ((Number) body.getOrDefault("interviewId", currentTenantId())).longValue()
        );
    }

    @PostMapping("/send/offer-approval")
    public Map<String, Object> sendOfferApproval(@RequestBody Map<String, Object> body) {
        return feishuService.sendOfferApproval(
            currentTenantId(),
            (String) body.getOrDefault("candidateName", "候选人"),
            (String) body.getOrDefault("jobTitle", "岗位"),
            (String) body.getOrDefault("salaryPackage", "50-70万"),
            (String) body.getOrDefault("level", "P7"),
            (String) body.getOrDefault("marketCompetitiveness", "市场 P50"),
            (String) body.getOrDefault("attritionRisk", "中等"),
            (String) body.getOrDefault("aiAdvice", "建议增加签字费"),
            ((Number) body.getOrDefault("offerId", currentTenantId())).longValue()
        );
    }

    @PostMapping("/send/pipeline-alert")
    public Map<String, Object> sendPipelineAlert(@RequestBody Map<String, Object> body) {
        return feishuService.sendPipelineAlert(
            currentTenantId(),
            (String) body.getOrDefault("jobTitle", "岗位"),
            ((Number) body.getOrDefault("totalCandidates", 3)).intValue(),
            ((Number) body.getOrDefault("daysSinceLast", 7)).intValue(),
            ((Number) body.getOrDefault("daysOpen", 30)).intValue(),
            (String) body.getOrDefault("aiAdvice", "建议增加寻源渠道"),
            ((Number) body.getOrDefault("jobId", currentTenantId())).longValue()
        );
    }

    @PostMapping("/send/decision-reminder")
    public Map<String, Object> sendDecisionReminder(@RequestBody Map<String, Object> body) {
        return feishuService.sendDecisionReminder(
            currentTenantId(),
            (String) body.getOrDefault("candidateName", "候选人"),
            (String) body.getOrDefault("jobTitle", "岗位"),
            (String) body.getOrDefault("currentStage", "终面"),
            ((Number) body.getOrDefault("daysInStage", 5)).intValue(),
            (String) body.getOrDefault("aiObservation", "该候选人在终面阶段停留 5 天，建议尽快完成决策"),
            ((Number) body.getOrDefault("candidateId", currentTenantId())).longValue()
        );
    }

    // ─────────────── 飞书 Webhook 事件接收 ───────────────

    @PostMapping("/feishu/events")
    public Map<String, Object> receiveFeishuEvents(@RequestBody Map<String, Object> body) {
        // 飞书事件回调处理
        String type = (String) body.getOrDefault("type", "unknown");
        Map<String, Object> result = new LinkedHashMap<>();

        // URL 验证
        if ("url_verification".equals(type)) {
            String challenge = (String) body.get("challenge");
            result.put("challenge", challenge);
            return result;
        }

        // 事件处理
        if ("event_callback".equals(type)) {
            Map<String, Object> event = (Map<String, Object>) body.getOrDefault("event", Map.of());
            String eventType = (String) event.getOrDefault("type", "");

            log.info("Feishu event received: type={}, eventType={}", type, eventType);

            switch (eventType) {
                case "card.action.trigger":
                    handleCardAction(event);
                    break;
                case "im.message.receive_v1":
                    handleBotMessage(event);
                    break;
                default:
                    log.info("Unhandled Feishu event: {}", eventType);
            }
        }

        result.put("code", 0);
        return result;
    }

    private void handleCardAction(Map<String, Object> event) {
        try {
            Map<String, Object> action = (Map<String, Object>) event.getOrDefault("action", Map.of());
            String value = (String) action.getOrDefault("value", "{}");
            // 解析 JSON value 获取 action 类型
            Map<String, Object> actionData = new com.fasterxml.jackson.databind.ObjectMapper()
                .readValue(value, Map.class);
            String actionType = (String) actionData.getOrDefault("action", "unknown");

            log.info("Card action: {} with data {}", actionType, actionData);

            switch (actionType) {
                case "confirm":
                    // 候选人确认面试时间
                    Long interviewId = toLong(actionData.get("interviewId"));
                    log.info("Candidate confirmed interview: {}", interviewId);
                    break;
                case "approve":
                    // 批准 Offer
                    Long offerId = toLong(actionData.get("offerId"));
                    log.info("Offer approved via Feishu: {}", offerId);
                    break;
                case "reject":
                    // 驳回 Offer
                    Long rejectOfferId = toLong(actionData.get("offerId"));
                    log.info("Offer rejected via Feishu: {}", rejectOfferId);
                    break;
                case "boost_sourcing":
                    // 增加寻源
                    Long jobId = toLong(actionData.get("jobId"));
                    log.info("Boost sourcing requested via Feishu: {}", jobId);
                    break;
                default:
                    log.info("Unknown card action: {}", actionType);
            }
        } catch (Exception e) {
            log.error("handleCardAction failed", e);
        }
    }

    private void handleBotMessage(Map<String, Object> event) {
        // 处理飞书机器人消息（用于候选人与 AI 对话）
        log.info("Bot message received: {}", event);
    }

    private Long currentTenantId() {
        return tenantResolver.getDefaultTenantId();
    }

    private Long toLong(Object o) {
        if (o instanceof Number) return ((Number) o).longValue();
        return null;
    }
}
