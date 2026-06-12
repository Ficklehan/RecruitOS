package com.recruitos.brain.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.recruitos.brain.TenantResolver;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.sql.DataSource;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 合规审计 & GDPR 数据删除 API。
 */
@RestController
@RequestMapping("/api/compliance")
public class ComplianceController {

    private static final Logger log = LoggerFactory.getLogger(ComplianceController.class);

    @Resource
    private DataSource dataSource;

    @Resource
    private TenantResolver tenantResolver;

    private Long currentTenantId() {
        return tenantResolver.getDefaultTenantId();
    }

    // ─────────────── 审计日志 ───────────────

    @GetMapping("/audit-logs")
    public Map<String, Object> getAuditLogs(
        @RequestParam(defaultValue = "1") int page,
        @RequestParam(defaultValue = "50") int pageSize,
        @RequestParam(required = false) String action,
        @RequestParam(required = false) String targetType,
        @RequestParam(required = false) Long actorId,
        @RequestParam(required = false) String startDate,
        @RequestParam(required = false) String endDate) {

        Map<String, Object> result = new LinkedHashMap<>();
        List<Map<String, Object>> records = new ArrayList<>();

        StringBuilder sql = new StringBuilder(
            "SELECT id, tenant_id, actor_id, actor_type, action, target_type, target_id, target_label, " +
            "detail_json, ip_address, user_agent, created_at " +
            "FROM compliance_audit_log WHERE tenant_id = " + currentTenantId());
        List<Object> params = new ArrayList<>();

        if (action != null && !action.isEmpty()) {
            sql.append(" AND action = ?");
            params.add(action);
        }
        if (targetType != null && !targetType.isEmpty()) {
            sql.append(" AND target_type = ?");
            params.add(targetType);
        }
        if (actorId != null) {
            sql.append(" AND actor_id = ?");
            params.add(actorId);
        }
        if (startDate != null && !startDate.isEmpty()) {
            sql.append(" AND created_at >= ?");
            params.add(startDate + " 00:00:00");
        }
        if (endDate != null && !endDate.isEmpty()) {
            sql.append(" AND created_at <= ?");
            params.add(endDate + " 23:59:59");
        }

        // Count
        String countSql = "SELECT COUNT(*) FROM (" + sql.toString() + ") t";
        int total = 0;
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(countSql)) {
            for (int i = 0; i < params.size(); i++) ps.setObject(i + 1, params.get(i));
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) total = rs.getInt(1);
            }
        } catch (Exception e) { log.warn("Audit count failed", e); }

        // Query
        sql.append(" ORDER BY created_at DESC LIMIT ? OFFSET ?");
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            int idx = 1;
            for (Object p : params) ps.setObject(idx++, p);
            ps.setInt(idx++, pageSize);
            ps.setInt(idx, (page - 1) * pageSize);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("id", rs.getLong("id"));
                    row.put("tenantId", rs.getLong("tenant_id"));
                    row.put("actorId", rs.getLong("actor_id"));
                    row.put("actorType", rs.getString("actor_type"));
                    row.put("action", rs.getString("action"));
                    row.put("targetType", rs.getString("target_type"));
                    row.put("targetId", rs.getLong("target_id"));
                    row.put("targetLabel", rs.getString("target_label"));
                    row.put("ipAddress", rs.getString("ip_address"));
                    Timestamp ts = rs.getTimestamp("created_at");
                    if (ts != null) row.put("createdAt", ts.toLocalDateTime().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
                    records.add(row);
                }
            }
        } catch (Exception e) { log.warn("Audit query failed", e); }

        result.put("records", records);
        result.put("total", total);
        result.put("page", page);
        result.put("pageSize", pageSize);
        return result;
    }

    @GetMapping("/audit-actions")
    public List<String> getAuditActions() {
        return Arrays.asList(
            "AI_ADVICE_GENERATED", "AI_ADVICE_ACCEPTED", "AI_ADVICE_IGNORED",
            "CANDIDATE_DECISION_HIRE", "CANDIDATE_DECISION_REJECT",
            "OFFER_CREATED", "OFFER_SENT", "OFFER_ACCEPTED", "OFFER_REJECTED",
            "INTERVIEW_SCHEDULED", "INTERVIEW_COMPLETED",
            "DATA_EXPORT", "USER_LOGIN",
            "GDPR_DELETE_REQUEST", "GDPR_DELETE_EXECUTED",
            "GDPR_EXPORT_REQUEST", "GDPR_EXPORT_EXECUTED",
            "RETENTION_POLICY_UPDATED"
        );
    }

    // ─────────────── GDPR 数据删除 ───────────────

    @GetMapping("/gdpr-requests")
    public Map<String, Object> getGdprRequests(
        @RequestParam(defaultValue = "1") int page,
        @RequestParam(defaultValue = "20") int pageSize,
        @RequestParam(required = false) String status) {

        Map<String, Object> result = new LinkedHashMap<>();
        List<Map<String, Object>> records = new ArrayList<>();

        StringBuilder sql = new StringBuilder(
            "SELECT id, tenant_id, request_type, target_type, target_id, target_identifier, " +
            "status, executed_at, verified_by, error_message, created_at " +
            "FROM compliance_gdpr_request WHERE tenant_id = ?");
        List<Object> params = new ArrayList<>();
        params.add(currentTenantId());

        if (status != null && !status.isEmpty()) {
            sql.append(" AND status = ?");
            params.add(status);
        }

        sql.append(" ORDER BY created_at DESC LIMIT ? OFFSET ?");

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            int idx = 1;
            for (Object p : params) ps.setObject(idx++, p);
            ps.setInt(idx++, pageSize);
            ps.setInt(idx, (page - 1) * pageSize);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("id", rs.getLong("id"));
                    row.put("requestType", rs.getString("request_type"));
                    row.put("targetType", rs.getString("target_type"));
                    row.put("targetId", rs.getLong("target_id"));
                    row.put("targetIdentifier", rs.getString("target_identifier"));
                    row.put("status", rs.getString("status"));
                    Timestamp executed = rs.getTimestamp("executed_at");
                    if (executed != null) row.put("executedAt", executed.toLocalDateTime().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
                    row.put("errorMessage", rs.getString("error_message"));
                    Timestamp created = rs.getTimestamp("created_at");
                    if (created != null) row.put("createdAt", created.toLocalDateTime().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
                    records.add(row);
                }
            }
        } catch (Exception e) { log.warn("GDPR query failed", e); }

        result.put("records", records);
        result.put("page", page);
        result.put("pageSize", pageSize);
        return result;
    }

    @PostMapping("/gdpr-requests")
    public Map<String, Object> createGdprRequest(@RequestBody Map<String, Object> body) {
        Map<String, Object> result = new LinkedHashMap<>();
        try (Connection conn = dataSource.getConnection()) {
            String sql = "INSERT INTO compliance_gdpr_request " +
                "(tenant_id, request_type, target_type, target_id, target_identifier, requestor_id, status) " +
                "VALUES (" + currentTenantId() + ", ?, ?, ?, ?, ?, 'PENDING')";
            try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, (String) body.getOrDefault("requestType", "DELETE"));
                ps.setString(2, (String) body.getOrDefault("targetType", "CANDIDATE"));
                ps.setLong(3, ((Number) body.get("targetId")).longValue());
                ps.setString(4, (String) body.getOrDefault("targetIdentifier", ""));
                ps.setLong(5, ((Number) body.getOrDefault("requestorId", 1L)).longValue());
                ps.executeUpdate();
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (keys.next()) result.put("id", keys.getLong(1));
                }
            }
            result.put("status", "PENDING");
            result.put("message", "GDPR 删除请求已提交，等待二次确认后执行");
        } catch (Exception e) {
            log.error("GDPR request creation failed", e);
            result.put("status", "FAILED");
            result.put("message", "请求创建失败: " + e.getMessage());
        }
        return result;
    }

    @PostMapping("/gdpr-requests/{id}/execute")
    public Map<String, Object> executeGdprDeletion(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        Map<String, Object> result = new LinkedHashMap<>();
        try (Connection conn = dataSource.getConnection()) {
            conn.setAutoCommit(false);
            try {
                // 获取请求详情
                String reqSql = "SELECT target_type, target_id FROM compliance_gdpr_request WHERE id = ?";
                String targetType = null;
                Long targetId = null;
                try (PreparedStatement ps = conn.prepareStatement(reqSql)) {
                    ps.setLong(1, id);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            targetType = rs.getString("target_type");
                            targetId = rs.getLong("target_id");
                        }
                    }
                }

                if (targetType == null || targetId == null) {
                    conn.rollback();
                    result.put("status", "FAILED");
                    result.put("message", "请求不存在");
                    return result;
                }

                List<String> operations = new ArrayList<>();

                // 根据目标类型执行数据删除/匿名化
                if ("CANDIDATE".equals(targetType)) {
                    // 匿名化候选人数据
                    String anonSql = "UPDATE external_candidate SET name = CONCAT('已删除用户_', id), " +
                        "email = CONCAT('deleted_', id, '@anonymized.local'), " +
                        "phone = '00000000000', resume_text = '[数据已按GDPR要求删除]' " +
                        "WHERE id = ?";
                    try (PreparedStatement ps = conn.prepareStatement(anonSql)) {
                        ps.setLong(1, targetId);
                        int rows = ps.executeUpdate();
                        operations.add("候选人个人信息匿名化: " + rows + " 条");
                    }

                    // 删除沟通记录中的可识别内容
                    String commSql = "UPDATE communication_message SET content = '[已删除]' " +
                        "WHERE candidate_id = ? AND content LIKE '%个人%'";
                    try (PreparedStatement ps = conn.prepareStatement(commSql)) {
                        ps.setLong(1, targetId);
                        int rows = ps.executeUpdate();
                        operations.add("敏感沟通内容清除: " + rows + " 条");
                    }

                    // 删除面试录音文件路径引用
                    String intvSql = "UPDATE interview_record SET recording_url = NULL WHERE candidate_id = ?";
                    try (PreparedStatement ps = conn.prepareStatement(intvSql)) {
                        ps.setLong(1, targetId);
                        int rows = ps.executeUpdate();
                        operations.add("面试录音文件引用清除: " + rows + " 条");
                    }
                }

                // 更新请求状态
                String updateSql = "UPDATE compliance_gdpr_request SET status = 'COMPLETED', " +
                    "executed_at = NOW(), verified_by = ?, audit_trail = ? WHERE id = ?";
                try (PreparedStatement ps = conn.prepareStatement(updateSql)) {
                    ps.setLong(1, ((Number) body.getOrDefault("verifiedBy", 1L)).longValue());
                    ps.setString(2, String.join("; ", operations));
                    ps.setLong(3, id);
                    ps.executeUpdate();
                }

                // 写入审计日志
                String auditSql = "INSERT INTO compliance_audit_log " +
                    "(tenant_id, actor_id, actor_type, action, target_type, target_id, target_label, detail_json) " +
                    "VALUES (" + currentTenantId() + ", ?, 'USER', 'GDPR_DELETE_EXECUTED', ?, ?, ?, ?)";
                try (PreparedStatement ps = conn.prepareStatement(auditSql)) {
                    ps.setLong(1, ((Number) body.getOrDefault("verifiedBy", 1L)).longValue());
                    ps.setString(2, targetType);
                    ps.setLong(3, targetId);
                    ps.setString(4, targetType + "_" + targetId);
                    ps.setString(5, "{\"operations\":\"" + String.join("; ", operations) + "\"}");
                    ps.executeUpdate();
                }

                conn.commit();
                result.put("status", "COMPLETED");
                result.put("operations", operations);
                result.put("message", "GDPR 数据删除执行完成");
            } catch (Exception e) {
                conn.rollback();
                throw e;
            }
        } catch (Exception e) {
            log.error("GDPR deletion execution failed", e);
            result.put("status", "FAILED");
            result.put("message", "执行失败: " + e.getMessage());
        }
        return result;
    }

    // ─────────────── 数据保留策略 ───────────────

    @GetMapping("/retention-policies")
    public List<Map<String, Object>> getRetentionPolicies() {
        List<Map<String, Object>> records = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(
                 "SELECT id, data_category, retention_days, auto_delete, legal_hold, updated_at " +
                 "FROM compliance_retention_policy WHERE tenant_id = " + currentTenantId() + " ORDER BY data_category")) {
            while (rs.next()) {
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("id", rs.getLong("id"));
                row.put("dataCategory", rs.getString("data_category"));
                row.put("retentionDays", rs.getInt("retention_days"));
                row.put("autoDelete", rs.getBoolean("auto_delete"));
                row.put("legalHold", rs.getBoolean("legal_hold"));
                Timestamp ts = rs.getTimestamp("updated_at");
                if (ts != null) row.put("updatedAt", ts.toLocalDateTime().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
                records.add(row);
            }
        } catch (Exception e) { log.warn("Retention policy query failed", e); }
        return records;
    }

    @PutMapping("/retention-policies/{id}")
    public Map<String, Object> updateRetentionPolicy(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        Map<String, Object> result = new LinkedHashMap<>();
        try (Connection conn = dataSource.getConnection()) {
            String sql = "UPDATE compliance_retention_policy SET retention_days = ?, auto_delete = ?, " +
                "legal_hold = ?, updated_at = NOW(), updated_by = ? WHERE id = ? AND tenant_id = " + currentTenantId();
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, ((Number) body.get("retentionDays")).intValue());
                ps.setBoolean(2, (Boolean) body.getOrDefault("autoDelete", false));
                ps.setBoolean(3, (Boolean) body.getOrDefault("legalHold", false));
                ps.setLong(4, ((Number) body.getOrDefault("updatedBy", 1L)).longValue());
                ps.setLong(5, id);
                ps.executeUpdate();
            }
            result.put("status", "OK");
            result.put("message", "保留策略已更新");
        } catch (Exception e) {
            log.error("Retention policy update failed", e);
            result.put("status", "FAILED");
            result.put("message", e.getMessage());
        }
        return result;
    }

    // ─────────────── 审计摘要 ───────────────

    @GetMapping("/summary")
    public Map<String, Object> getAuditSummary() {
        Map<String, Object> summary = new LinkedHashMap<>();
        try (Connection conn = dataSource.getConnection();
             Statement st = conn.createStatement()) {

            // 近 30 天审计记录数
            ResultSet rs = st.executeQuery(
                "SELECT COUNT(*) FROM compliance_audit_log WHERE tenant_id = " + currentTenantId() + " AND created_at >= DATE_SUB(NOW(), INTERVAL 30 DAY)");
            if (rs.next()) summary.put("auditCount30d", rs.getInt(1));

            // 待处理的 GDPR 请求
            rs = st.executeQuery(
                "SELECT COUNT(*) FROM compliance_gdpr_request WHERE tenant_id = " + currentTenantId() + " AND status IN ('PENDING','IN_PROGRESS')");
            if (rs.next()) summary.put("pendingGdprRequests", rs.getInt(1));

            // 已完成的 GDPR 请求
            rs = st.executeQuery(
                "SELECT COUNT(*) FROM compliance_gdpr_request WHERE tenant_id = " + currentTenantId() + " AND status = 'COMPLETED'");
            if (rs.next()) summary.put("completedGdprRequests", rs.getInt(1));

            // 即将到期的数据类别
            rs = st.executeQuery(
                "SELECT data_category, retention_days FROM compliance_retention_policy " +
                "WHERE tenant_id = " + currentTenantId() + " AND auto_delete = TRUE AND legal_hold = FALSE ORDER BY retention_days LIMIT 3");
            List<Map<String, Object>> expiringSoon = new ArrayList<>();
            while (rs.next()) {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("category", rs.getString("data_category"));
                item.put("retentionDays", rs.getInt("retention_days"));
                expiringSoon.add(item);
            }
            summary.put("expiringDataCategories", expiringSoon);

        } catch (Exception e) { log.warn("Audit summary failed", e); }
        return summary;
    }
}
