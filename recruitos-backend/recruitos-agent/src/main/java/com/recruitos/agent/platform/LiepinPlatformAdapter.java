package com.recruitos.agent.platform;

import com.recruitos.agent.entity.AgentAccount;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.List;

@Component
public class LiepinPlatformAdapter implements PlatformAdapter {

    @Resource
    private RpaPlatformBridge bridge;

    @Override
    public String platform() {
        return "LIEPIN";
    }

    @Override
    public void login(AgentAccount account) {
        bridge.login(account);
    }

    @Override
    public String publishJob(AgentAccount account, String jobTitle, String jdText, List<String> keywords) {
        return bridge.publishJob(account, jobTitle, jdText, keywords);
    }

    @Override
    public List<PlatformCandidate> searchCandidates(AgentAccount account, List<String> keywords, int limit) {
        return bridge.searchCandidates(account, keywords, limit);
    }

    @Override
    public void sendGreeting(AgentAccount account, String platformUserId, String candidateName, String jobTitle, String template) {
        bridge.sendGreeting(account, platformUserId, candidateName, jobTitle, template);
    }

    @Override
    public boolean hasResumeInChat(AgentAccount account, String platformUserId) {
        return bridge.hasResumeInChat(account, platformUserId);
    }

    @Override
    public PlatformResume fetchResume(AgentAccount account, String platformUserId, String candidateName) {
        return bridge.fetchResume(account, platformUserId, candidateName);
    }
}
