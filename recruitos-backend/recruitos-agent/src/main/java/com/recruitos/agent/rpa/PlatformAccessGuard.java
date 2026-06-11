package com.recruitos.agent.rpa;

import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Arrays;
import java.util.List;

/**
 * 平台实况访问闸：测试阶段默认禁止连接 Boss直聘 / 猎聘，仅允许模拟适配器。
 */
@Component
public class PlatformAccessGuard {

    public static final String BLOCKED_MESSAGE =
            "平台实况访问已关闭（测试模式），系统不会连接 Boss直聘/猎聘。"
                    + " 手动联调平台时请设置 recruitos.agent.rpa.platform-access-enabled=true";

    private static final List<String> BLOCKED_HOSTS = Arrays.asList(
            "zhipin.com",
            "liepin.com",
            "lpt.liepin.com",
            "passport.liepin.com"
    );

    @Resource
    private RpaProperties properties;
    @Resource
    private PlatformAccessRuntimeSwitch runtimeSwitch;

    public boolean isAccessAllowed() {
        return runtimeSwitch.isAccessAllowed(properties);
    }

    public void assertLiveAccessAllowed() {
        if (!isAccessAllowed()) {
            throw new RpaException(blockedMessage());
        }
    }

    public String blockedMessage() {
        if (runtimeSwitch.isTestingLocked()) {
            return "平台访问已一键锁定（测试模式），系统不会连接 Boss直聘/猎聘。"
                    + " 手动联调请在工作流/渠道页点击「开启平台联调」或调用 unlock-live 接口";
        }
        return BLOCKED_MESSAGE;
    }

    public void assertUrlAllowed(String url) {
        if (url == null || isAccessAllowed()) {
            return;
        }
        String lower = url.toLowerCase();
        for (String host : BLOCKED_HOSTS) {
            if (lower.contains(host)) {
                throw new RpaException(blockedMessage() + " 被拦截 URL: " + url);
            }
        }
    }
}
