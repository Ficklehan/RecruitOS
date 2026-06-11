package com.recruitos.common.evolution;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.recruitos.common.tenant.TenantContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

/**
 * 向进化引擎异步发射信号；失败仅记日志，不阻塞主业务。
 */
@Component
@EnableConfigurationProperties(EvolutionClientProperties.class)
@ConditionalOnClass(RestTemplate.class)
public class EvolutionSignalEmitter {

    private static final Logger log = LoggerFactory.getLogger(EvolutionSignalEmitter.class);

    private final EvolutionClientProperties properties;
    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate;

    @Autowired(required = false)
    private EvolutionSignalRetryStore retryStore;

    public EvolutionSignalEmitter(EvolutionClientProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(properties.getTimeoutMs());
        factory.setReadTimeout(properties.getTimeoutMs());
        this.restTemplate = new RestTemplate(factory);
    }

    /**
     * 发射进化信号（fire-and-forget）
     */
    public void emit(EvolutionEmitRequest request) {
        if (request == null || request.getJobId() == null) {
            return;
        }
        if (!properties.isEnabled()) {
            log.debug("Evolution emit skipped (disabled): jobId={}", request.getJobId());
            return;
        }
        if (request.getConfidence() == null) {
            request.setConfidence(EvolutionSignalLevel.defaultConfidence(request.getSignalLevel()));
        }
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            Long tenantId = TenantContext.getTenantId();
            if (tenantId != null) {
                headers.set("X-Tenant-Id", String.valueOf(tenantId));
            }
            String url = properties.getBaseUrl().replaceAll("/$", "") + "/api/evolution/signals/emit";
            restTemplate.postForEntity(url, new HttpEntity<>(request, headers), String.class);
            log.debug("Evolution signal emitted: jobId={} level={} event={}",
                    request.getJobId(), request.getSignalLevel(), request.getSourceEvent());
        } catch (Exception e) {
            log.warn("Evolution signal emit failed (non-blocking): jobId={} event={} err={}",
                    request.getJobId(), request.getSourceEvent(), e.getMessage());
            if (retryStore != null) {
                retryStore.enqueue(request, e.getMessage());
            }
        }
    }
}
