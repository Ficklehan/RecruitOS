package com.recruitos.agent.platform;

import com.recruitos.agent.entity.AgentAccount;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.Set;

/**
 * 模拟平台行为（RPA 关闭或 fallback 时使用）
 */
@Component
public class SimulatedPlatformSupport {

    private final Map<String, AtomicInteger> searchRounds = new ConcurrentHashMap<>();
    private final Set<String> resumeReadyUsers = ConcurrentHashMap.newKeySet();
    private final Map<String, Long> lastGreetAt = new ConcurrentHashMap<>();

    public void login(AgentAccount account) {
        // no-op
    }

    public String publishJob(AgentAccount account, String jobTitle, String jdText, List<String> keywords) {
        String base = "BOSS".equalsIgnoreCase(account.getPlatform())
                ? "https://www.zhipin.com" : "https://lpt.liepin.com";
        return base + "/job/" + System.currentTimeMillis();
    }

    public List<PlatformCandidate> searchCandidates(AgentAccount account, List<String> keywords, int limit) {
        return searchCandidates(account, keywords, limit, "RECOMMEND");
    }

    public List<PlatformCandidate> searchCandidates(AgentAccount account, List<String> keywords, int limit,
                                                    String searchSource) {
        String key = account.getPlatform() + ":" + account.getId() + ":" + searchSource;
        int round = searchRounds.computeIfAbsent(key, k -> new AtomicInteger(0)).incrementAndGet();
        List<PlatformCandidate> list = new ArrayList<>();
        int maxRounds = "LATEST".equalsIgnoreCase(searchSource) ? 2 : 3;
        if (round > maxRounds) {
            return list;
        }
        String prefix = account.getPlatform().toLowerCase();
        String kw = keywords.isEmpty() ? "工程师" : keywords.get(0);
        int batch = "SEARCH".equalsIgnoreCase(searchSource) ? 3 : 2;
        for (int i = 1; i <= Math.min(limit, batch); i++) {
            PlatformCandidate c = new PlatformCandidate();
            c.setPlatformUserId(prefix + "-" + searchSource.toLowerCase() + "-u" + round + i);
            c.setName(kw + "候选人" + round + "-" + i);
            c.setPhone("139" + String.format("%04d%04d", round, i));
            c.setEmail("c" + round + i + "@demo.recruitos.com");
            c.setCompany("示例科技" + i);
            c.setTitle(kw + "工程师");
            c.setWorkYears(3 + i);
            int baseScore = "RECOMMEND".equalsIgnoreCase(searchSource) ? 72 : 65;
            c.setMatchScore(new BigDecimal(baseScore + i * 4));
            list.add(c);
        }
        return list;
    }

    public void sendGreeting(AgentAccount account, String platformUserId, String candidateName,
                             String jobTitle, String template) {
        lastGreetAt.put(platformUserId, System.currentTimeMillis());
    }

    public void sendFollowUp(AgentAccount account, String platformUserId, String message) {
        lastGreetAt.put(platformUserId, System.currentTimeMillis());
        resumeReadyUsers.add(platformUserId);
    }

    public boolean hasResumeInChat(AgentAccount account, String platformUserId) {
        return resumeReadyUsers.contains(platformUserId);
    }

    public PlatformResume fetchResume(AgentAccount account, String platformUserId, String candidateName) {
        PlatformResume r = new PlatformResume();
        r.setFileName(candidateName + "-简历.pdf");
        r.setFileUrl("/storage/resumes/" + platformUserId + ".pdf");
        r.setRawText(candidateName + " 个人简历\n3年经验\n熟悉 " + account.getPlatform());
        r.setParsedJson("{\"basic\":{\"name\":\"" + candidateName + "\"},\"skills\":[\"" + account.getPlatform() + "\"]}");
        return r;
    }
}
