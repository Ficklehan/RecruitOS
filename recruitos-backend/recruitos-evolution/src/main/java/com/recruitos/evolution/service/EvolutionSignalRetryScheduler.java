package com.recruitos.evolution.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.recruitos.common.evolution.EvolutionEmitRequest;
import com.recruitos.common.tenant.TenantContext;
import com.recruitos.evolution.dto.SignalEmitDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;

/**
 * 重放 evolution_signal_retry 队列中失败的发射请求。
 */
@Service
public class EvolutionSignalRetryScheduler {

    private static final Logger log = LoggerFactory.getLogger(EvolutionSignalRetryScheduler.class);

    @Resource
    private JdbcTemplate jdbcTemplate;

    @Resource
    private EvolutionService evolutionService;

    @Resource
    private ObjectMapper objectMapper;

    @Scheduled(fixedDelay = 60000)
    public void replayPendingRetries() {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT id, tenant_id, payload_json, attempts, max_attempts FROM evolution_signal_retry "
                        + "WHERE status = 'PENDING' AND next_retry_at <= NOW() ORDER BY id ASC LIMIT 20");
        for (Map<String, Object> row : rows) {
            Long id = ((Number) row.get("id")).longValue();
            Long tenantId = row.get("tenant_id") != null ? ((Number) row.get("tenant_id")).longValue() : null;
            int attempts = row.get("attempts") != null ? ((Number) row.get("attempts")).intValue() : 0;
            int maxAttempts = row.get("max_attempts") != null ? ((Number) row.get("max_attempts")).intValue() : 5;
            try {
                if (tenantId != null && tenantId > 0) {
                    TenantContext.setTenantId(tenantId);
                }
                EvolutionEmitRequest req = objectMapper.readValue(
                        row.get("payload_json").toString(), EvolutionEmitRequest.class);
                SignalEmitDTO dto = toDto(req);
                evolutionService.emitSignal(dto);
                jdbcTemplate.update(
                        "UPDATE evolution_signal_retry SET status = 'DONE', updated_at = NOW() WHERE id = ?", id);
                log.info("Evolution signal retry succeeded id={} jobId={}", id, req.getJobId());
            } catch (Exception e) {
                int next = attempts + 1;
                if (next >= maxAttempts) {
                    jdbcTemplate.update(
                            "UPDATE evolution_signal_retry SET status = 'FAILED', attempts = ?, "
                                    + "last_error = ?, updated_at = NOW() WHERE id = ?",
                            next, truncate(e.getMessage()), id);
                } else {
                    jdbcTemplate.update(
                            "UPDATE evolution_signal_retry SET attempts = ?, last_error = ?, "
                                    + "next_retry_at = DATE_ADD(NOW(), INTERVAL ? MINUTE), updated_at = NOW() WHERE id = ?",
                            next, truncate(e.getMessage()), Math.min(next * 2, 30), id);
                }
                log.warn("Evolution signal retry failed id={}: {}", id, e.getMessage());
            } finally {
                TenantContext.clear();
            }
        }
    }

    private SignalEmitDTO toDto(EvolutionEmitRequest req) {
        SignalEmitDTO dto = new SignalEmitDTO();
        dto.setJobId(req.getJobId());
        dto.setSignalLevel(req.getSignalLevel());
        dto.setConfidence(req.getConfidence());
        dto.setCandidateId(req.getCandidateId());
        dto.setTagAdjustments(req.getTagAdjustments());
        dto.setSourceModule(req.getSourceModule());
        dto.setSourceEvent(req.getSourceEvent());
        dto.setCampaignId(req.getCampaignId());
        dto.setTraceId(req.getTraceId());
        dto.setAbGroup(req.getAbGroup());
        return dto;
    }

    private static String truncate(String msg) {
        if (msg == null) {
            return null;
        }
        return msg.length() > 500 ? msg.substring(0, 500) : msg;
    }
}
