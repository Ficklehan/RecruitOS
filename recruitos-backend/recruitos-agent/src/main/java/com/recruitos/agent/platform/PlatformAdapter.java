package com.recruitos.agent.platform;

import com.recruitos.agent.entity.AgentAccount;

import java.util.List;

public interface PlatformAdapter {

    String platform();

    void login(AgentAccount account);

    String publishJob(AgentAccount account, String jobTitle, String jdText, List<String> keywords);

    List<PlatformCandidate> searchCandidates(AgentAccount account, List<String> keywords, int limit);

    default List<PlatformCandidate> searchCandidates(AgentAccount account, List<String> keywords,
                                                       int limit, String searchSource) {
        return searchCandidates(account, keywords, limit);
    }

    void sendGreeting(AgentAccount account, String platformUserId, String candidateName, String jobTitle, String template);

    void sendFollowUp(AgentAccount account, String platformUserId, String message);

    boolean hasResumeInChat(AgentAccount account, String platformUserId);

    PlatformResume fetchResume(AgentAccount account, String platformUserId, String candidateName);
}
