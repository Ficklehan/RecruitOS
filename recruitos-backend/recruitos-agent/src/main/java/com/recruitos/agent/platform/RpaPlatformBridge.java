package com.recruitos.agent.platform;

import com.recruitos.agent.entity.AgentAccount;
import com.recruitos.agent.rpa.PlatformRpaExecutor;
import com.recruitos.agent.rpa.RpaException;
import com.recruitos.agent.rpa.RpaProperties;
import com.recruitos.agent.rpa.boss.BossRpaExecutor;
import com.recruitos.agent.rpa.liepin.LiepinRpaExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.List;
import java.util.function.Supplier;

@Component
public class RpaPlatformBridge {

    private static final Logger log = LoggerFactory.getLogger(RpaPlatformBridge.class);

    @Resource
    private RpaProperties properties;
    @Resource
    private com.recruitos.agent.rpa.PlatformAccessRuntimeSwitch runtimeSwitch;
    @Resource
    private SimulatedPlatformSupport simulated;
    @Resource
    private BossRpaExecutor bossRpa;
    @Resource
    private LiepinRpaExecutor liepinRpa;

    public void login(AgentAccount account) {
        run(account, () -> {
            executor(account).login(account);
            return null;
        }, () -> {
            simulated.login(account);
            return null;
        });
    }

    public String publishJob(AgentAccount account, String jobTitle, String jdText, List<String> keywords) {
        return run(account,
                () -> executor(account).publishJob(account, jobTitle, jdText, keywords),
                () -> simulated.publishJob(account, jobTitle, jdText, keywords));
    }

    public List<PlatformCandidate> searchCandidates(AgentAccount account, List<String> keywords, int limit) {
        return searchCandidates(account, keywords, limit, "RECOMMEND");
    }

    public List<PlatformCandidate> searchCandidates(AgentAccount account, List<String> keywords, int limit,
                                                    String searchSource) {
        return run(account,
                () -> executor(account).searchCandidates(account, keywords, limit),
                () -> simulated.searchCandidates(account, keywords, limit, searchSource));
    }

    public void sendGreeting(AgentAccount account, String platformUserId, String candidateName,
                             String jobTitle, String template) {
        run(account, () -> {
            executor(account).sendGreeting(account, platformUserId, candidateName, jobTitle, template);
            return null;
        }, () -> {
            simulated.sendGreeting(account, platformUserId, candidateName, jobTitle, template);
            return null;
        });
    }

    public void sendFollowUp(AgentAccount account, String platformUserId, String message) {
        run(account, () -> {
            executor(account).sendFollowUp(account, platformUserId, message);
            return null;
        }, () -> {
            simulated.sendFollowUp(account, platformUserId, message);
            return null;
        });
    }

    public boolean hasResumeInChat(AgentAccount account, String platformUserId) {
        return run(account,
                () -> executor(account).hasResumeInChat(account, platformUserId),
                () -> simulated.hasResumeInChat(account, platformUserId));
    }

    public PlatformResume fetchResume(AgentAccount account, String platformUserId, String candidateName) {
        return run(account,
                () -> executor(account).fetchResume(account, platformUserId, candidateName),
                () -> simulated.fetchResume(account, platformUserId, candidateName));
    }

    private PlatformRpaExecutor executor(AgentAccount account) {
        if ("BOSS".equalsIgnoreCase(account.getPlatform())) {
            return bossRpa;
        }
        if ("LIEPIN".equalsIgnoreCase(account.getPlatform())) {
            return liepinRpa;
        }
        throw new RpaException("平台 " + account.getPlatform() + " 暂未实现真实 RPA");
    }

    private <T> T run(AgentAccount account, Supplier<T> rpa, Supplier<T> sim) {
        if (!runtimeSwitch.isAccessAllowed(properties)) {
            return sim.get();
        }
        try {
            return rpa.get();
        } catch (Exception e) {
            log.warn("RPA {} failed for account {}: {}", account.getPlatform(), account.getId(), e.getMessage());
            if (properties.isFallbackSimulated()) {
                return sim.get();
            }
            if (e instanceof RpaException) {
                throw (RpaException) e;
            }
            throw new RpaException(e.getMessage(), e);
        }
    }
}
