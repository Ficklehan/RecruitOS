package com.recruitos.agent.rpa.liepin;

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
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class LiepinRpaExecutor extends AbstractPlaywrightRpaExecutor implements PlatformRpaExecutor {

    private static final Pattern RES_ID = Pattern.compile("resId[=:](\\d+)", Pattern.CASE_INSENSITIVE);

    @Override
    protected String platform() {
        return "LIEPIN";
    }

    @Override
    protected String recruiterHomeUrl() {
        return "https://lpt.liepin.com/";
    }

    @Override
    protected boolean detectLoggedIn(Page page) {
        String url = page.url();
        if (url.contains("lpt.liepin.com") && !url.contains("login")) {
            return page.locator(".header-user, .user-name, .nav-user").count() > 0
                    || page.locator("a:has-text('职位'), a:has-text('人才')").count() > 0;
        }
        return false;
    }

    @Override
    protected void performCredentialLogin(Page page, AgentAccount account, RpaCredential cred) {
        page.navigate("https://passport.liepin.com/account/login");
        page.waitForLoadState(LoadState.DOMCONTENTLOADED);
        Locator phone = page.locator("input[name='user_login'], input[type='tel'], #loginName");
        Locator pwd = page.locator("input[type='password'], #password");
        if (phone.count() > 0) {
            phone.first().fill(cred.getPhone());
        }
        if (pwd.count() > 0) {
            pwd.first().fill(cred.getPassword());
        }
        Locator submit = page.locator("button:has-text('登录'), .btn-login");
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
        page.navigate("https://lpt.liepin.com/job/publish");
        page.waitForLoadState(LoadState.DOMCONTENTLOADED);

        fillIfPresent(page, "input[placeholder*='职位'], input[name='jobTitle']", jobTitle);
        String desc = jdText != null ? jdText : String.join("、", keywords);
        fillIfPresent(page, "textarea[placeholder*='描述'], textarea[name='jobDesc']", desc);

        Locator publish = page.locator("button:has-text('发布'), button:has-text('保存并发布')");
        if (publish.count() > 0) {
            publish.first().click();
            page.waitForTimeout(2000);
        }
        saveSession(account, page.context());
        return page.url();
    }

    public List<PlatformCandidate> searchCandidates(AgentAccount account, List<String> keywords, int limit) {
        ensureLoggedIn(account);
        Page page = primaryPage(account);
        String kw = keywords.isEmpty() ? "工程师" : keywords.get(0);
        page.navigate("https://lpt.liepin.com/cvsearch/showcondition/?keyword=" + encode(kw));
        page.waitForLoadState(LoadState.NETWORKIDLE);
        page.waitForTimeout(1500);

        List<PlatformCandidate> result = new ArrayList<>();
        Locator cards = page.locator(".resume-item, .search-resume-item, .candidate-card, li[data-resid]");
        int count = Math.min(cards.count(), limit);
        for (int i = 0; i < count; i++) {
            PlatformCandidate c = parseResumeCard(cards.nth(i), kw);
            if (c.getPlatformUserId() != null) {
                result.add(c);
            }
        }
        saveSession(account, page.context());
        return result;
    }

    public void sendGreeting(AgentAccount account, String platformUserId, String candidateName,
                             String jobTitle, String template) {
        ensureLoggedIn(account);
        Page page = primaryPage(account);
        page.navigate("https://lpt.liepin.com/im/showresume/?resId=" + platformUserId);
        page.waitForLoadState(LoadState.DOMCONTENTLOADED);

        String msg = template != null ? template : ("您好" + candidateName + "，我们在招聘「" + jobTitle + "」，方便沟通吗？");
        Locator greet = page.locator("button:has-text('打招呼'), .btn-sayhi");
        if (greet.count() > 0) {
            greet.first().click();
            page.waitForTimeout(800);
        }
        Locator input = page.locator("textarea, div[contenteditable='true'], .im-input textarea");
        if (input.count() > 0) {
            input.first().fill(msg);
            Locator send = page.locator("button:has-text('发送')");
            if (send.count() > 0) {
                send.first().click();
            }
        }
        saveSession(account, page.context());
    }

    public boolean hasResumeInChat(AgentAccount account, String platformUserId) {
        ensureLoggedIn(account);
        Page page = primaryPage(account);
        page.navigate("https://lpt.liepin.com/im/showresume/?resId=" + platformUserId);
        page.waitForLoadState(LoadState.DOMCONTENTLOADED);
        return page.locator("a:has-text('简历'), .resume-attachment, .attach-resume").count() > 0
                || page.locator(".resume-detail, .resume-content").count() > 0;
    }

    public PlatformResume fetchResume(AgentAccount account, String platformUserId, String candidateName) {
        ensureLoggedIn(account);
        Page page = primaryPage(account);
        page.navigate("https://lpt.liepin.com/resume/showresumedetail/?resId=" + platformUserId);
        page.waitForLoadState(LoadState.DOMCONTENTLOADED);

        PlatformResume resume = new PlatformResume();
        resume.setFileName(candidateName + "-liepin-resume.pdf");
        String body = textOrEmpty(page.locator(".resume-detail, .resume-content, .rd-info"));
        resume.setRawText(body);
        resume.setParsedJson("{\"basic\":{\"name\":\"" + escape(candidateName) + "\",\"platform\":\"LIEPIN\"}}");

        Locator downloadBtn = page.locator("a:has-text('下载'), button:has-text('下载简历')");
        if (downloadBtn.count() > 0) {
            Path dir = sessionStorage.downloadDir(account);
            try {
                Download download = page.waitForDownload(() -> downloadBtn.first().click());
                Path saved = dir.resolve(candidateName + "-" + platformUserId + ".pdf");
                download.saveAs(saved);
                resume.setFileUrl(saved.toAbsolutePath().toString());
            } catch (Exception e) {
                log.warn("Liepin resume download failed: {}", e.getMessage());
            }
        }
        saveSession(account, page.context());
        return resume;
    }

    private PlatformCandidate parseResumeCard(Locator card, String kw) {
        PlatformCandidate c = new PlatformCandidate();
        c.setName(textOrEmpty(card.locator(".name, .resume-name, h3")));
        c.setCompany(textOrEmpty(card.locator(".company, .comp-name")));
        c.setTitle(textOrEmpty(card.locator(".title, .job-title")));
        c.setMatchScore(new BigDecimal("72"));

        String resId = card.getAttribute("data-resid");
        if (resId == null) {
            try {
                String href = card.locator("a").first().getAttribute("href");
                Matcher m = RES_ID.matcher(href != null ? href : "");
                if (m.find()) {
                    resId = m.group(1);
                }
            } catch (Exception ignored) {
                // ignore
            }
        }
        c.setPlatformUserId(resId != null ? resId : ("liepin-unknown-" + System.currentTimeMillis()));
        if (c.getTitle().isEmpty()) {
            c.setTitle(kw);
        }
        return c;
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
