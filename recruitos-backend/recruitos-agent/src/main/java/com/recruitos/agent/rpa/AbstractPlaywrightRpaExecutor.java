package com.recruitos.agent.rpa;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.LoadState;
import com.recruitos.agent.entity.AgentAccount;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Resource;
import java.nio.file.Path;
import java.time.LocalDateTime;

public abstract class AbstractPlaywrightRpaExecutor {

    protected final Logger log = LoggerFactory.getLogger(getClass());

    @Resource
    protected RpaProperties properties;
    @Resource
    protected PlaywrightManager playwrightManager;
    @Resource
    protected RpaSessionStorage sessionStorage;
    @Resource
    protected RpaCredentialParser credentialParser;
    @Resource
    protected PlatformAccessGuard platformAccessGuard;

    protected abstract String platform();

    protected abstract String recruiterHomeUrl();

    protected abstract boolean detectLoggedIn(Page page);

    protected abstract void performCredentialLogin(Page page, AgentAccount account, RpaCredential cred);

    public void login(AgentAccount account) {
        platformAccessGuard.assertLiveAccessAllowed();
        BrowserContext ctx = openContext(account, true);
        Page page = ctx.pages().isEmpty() ? ctx.newPage() : ctx.pages().get(0);
        page.setDefaultTimeout(properties.getActionTimeoutMs());
        page.navigate(recruiterHomeUrl());
        page.waitForLoadState(LoadState.DOMCONTENTLOADED);

        if (!detectLoggedIn(page)) {
            RpaCredential cred = credentialParser.parse(account);
            if ("phone".equalsIgnoreCase(cred.getAuthMode())
                    && cred.getPhone() != null && cred.getPassword() != null) {
                performCredentialLogin(page, account, cred);
            } else {
                waitForManualLogin(page, account);
            }
        }
        saveSession(account, ctx);
        account.setLastActiveAt(LocalDateTime.now());
    }

    public void ensureLoggedIn(AgentAccount account) {
        platformAccessGuard.assertLiveAccessAllowed();
        BrowserContext ctx = openContext(account, false);
        Page page = ctx.pages().isEmpty() ? ctx.newPage() : ctx.pages().get(0);
        page.setDefaultTimeout(properties.getActionTimeoutMs());
        page.navigate(recruiterHomeUrl());
        page.waitForLoadState(LoadState.DOMCONTENTLOADED);
        if (!detectLoggedIn(page)) {
            throw new RpaException(platform() + " 账号未登录，请先在「平台账号」执行「登录平台」");
        }
    }

    protected BrowserContext openContext(AgentAccount account, boolean forceNew) {
        if (!forceNew) {
            BrowserContext existing = playwrightManager.getContext(account.getId());
            if (existing != null) {
                return existing;
            }
        } else {
            playwrightManager.closeContext(account.getId());
        }

        Browser browser = playwrightManager.browser(properties);
        Browser.NewContextOptions opts = new Browser.NewContextOptions()
                .setViewportSize(1440, 900)
                .setLocale("zh-CN");
        Path session = sessionStorage.sessionFile(account);
        if (sessionStorage.hasSession(account)) {
            opts.setStorageStatePath(session);
        }
        BrowserContext ctx = browser.newContext(opts);
        playwrightManager.putContext(account.getId(), ctx);
        return ctx;
    }

    protected void saveSession(AgentAccount account, BrowserContext ctx) {
        try {
            ctx.storageState(new BrowserContext.StorageStateOptions()
                    .setPath(sessionStorage.sessionFile(account)));
        } catch (Exception e) {
            log.warn("Save session failed accountId={}: {}", account.getId(), e.getMessage());
        }
    }

    protected Page primaryPage(AgentAccount account) {
        BrowserContext ctx = openContext(account, false);
        return ctx.pages().isEmpty() ? ctx.newPage() : ctx.pages().get(0);
    }

    protected void waitForManualLogin(Page page, AgentAccount account) {
        String name = account != null ? account.getAccountName() : "unknown";
        log.info("{} manual login: complete login in browser window for account {}", platform(), name);
        long deadline = System.currentTimeMillis() + properties.getLoginTimeoutSeconds() * 1000L;
        while (System.currentTimeMillis() < deadline) {
            if (detectLoggedIn(page)) {
                return;
            }
            page.waitForTimeout(2000);
            try {
                page.reload();
                page.waitForLoadState(LoadState.DOMCONTENTLOADED);
            } catch (Exception ignored) {
                // continue polling
            }
        }
        throw new RpaException(platform() + " 登录超时，请在浏览器中完成扫码/验证后重试");
    }

    protected String textOrEmpty(com.microsoft.playwright.Locator locator) {
        try {
            if (locator.count() == 0) {
                return "";
            }
            return locator.first().innerText().trim();
        } catch (Exception e) {
            return "";
        }
    }
}
