package com.recruitos.evolution.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.recruitos.common.exception.BizException;
import com.recruitos.common.tenant.TenantContext;
import com.recruitos.evolution.mapper.TenantEvolutionConfigMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class EvolutionSettingsService {

    private static final String KEY_MIN_SIGNALS = "evolutionMinSignals";

    @Value("${recruitos.evolution.min-signals:15}")
    private int defaultMinSignals;

    @Resource
    private TenantEvolutionConfigMapper tenantConfigMapper;
    @Resource
    private ObjectMapper objectMapper;

    public int getMinSignals() {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            return defaultMinSignals;
        }
        return resolveMinSignals(tenantId);
    }

    public int resolveMinSignals(Long tenantId) {
        try {
            String raw = tenantConfigMapper.selectConfigJson(tenantId);
            if (!StringUtils.hasText(raw)) {
                return defaultMinSignals;
            }
            Map<String, Object> cfg = objectMapper.readValue(raw, new TypeReference<Map<String, Object>>() {});
            Object v = cfg.get(KEY_MIN_SIGNALS);
            if (v instanceof Number) {
                int n = ((Number) v).intValue();
                return Math.max(3, Math.min(n, 100));
            }
        } catch (Exception ignored) {
            // fall through
        }
        return defaultMinSignals;
    }

    public Map<String, Object> getSettings() {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("minSignals", getMinSignals());
        out.put("defaultMinSignals", defaultMinSignals);
        return out;
    }

    public Map<String, Object> updateMinSignals(int minSignals) {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new BizException("未登录");
        }
        if (minSignals < 3 || minSignals > 100) {
            throw new BizException("信号阈值须在 3–100 之间");
        }
        try {
            String raw = tenantConfigMapper.selectConfigJson(tenantId);
            Map<String, Object> cfg = StringUtils.hasText(raw)
                    ? objectMapper.readValue(raw, new TypeReference<Map<String, Object>>() {})
                    : new LinkedHashMap<>();
            cfg.put(KEY_MIN_SIGNALS, minSignals);
            tenantConfigMapper.updateConfigJson(tenantId, objectMapper.writeValueAsString(cfg));
        } catch (Exception e) {
            throw new BizException("保存设置失败: " + e.getMessage());
        }
        return getSettings();
    }
}
