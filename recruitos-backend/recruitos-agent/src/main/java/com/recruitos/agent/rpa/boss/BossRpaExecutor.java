package com.recruitos.agent.rpa.boss;

import com.microsoft.playwright.Download;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.LoadState;
import com.recruitos.agent.entity.AgentAccount;
import com.recruitos.agent.platform.PlatformCandidate;
import com.recruitos.agent.platform.PlatformResume;
import com.recruitos.agent.rpa.AbstractPlaywrightRpaExecutor;
import com.recruitos.agent.rpa.PlatformRpaExecutor;
import com.recruitos.agent.rpa.RpaCredential;
import com.recruitos.agent.rpa.RpaException;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class BossRpaExecutor extends AbstractPlaywrightRpaExecutor implements PlatformRpaExecutor {

    private static final Pattern GEEK_ID = Pattern.compile("geekId[=:](\\d+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern SECURITY_ID = Pattern.compile("securityId=([^&\"']+)");

    @Override
    protected String platform() {
        return "BOSS";
    }

    @Override
    protected String recruiterHomeUrl() {
        return "https://www.zhipin.com/web/chat/index";
    }

    @Override
    protected boolean detectLoggedIn(Page page) {
        String url = page.url();
        if (url.contains("/web/chat/") || url.contains("/web/geek/")) {
            return true;
        }
        return page.locator(".nav-figure, .user-nav, [ka='header-username'], .label-name").count() > 0;
    }

    @Override
    protected void performCredentialLogin(Page page, AgentAccount account, RpaCredential cred) {
        page.navigate("https://www.zhipin.com/web/user/?ka=header-login");
        page.waitForLoadState(LoadState.DOMCONTENTLOADED);
        Locator phone = page.locator("input[type='tel'], input[name='phone'], .ipt-phone input");
        Locator pwd = page.locator("input[type='password'], input[name='password']");
        if (phone.count() > 0) {
            phone.first().fill(cred.getPhone());
        }
        if (pwd.count() > 0) {
            pwd.first().fill(cred.getPassword());
        }
        Locator submit = page.locator("button:has-text('登录'), .btn-login, [ka='login-submit']");
        if (submit.count() > 0) {
            submit.first().click();
        }
        page.waitForTimeout(3000);
        if (!detectLoggedIn(page)) {
            waitForManualLogin(page, account);
        }
    }

    public String publishJob(AgentAccount account, String jobTitle, String jdText, List<String> keywords) {
        ensureLoggedIn(account);
        Page page = primaryPage(account);
        page.navigate("https://www.zhipin.com/web/chat/job/list");
        page.waitForLoadState(LoadState.DOMCONTENTLOADED);

        Locator publishBtn = page.locator("a:has-text('发布职位'), button:has-text('发布职位'), .job-add-btn");
        if (publishBtn.count() > 0) {
            publishBtn.first().click();
            page.waitForTimeout(1500);
        }

        fillIfPresent(page, "input[placeholder*='职位名称'], input[name='jobName'], .job-name-input input", jobTitle);
        String desc = jdText != null ? jdText : String.join("、", keywords);
        fillIfPresent(page, "textarea[placeholder*='描述'], textarea[name='description'], .job-desc textarea", desc);

        Locator confirm = page.locator("button:has-text('发布'), button:has-text('确认发布')");
        if (confirm.count() > 0) {
            confirm.first().click();
            page.waitForTimeout(2000);
        }
        saveSession(account, page.context());
        return page.url();
    }

    public List<PlatformCandidate> searchCandidates(AgentAccount account, List<String> keywords, int limit) {
        ensureLoggedIn(account);
        Page page = primaryPage(account);
        String kw = keywords.isEmpty() ? "工程师" : keywords.get(0);
        String url = "https://www.zhipin.com/web/geek/search?query=" + encode(kw);
        page.navigate(url);
        page.waitForLoadState(LoadState.NETWORKIDLE);
        page.waitForTimeout(1500);

        List<PlatformCandidate> result = new ArrayList<>();
        Locator cards = page.locator(".geek-info-card, .card-inner, .candidate-card, li.geek-item");
        int count = Math.min(cards.count(), limit);
        for (int i = 0; i < count; i++) {
            Locator card = cards.nth(i);
            PlatformCandidate c = parseGeekCard(card, kw);
            if (c.getPlatformUserId() != null) {
                result.add(c);
            }
        }

        if (result.isEmpty()) {
            page.navigate("https://www.zhipin.com/web/geek/recommend");
            page.waitForLoadState(LoadState.NETWORKIDLE);
            cards = page.locator(".geek-info-card, .card-inner, .candidate-card, li.geek-item");
            count = Math.min(cards.count(), limit);
            for (int i = 0; i < count; i++) {
                PlatformCandidate c = parseGeekCard(cards.nth(i), kw);
                if (c.getPlatformUserId() != null) {
                    result.add(c);
                }
            }
        }
        saveSession(account, page.context());
        return result;
    }

    public void sendGreeting(AgentAccount account, String platformUserId, String candidateName,
                             String jobTitle, String template) {
        ensureLoggedIn(account);
        Page page = primaryPage(account);
        String chatUrl = "https://www.zhipin.com/web/geek/chat?geekId=" + platformUserId;
        page.navigate(chatUrl);
        page.waitForLoadState(LoadState.DOMCONTENTLOADED);
        page.waitForTimeout(1000);

        String msg = template != null ? template : ("您好" + candidateName + "，我们在招聘「" + jobTitle + "」，方便沟通吗？");
        Locator greetBtn = page.locator("button:has-text('打招呼'), .btn-greet, [ka='geek_greet_btn']");
        if (greetBtn.count() > 0) {
            greetBtn.first().click();
            page.waitForTimeout(800);
        }

        Locator input = page.locator("div[contenteditable='true'], textarea.input-area, .chat-input textarea, .im-input textarea");
        if (input.count() > 0) {
            input.first().click();
            input.first().fill(msg);
            Locator send = page.locator("button:has-text('发送'), .btn-send, [ka='im_send']");
            if (send.count() > 0) {
                send.first().click();
            } else {
                page.keyboard().press("Enter");
            }
        }
        saveSession(account, page.context());
    }

    public boolean hasResumeInChat(AgentAccount account, String platformUserId) {
        ensureLoggedIn(account);
        Page page = primaryPage(account);
        page.navigate("https://www.zhipin.com/web/geek/chat?geekId=" + platformUserId);
        page.waitForLoadState(LoadState.DOMCONTENTLOADED);
        Locator resume = page.locator(
                "a:has-text('简历'), .message-card .btn, .file-resume, [class*='resume'], .msg-card-resume");
        return resume.count() > 0;
    }

    public PlatformResume fetchResume(AgentAccount account, String platformUserId, String candidateName) {
        ensureLoggedIn(account);
        Page page = primaryPage(account);
        page.navigate("https://www.zhipin.com/web/geek/chat?geekId=" + platformUserId);
        page.waitForLoadState(LoadState.DOMCONTENTLOADED);

        PlatformResume resume = new PlatformResume();
        resume.setFileName(candidateName + "-boss-resume.pdf");

        Locator resumeBtn = page.locator(
                "a:has-text('查看简历'), a:has-text('附件简历'), .file-resume, .message-card .btn:has-text('简历')");
        if (resumeBtn.count() == 0) {
            resume.setRawText(extractChatText(page));
            resume.setParsedJson("{\"basic\":{\"name\":\"" + escape(candidateName) + "\"}}");
            return resume;
        }

        Path dir = sessionStorage.downloadDir(account);
        try {
            Download download = page.waitForDownload(() -> resumeBtn.first().click());
            Path saved = dir.resolve(candidateName + "-" + platformUserId + ".pdf");
            download.saveAs(saved);
            resume.setFileUrl(saved.toAbsolutePath().toString());
        } catch (Exception e) {
            log.warn("Boss resume download failed: {}", e.getMessage());
            resume.setRawText(extractChatText(page));
        }
        resume.setParsedJson("{\"basic\":{\"name\":\"" + escape(candidateName) + "\",\"platform\":\"BOSS\"}}");
        saveSession(account, page.context());
        return resume;
    }

    private PlatformCandidate parseGeekCard(Locator card, String kw) {
        PlatformCandidate c = new PlatformCandidate();
        String name = textOrEmpty(card.locator(".name, .geek-name, h3, .title"));
        c.setName(name.isEmpty() ? "候选人" : name);
        c.setCompany(textOrEmpty(card.locator(".company, .company-name, .user-company")));
        c.setTitle(textOrEmpty(card.locator(".position, .job-title, .title-sub")));
        c.setWorkYears(parseWorkYears(textOrEmpty(card.locator(".work-year, .experience"))));
        c.setMatchScore(new BigDecimal("75"));

        String href = "";
        try {
            Locator link = card.locator("a[href*='geek'], a[href*='securityId']");
            if (link.count() > 0) {
                href = link.first().getAttribute("href");
            }
        } catch (Exception ignored) {
            // ignore
        }
        String geekId = extractGeekId(href);
        if (geekId == null) {
            geekId = extractGeekId(card.getAttribute("data-geekid"));
        }
        c.setPlatformUserId(geekId != null ? geekId : ("boss-unknown-" + System.currentTimeMillis()));
        if (c.getTitle().isEmpty()) {
            c.setTitle(kw + "相关");
        }
        return c;
    }

    private String extractGeekId(String text) {
        if (text == null) {
            return null;
        }
        Matcher m = GEEK_ID.matcher(text);
        if (m.find()) {
            return m.group(1);
        }
        Matcher s = SECURITY_ID.matcher(text);
        if (s.find()) {
            return s.group(1);
        }
        return null;
    }

    private int parseWorkYears(String text) {
        if (text == null) {
            return 0;
        }
        Matcher m = Pattern.compile("(\\d+)").matcher(text);
        if (m.find()) {
            return Integer.parseInt(m.group(1));
        }
        return 0;
    }

    private void fillIfPresent(Page page, String selector, String value) {
        if (value == null) {
            return;
        }
        Locator loc = page.locator(selector);
        if (loc.count() > 0) {
            loc.first().fill(value);
        }
    }

    private String extractChatText(Page page) {
        return textOrEmpty(page.locator(".chat-message, .message-content, .im-list"));
    }

    private String encode(String s) {
        try {
            return java.net.URLEncoder.encode(s, "UTF-8");
        } catch (Exception e) {
            return s;
        }
    }

    private String escape(String s) {
        return s == null ? "" : s.replace("\"", "'");
    }
}
