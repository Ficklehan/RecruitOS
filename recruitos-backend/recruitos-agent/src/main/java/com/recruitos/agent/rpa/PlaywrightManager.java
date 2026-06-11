package com.recruitos.agent.rpa;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Playwright;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.annotation.PreDestroy;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 单例 Playwright + 按账号复用 Browser 实例
 */
@Component
public class PlaywrightManager {

    private static final Logger log = LoggerFactory.getLogger(PlaywrightManager.class);

    @javax.annotation.Resource
    private PlatformAccessGuard platformAccessGuard;

    private final Object lock = new Object();
    private Playwright playwright;
    private Browser browser;

    private final ConcurrentMap<Long, com.microsoft.playwright.BrowserContext> contexts = new ConcurrentHashMap<>();

    public Playwright playwright() {
        ensureReady();
        return playwright;
    }

    public Browser browser(RpaProperties props) {
        platformAccessGuard.assertLiveAccessAllowed();
        ensureReady();
        if (browser == null || !browser.isConnected()) {
            BrowserType.LaunchOptions opts = new BrowserType.LaunchOptions()
                    .setHeadless(props.isHeadless())
                    .setSlowMo(props.getSlowMoMs());
            browser = playwright.chromium().launch(opts);
        }
        return browser;
    }

    public com.microsoft.playwright.BrowserContext getContext(Long accountId) {
        return contexts.get(accountId);
    }

    public void putContext(Long accountId, com.microsoft.playwright.BrowserContext ctx) {
        contexts.put(accountId, ctx);
    }

    public void closeContext(Long accountId) {
        com.microsoft.playwright.BrowserContext ctx = contexts.remove(accountId);
        if (ctx != null) {
            try {
                ctx.close();
            } catch (Exception e) {
                log.debug("Close context failed accountId={}: {}", accountId, e.getMessage());
            }
        }
    }

    /** 关闭全部浏览器上下文与 Browser 实例（锁定测试模式时调用） */
    public void shutdownBrowser() {
        for (Long id : contexts.keySet()) {
            closeContext(id);
        }
        synchronized (lock) {
            if (browser != null) {
                try {
                    browser.close();
                } catch (Exception e) {
                    log.debug("Close browser failed: {}", e.getMessage());
                }
                browser = null;
            }
        }
    }

    private void ensureReady() {
        synchronized (lock) {
            if (playwright == null) {
                playwright = Playwright.create();
            }
        }
    }

    @PreDestroy
    public void shutdown() {
        for (Long id : contexts.keySet()) {
            closeContext(id);
        }
        if (browser != null) {
            try {
                browser.close();
            } catch (Exception ignored) {
                // ignore
            }
        }
        if (playwright != null) {
            try {
                playwright.close();
            } catch (Exception ignored) {
                // ignore
            }
        }
    }
}
