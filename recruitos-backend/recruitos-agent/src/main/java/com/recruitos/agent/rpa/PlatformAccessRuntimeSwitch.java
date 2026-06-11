package com.recruitos.agent.rpa;

import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 运行时平台访问开关，可一键锁定/解锁，优先级高于 application.yml。
 */
@Component
public class PlatformAccessRuntimeSwitch {

    private volatile boolean testingLocked;
    /** null 表示跟随配置文件；true/false 为运行时覆盖 */
    private volatile Boolean runtimeOverride;
    private volatile LocalDateTime lastLockedAt;
    private volatile LocalDateTime lastUnlockedAt;
    private volatile String lastUnlockReason;

    public boolean isTestingLocked() {
        return testingLocked;
    }

    public Boolean getRuntimeOverride() {
        return runtimeOverride;
    }

    public LocalDateTime getLastLockedAt() {
        return lastLockedAt;
    }

    public LocalDateTime getLastUnlockedAt() {
        return lastUnlockedAt;
    }

    public String getLastUnlockReason() {
        return lastUnlockReason;
    }

    public boolean isAccessAllowed(RpaProperties properties) {
        if (testingLocked) {
            return false;
        }
        if (runtimeOverride != null) {
            return properties.isEnabled() && runtimeOverride;
        }
        return properties.isPlatformAccessAllowed();
    }

    public synchronized void lockTestingMode() {
        testingLocked = true;
        runtimeOverride = false;
        lastLockedAt = LocalDateTime.now();
    }

    public synchronized void unlockLiveMode(String reason) {
        testingLocked = false;
        runtimeOverride = true;
        lastUnlockedAt = LocalDateTime.now();
        lastUnlockReason = reason != null ? reason.trim() : null;
    }

    public synchronized void resetToConfig() {
        testingLocked = false;
        runtimeOverride = null;
        lastUnlockReason = null;
    }
}
