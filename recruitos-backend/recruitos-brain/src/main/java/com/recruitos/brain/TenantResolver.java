package com.recruitos.brain;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * 租户解析器 — 从数据库动态获取活跃租户列表。
 * 替代全局硬编码的 tenant_id=1。
 */
@Component
public class TenantResolver {

    private static final Logger log = LoggerFactory.getLogger(TenantResolver.class);

    @Resource
    private DataSource dataSource;

    /** 获取所有活跃租户 ID */
    public List<Long> getActiveTenantIds() {
        List<Long> ids = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             Statement st = conn.createStatement()) {
            // 尝试从租户表获取，如果表不存在则返回默认租户
            try (ResultSet rs = st.executeQuery(
                "SELECT id FROM sys_tenant WHERE status = 'ACTIVE' ORDER BY id")) {
                while (rs.next()) ids.add(rs.getLong("id"));
            }
        } catch (Exception e) {
            log.debug("Tenant table not available, using default tenant: {}", e.getMessage());
        }
        if (ids.isEmpty()) {
            ids.add(1L); // 开发环境默认租户
        }
        return ids;
    }

    /** 单租户兼容：返回默认租户 */
    public Long getDefaultTenantId() {
        List<Long> ids = getActiveTenantIds();
        return ids.isEmpty() ? 1L : ids.get(0);
    }
}
