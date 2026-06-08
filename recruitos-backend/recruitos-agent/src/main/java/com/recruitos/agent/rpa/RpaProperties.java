package com.recruitos.agent.rpa;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "recruitos.agent.rpa")
public class RpaProperties {

    /** 启用 Playwright 真实 RPA；false 时使用模拟适配器 */
    private boolean enabled = true;

    /** 无头模式；首次扫码登录建议 false */
    private boolean headless = false;

    /** RPA 失败时是否回退模拟数据 */
    private boolean fallbackSimulated = false;

    /** 会话与下载文件根目录 */
    private String sessionDir = "./data/rpa-sessions";

    /** 手动/扫码登录最长等待秒数 */
    private int loginTimeoutSeconds = 300;

    /** 单步操作超时毫秒 */
    private int actionTimeoutMs = 45000;

    /** 操作间隔毫秒，降低风控 */
    private int slowMoMs = 80;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isHeadless() {
        return headless;
    }

    public void setHeadless(boolean headless) {
        this.headless = headless;
    }

    public boolean isFallbackSimulated() {
        return fallbackSimulated;
    }

    public void setFallbackSimulated(boolean fallbackSimulated) {
        this.fallbackSimulated = fallbackSimulated;
    }

    public String getSessionDir() {
        return sessionDir;
    }

    public void setSessionDir(String sessionDir) {
        this.sessionDir = sessionDir;
    }

    public int getLoginTimeoutSeconds() {
        return loginTimeoutSeconds;
    }

    public void setLoginTimeoutSeconds(int loginTimeoutSeconds) {
        this.loginTimeoutSeconds = loginTimeoutSeconds;
    }

    public int getActionTimeoutMs() {
        return actionTimeoutMs;
    }

    public void setActionTimeoutMs(int actionTimeoutMs) {
        this.actionTimeoutMs = actionTimeoutMs;
    }

    public int getSlowMoMs() {
        return slowMoMs;
    }

    public void setSlowMoMs(int slowMoMs) {
        this.slowMoMs = slowMoMs;
    }
}
