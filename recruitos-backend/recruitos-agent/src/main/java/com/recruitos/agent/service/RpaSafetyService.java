package com.recruitos.agent.service;

import com.recruitos.agent.dto.RpaUnlockDTO;
import com.recruitos.agent.rpa.PlatformAccessGuard;
import com.recruitos.agent.rpa.PlatformAccessRuntimeSwitch;
import com.recruitos.agent.rpa.PlaywrightManager;
import com.recruitos.agent.rpa.RpaProperties;
import com.recruitos.common.exception.BizException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.Map;

@Service
public class RpaSafetyService {

    private static final Logger log = LoggerFactory.getLogger(RpaSafetyService.class);

    @Resource
    private RpaProperties rpaProperties;
    @Resource
    private PlatformAccessRuntimeSwitch runtimeSwitch;
    @Resource
    private SourcingCampaignService sourcingCampaignService;
    @Resource
    private PlaywrightManager playwrightManager;

    public boolean isPlatformAccessAllowed() {
        return runtimeSwitch.isAccessAllowed(rpaProperties);
    }

    public Map<String, Object> status() {
        Map<String, Object> m = new HashMap<>();
        boolean allowed = runtimeSwitch.isAccessAllowed(rpaProperties);
        m.put("enabled", rpaProperties.isEnabled());
        m.put("configPlatformAccessEnabled", rpaProperties.isPlatformAccessEnabled());
        m.put("platformAccessEnabled", allowed);
        m.put("platformAccessAllowed", allowed);
        m.put("simulatedMode", !allowed);
        m.put("testingLocked", runtimeSwitch.isTestingLocked());
        m.put("runtimeOverride", runtimeSwitch.getRuntimeOverride());
        m.put("lastLockedAt", runtimeSwitch.getLastLockedAt());
        m.put("lastUnlockedAt", runtimeSwitch.getLastUnlockedAt());
        m.put("lastUnlockReason", runtimeSwitch.getLastUnlockReason());
        m.put("headless", rpaProperties.isHeadless());
        m.put("fallbackSimulated", rpaProperties.isFallbackSimulated());
        m.put("sessionDir", rpaProperties.getSessionDir());
        return m;
    }

    /**
     * 一键：暂停全部寻源工作流 + 关闭浏览器 + 锁定平台访问（仅模拟）。
     */
    public Map<String, Object> lockTestingMode() {
        int paused = sourcingCampaignService.pauseAllRunningGlobal();
        playwrightManager.shutdownBrowser();
        runtimeSwitch.lockTestingMode();
        log.warn("RPA testing lock engaged, pausedCampaigns={}", paused);

        Map<String, Object> result = status();
        result.put("pausedCampaigns", paused);
        result.put("message", "已暂停 " + paused + " 个寻源工作流，并锁定平台访问（测试模式）");
        return result;
    }

    /**
     * 开启真实平台联调（不自动恢复已暂停的工作流）。
     */
    public Map<String, Object> unlockLiveMode(RpaUnlockDTO dto) {
        if (dto == null || !Boolean.TRUE.equals(dto.getConfirm())) {
            throw new BizException("开启平台实况访问须显式确认（confirm=true）");
        }
        String reason = StringUtils.hasText(dto.getReason()) ? dto.getReason().trim() : "manual platform test";
        runtimeSwitch.unlockLiveMode(reason);
        log.warn("RPA live platform access unlocked, reason={}", reason);

        Map<String, Object> result = status();
        result.put("message", "已开启平台实况访问，可手动联调 Boss/猎聘。寻源工作流需自行恢复。");
        result.put("warning", "将真实访问招聘平台，请注意频率与账号安全");
        return result;
    }

    /**
     * 清除运行时覆盖，恢复为 application.yml 配置。
     */
    public Map<String, Object> resetToConfig() {
        playwrightManager.shutdownBrowser();
        runtimeSwitch.resetToConfig();
        log.info("RPA platform access reset to application.yml");

        Map<String, Object> result = status();
        result.put("message", "已恢复为配置文件默认的平台访问策略");
        return result;
    }

    public String blockedMessage() {
        if (runtimeSwitch.isTestingLocked()) {
            return "平台访问已一键锁定（测试模式），系统不会连接 Boss直聘/猎聘。"
                    + " 联调请调用 POST /api/agent/rpa/unlock-live";
        }
        return PlatformAccessGuard.BLOCKED_MESSAGE;
    }
}
