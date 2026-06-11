package com.recruitos.common.evolution;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.time.LocalDateTime;

/**
 * 进化信号 HTTP 发射失败时写入重试队列（需 JdbcTemplate，llm 等无 DB 服务自动跳过）。
 */
@Component
@ConditionalOnBean(JdbcTemplate.class)
public class EvolutionSignalRetryStore {

    private static final Logger log = LoggerFactory.getLogger(EvolutionSignalRetryStore.class);

    @Resource
    private JdbcTemplate jdbcTemplate;

    @Resource
    private ObjectMapper objectMapper;

    public void enqueue(EvolutionEmitRequest request, String error) {
        if (request == null || request.getJobId() == null) {
            return;
        }
        try {
            Long tenantId = com.recruitos.common.tenant.TenantContext.getTenantId();
            if (tenantId == null) {
                tenantId = 0L;
            }
            String payload = objectMapper.writeValueAsString(request);
            String err = error != null && error.length() > 500 ? error.substring(0, 500) : error;
            jdbcTemplate.update(
                    "INSERT INTO evolution_signal_retry (tenant_id, payload_json, attempts, max_attempts, "
                            + "next_retry_at, last_error, status, created_at, updated_at) "
                            + "VALUES (?, ?, 0, 5, DATE_ADD(NOW(), INTERVAL 1 MINUTE), ?, 'PENDING', NOW(), NOW())",
                    tenantId, payload, err);
            log.info("Evolution signal queued for retry: jobId={} event={}", request.getJobId(), request.getSourceEvent());
        } catch (Exception e) {
            log.warn("Failed to queue evolution signal retry: {}", e.getMessage());
        }
    }
}
