package com.recruitos.common.license;

import com.recruitos.common.exception.BizException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDate;
import java.util.Map;

/**
 * 租户许可配额 hard block：超额直接拒绝 API 请求（HTTP 402）。
 */
@Service
public class LicenseQuotaService {

    private static final int HTTP_QUOTA_EXCEEDED = 402;

    @Resource
    private JdbcTemplate jdbcTemplate;

    public void assertCanCreateJob(Long tenantId) {
        LicenseSnapshot license = requireActiveLicense(tenantId);
        int used = countActiveJobs(tenantId);
        if (used >= license.maxJobs) {
            throw quotaExceeded("岗位配额已满（上限 " + license.maxJobs + "），请升级套餐或关闭闲置岗位");
        }
    }

    public void assertCanCreateAgent(Long tenantId) {
        LicenseSnapshot license = requireActiveLicense(tenantId);
        int used = countActiveAgents(tenantId);
        if (used >= license.maxAgents) {
            throw quotaExceeded("Agent 配额已满（上限 " + license.maxAgents + "），请升级套餐");
        }
    }

    public void assertCanSendMessage(Long tenantId) {
        LicenseSnapshot license = requireActiveLicense(tenantId);
        int used = license.messageUsed != null ? license.messageUsed : 0;
        if (used >= license.messageQuota) {
            throw quotaExceeded("AI 消息配额已用尽（上限 " + license.messageQuota + "），请升级套餐");
        }
    }

    public void assertCanParseResume(Long tenantId) {
        LicenseSnapshot license = requireActiveLicense(tenantId);
        int used = license.resumeUsed != null ? license.resumeUsed : 0;
        if (used >= license.resumeQuota) {
            throw quotaExceeded("简历解析配额已用尽（上限 " + license.resumeQuota + "），请升级套餐");
        }
    }

    @Transactional
    public void recordJobCreated(Long tenantId) {
        syncUsedJobs(tenantId);
    }

    @Transactional
    public void recordAgentCreated(Long tenantId) {
        syncUsedAgents(tenantId);
    }

    @Transactional
    public void recordMessageSent(Long tenantId) {
        jdbcTemplate.update(
                "UPDATE tenant_license SET message_used = IFNULL(message_used, 0) + 1, updated_at = NOW() "
                        + "WHERE tenant_id = ? AND status = 1",
                tenantId);
    }

    @Transactional
    public void recordResumeParsed(Long tenantId) {
        jdbcTemplate.update(
                "UPDATE tenant_license SET resume_used = IFNULL(resume_used, 0) + 1, updated_at = NOW() "
                        + "WHERE tenant_id = ? AND status = 1",
                tenantId);
    }

    private LicenseSnapshot requireActiveLicense(Long tenantId) {
        if (tenantId == null) {
            throw new BizException("User not authenticated");
        }
        LicenseSnapshot license = loadLicense(tenantId);
        if (license == null) {
            throw quotaExceeded("未找到有效许可，请联系管理员");
        }
        if (license.status == null || license.status != 1) {
            throw quotaExceeded("许可已停用，请续费后继续使用");
        }
        LocalDate today = LocalDate.now();
        if (license.endDate != null && today.isAfter(license.endDate)) {
            int grace = license.graceDays != null ? license.graceDays : 0;
            if (license.endDate.plusDays(grace).isBefore(today)) {
                throw quotaExceeded("许可已过期，请续费后继续使用");
            }
        }
        return license;
    }

    private LicenseSnapshot loadLicense(Long tenantId) {
        try {
            Map<String, Object> row = jdbcTemplate.queryForMap(
                    "SELECT plan, max_jobs, max_agents, resume_quota, resume_used, message_quota, message_used, "
                            + "end_date, grace_days, status FROM tenant_license WHERE tenant_id = ? LIMIT 1",
                    tenantId);
            LicenseSnapshot s = new LicenseSnapshot();
            s.plan = stringVal(row.get("plan"));
            s.maxJobs = intVal(row.get("max_jobs"), 5);
            s.maxAgents = intVal(row.get("max_agents"), 1);
            s.resumeQuota = intVal(row.get("resume_quota"), 100);
            s.resumeUsed = intVal(row.get("resume_used"), 0);
            s.messageQuota = intVal(row.get("message_quota"), 500);
            s.messageUsed = intVal(row.get("message_used"), 0);
            s.graceDays = intVal(row.get("grace_days"), 15);
            s.status = intVal(row.get("status"), 0);
            Object endDate = row.get("end_date");
            if (endDate instanceof java.sql.Date) {
                s.endDate = ((java.sql.Date) endDate).toLocalDate();
            } else if (endDate instanceof LocalDate) {
                s.endDate = (LocalDate) endDate;
            }
            return s;
        } catch (Exception e) {
            return null;
        }
    }

    private int countActiveJobs(Long tenantId) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM job_position WHERE tenant_id = ? "
                        + "AND status NOT IN ('CLOSED', 'CANCELLED', 'ARCHIVED')",
                Integer.class, tenantId);
        return count != null ? count : 0;
    }

    private int countActiveAgents(Long tenantId) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM agent_account WHERE tenant_id = ? AND status = 'ACTIVE'",
                Integer.class, tenantId);
        return count != null ? count : 0;
    }

    private void syncUsedJobs(Long tenantId) {
        int used = countActiveJobs(tenantId);
        jdbcTemplate.update(
                "UPDATE tenant_license SET used_jobs = ?, updated_at = NOW() WHERE tenant_id = ?",
                used, tenantId);
    }

    private void syncUsedAgents(Long tenantId) {
        int used = countActiveAgents(tenantId);
        jdbcTemplate.update(
                "UPDATE tenant_license SET used_agents = ?, updated_at = NOW() WHERE tenant_id = ?",
                used, tenantId);
    }

    private BizException quotaExceeded(String message) {
        return new BizException(HTTP_QUOTA_EXCEEDED, message);
    }

    private static int intVal(Object v, int defaultVal) {
        if (v instanceof Number) {
            return ((Number) v).intValue();
        }
        return defaultVal;
    }

    private static String stringVal(Object v) {
        return v != null ? v.toString() : null;
    }

    private static final class LicenseSnapshot {
        String plan;
        int maxJobs;
        int maxAgents;
        int resumeQuota;
        Integer resumeUsed;
        int messageQuota;
        Integer messageUsed;
        LocalDate endDate;
        Integer graceDays;
        Integer status;
    }
}
