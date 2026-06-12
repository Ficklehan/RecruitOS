package com.recruitos.brain.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.sql.DataSource;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.sql.*;
import java.util.*;

/**
 * 飞书深度工作流集成服务。
 * 负责发送飞书卡片消息、管理 webhook 配置、渲染消息模板。
 */
@Service
public class FeishuIntegrationService {

    private static final Logger log = LoggerFactory.getLogger(FeishuIntegrationService.class);

    @Resource
    private DataSource dataSource;

    @Resource
    private ObjectMapper objectMapper;

    // ─────────────── 配置管理 ───────────────

    /** 获取租户的飞书 webhook URL */
    public String getWebhookUrl(Long tenantId) {
        return getConfig(tenantId, "FEISHU", "webhook_url");
    }

    private String getConfig(Long tenantId, String platform, String key) {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                 "SELECT config_value FROM integration_config WHERE tenant_id = ? AND platform = ? AND config_key = ? AND enabled = TRUE")) {
            ps.setLong(1, tenantId);
            ps.setString(2, platform);
            ps.setString(3, key);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getString("config_value");
            }
        } catch (Exception e) { log.warn("getConfig failed: {} {} {}", tenantId, platform, key, e); }
        return null;
    }

    /** 获取所有集成配置 */
    public List<Map<String, Object>> getConfigs(Long tenantId) {
        List<Map<String, Object>> list = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                 "SELECT id, platform, config_key, enabled, updated_at FROM integration_config WHERE tenant_id = ?")) {
            ps.setLong(1, tenantId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("id", rs.getLong("id"));
                    row.put("platform", rs.getString("platform"));
                    row.put("configKey", rs.getString("config_key"));
                    row.put("enabled", rs.getBoolean("enabled"));
                    Timestamp ts = rs.getTimestamp("updated_at");
                    if (ts != null) row.put("updatedAt", ts.toLocalDateTime().toString());
                    list.add(row);
                }
            }
        } catch (Exception e) { log.warn("getConfigs failed", e); }
        return list;
    }

    /** 保存集成配置 */
    public void saveConfig(Long tenantId, String platform, String key, String value) {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                 "INSERT INTO integration_config (tenant_id, platform, config_key, config_value) " +
                 "VALUES (?, ?, ?, ?) ON DUPLICATE KEY UPDATE config_value = VALUES(config_value), updated_at = NOW()")) {
            ps.setLong(1, tenantId);
            ps.setString(2, platform);
            ps.setString(3, key);
            ps.setString(4, value);
            ps.executeUpdate();
        } catch (Exception e) { log.warn("saveConfig failed", e); }
    }

    // ─────────────── 消息发送 ───────────────

    /**
     * 发送飞书卡片消息。
     * @param tenantId 租户 ID
     * @param templateCode 模板编码（INTERVIEW_INVITE / OFFER_APPROVAL / PIPELINE_ALERT / DECISION_REMINDER）
     * @param variables 模板变量替换
     */
    public Map<String, Object> sendFeishuCard(Long tenantId, String templateCode, Map<String, Object> variables) {
        Map<String, Object> result = new LinkedHashMap<>();
        try {
            String webhookUrl = getWebhookUrl(tenantId);
            if (webhookUrl == null || webhookUrl.isEmpty()) {
                result.put("success", false);
                result.put("message", "飞书 webhook 未配置");
                return result;
            }

            // 获取模板
            String cardJson = getTemplateCard(tenantId, "FEISHU", templateCode);
            if (cardJson == null) {
                result.put("success", false);
                result.put("message", "消息模板未找到: " + templateCode);
                return result;
            }

            // 变量替换
            String rendered = cardJson;
            for (Map.Entry<String, Object> entry : variables.entrySet()) {
                rendered = rendered.replace("{{" + entry.getKey() + "}}",
                    entry.getValue() != null ? entry.getValue().toString() : "");
            }

            // 构建飞书卡片消息体
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("msg_type", "interactive");
            Map<String, Object> card = new LinkedHashMap<>();
            card.put("config", Map.of("wide_screen_mode", true));
            card.put("header", Map.of("title", Map.of("tag", "plain_text", "content", "RecruitOS 通知")));
            payload.put("card", objectMapper.readTree(rendered));

            String body = objectMapper.writeValueAsString(payload);
            URL url = new URL(webhookUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(10000);
            conn.setDoOutput(true);

            try (OutputStream os = conn.getOutputStream()) {
                os.write(body.getBytes("UTF-8"));
            }

            int statusCode = conn.getResponseCode();
            String responseBody = "";
            try (BufferedReader br = new BufferedReader(new InputStreamReader(
                    statusCode >= 200 && statusCode < 300 ? conn.getInputStream() : conn.getErrorStream()))) {
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) sb.append(line);
                responseBody = sb.toString();
            }

            Map<String, Object> respBody = objectMapper.readValue(responseBody, new TypeReference<Map<String, Object>>() {});
            result.put("success", statusCode == 200 && "ok".equals(respBody.get("code")));
            result.put("message", respBody.getOrDefault("msg", ""));

        } catch (Exception e) {
            log.error("sendFeishuCard failed: {} {}", templateCode, e.getMessage());
            result.put("success", false);
            result.put("message", "发送失败: " + e.getMessage());
        }
        return result;
    }

    /**
     * 发送面试邀请卡片。
     */
    public Map<String, Object> sendInterviewInvite(Long tenantId, String candidateName, String jobTitle,
                                                    String interviewTime, String interviewType,
                                                    String interviewer, String prepNotes, Long interviewId) {
        return sendFeishuCard(tenantId, "INTERVIEW_INVITE", Map.of(
            "candidateName", candidateName,
            "jobTitle", jobTitle,
            "interviewTime", interviewTime,
            "interviewType", interviewType,
            "interviewer", interviewer,
            "prepNotes", prepNotes,
            "interviewId", String.valueOf(interviewId),
            "detailUrl", "/pipeline/candidate/" + interviewId
        ));
    }

    /**
     * 发送 Offer 审批卡片。
     */
    public Map<String, Object> sendOfferApproval(Long tenantId, String candidateName, String jobTitle,
                                                  String salaryPackage, String level, String marketCompetitiveness,
                                                  String attritionRisk, String aiAdvice, Long offerId) {
        return sendFeishuCard(tenantId, "OFFER_APPROVAL", Map.of(
            "candidateName", candidateName,
            "jobTitle", jobTitle,
            "salaryPackage", salaryPackage,
            "level", level,
            "marketCompetitiveness", marketCompetitiveness,
            "attritionRisk", attritionRisk,
            "aiAdvice", aiAdvice,
            "offerId", String.valueOf(offerId),
            "detailUrl", "/pipeline/offer/" + offerId
        ));
    }

    /**
     * 发送管道告警卡片。
     */
    public Map<String, Object> sendPipelineAlert(Long tenantId, String jobTitle, int totalCandidates,
                                                  int daysSinceLast, int daysOpen, String aiAdvice, Long jobId) {
        return sendFeishuCard(tenantId, "PIPELINE_ALERT", Map.of(
            "jobTitle", jobTitle,
            "totalCandidates", String.valueOf(totalCandidates),
            "daysSinceLast", String.valueOf(daysSinceLast),
            "daysOpen", String.valueOf(daysOpen),
            "aiAdvice", aiAdvice,
            "jobId", String.valueOf(jobId),
            "detailUrl", "/job/" + jobId + "/workspace"
        ));
    }

    /**
     * 发送决策提醒卡片。
     */
    public Map<String, Object> sendDecisionReminder(Long tenantId, String candidateName, String jobTitle,
                                                     String currentStage, int daysInStage,
                                                     String aiObservation, Long candidateId) {
        return sendFeishuCard(tenantId, "DECISION_REMINDER", Map.of(
            "candidateName", candidateName,
            "jobTitle", jobTitle,
            "currentStage", currentStage,
            "daysInStage", String.valueOf(daysInStage),
            "aiObservation", aiObservation,
            "candidateId", String.valueOf(candidateId),
            "detailUrl", "/pipeline/candidate/" + candidateId
        ));
    }

    // ─────────────── 模板管理 ───────────────

    private String getTemplateCard(Long tenantId, String platform, String templateCode) {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                 "SELECT card_json FROM integration_message_template " +
                 "WHERE tenant_id = ? AND platform = ? AND template_code = ? AND enabled = TRUE")) {
            ps.setLong(1, tenantId);
            ps.setString(2, platform);
            ps.setString(3, templateCode);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getString("card_json");
            }
        } catch (Exception e) { log.warn("getTemplateCard failed", e); }
        return null;
    }

    public List<Map<String, Object>> getTemplates(Long tenantId, String platform) {
        List<Map<String, Object>> list = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                 "SELECT id, template_code, template_name, enabled, updated_at " +
                 "FROM integration_message_template WHERE tenant_id = ? AND platform = ?")) {
            ps.setLong(1, tenantId);
            ps.setString(2, platform);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("id", rs.getLong("id"));
                    row.put("templateCode", rs.getString("template_code"));
                    row.put("templateName", rs.getString("template_name"));
                    row.put("enabled", rs.getBoolean("enabled"));
                    Timestamp ts = rs.getTimestamp("updated_at");
                    if (ts != null) row.put("updatedAt", ts.toLocalDateTime().toString());
                    list.add(row);
                }
            }
        } catch (Exception e) { log.warn("getTemplates failed", e); }
        return list;
    }
}
