package com.recruitos.agent.rpa;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

@Component
public class RpaStartupNotice {

    private static final Logger log = LoggerFactory.getLogger(RpaStartupNotice.class);

    @Resource
    private RpaProperties rpaProperties;
    @Resource
    private PlatformAccessRuntimeSwitch runtimeSwitch;

    @EventListener(ApplicationReadyEvent.class)
    public void onReady() {
        if (!runtimeSwitch.isAccessAllowed(rpaProperties)) {
            log.warn("============================================================");
            log.warn(" RPA 测试模式：platform-access-enabled=false");
            log.warn(" 不会连接 Boss直聘 / 猎聘，寻源流程使用模拟数据");
            log.warn(" 手动联调平台请设置 recruitos.agent.rpa.platform-access-enabled=true");
            log.warn("============================================================");
        } else {
            log.warn(" RPA 平台实况访问已开启 — 将真实连接 Boss直聘 / 猎聘");
        }
    }
}
