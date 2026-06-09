package com.recruitos.common.evolution;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "recruitos.evolution")
public class EvolutionClientProperties {

    /** 进化服务 base URL，如 http://localhost:8090 */
    private String baseUrl = "http://localhost:8090";

    /** 是否启用远程 emit */
    private boolean enabled = true;

    /** HTTP 超时毫秒 */
    private int timeoutMs = 3000;

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getTimeoutMs() {
        return timeoutMs;
    }

    public void setTimeoutMs(int timeoutMs) {
        this.timeoutMs = timeoutMs;
    }
}
