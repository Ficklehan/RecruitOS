package com.recruitos.agent.rpa;

import com.recruitos.agent.entity.AgentAccount;
import com.recruitos.agent.platform.PlatformCandidate;
import com.recruitos.agent.platform.PlatformResume;

import java.util.List;

public interface PlatformRpaExecutor {

    void login(AgentAccount account);

    String publishJob(AgentAccount account, String jobTitle, String jdText, List<String> keywords);

    List<PlatformCandidate> searchCandidates(AgentAccount account, List<String> keywords, int limit);

    void sendGreeting(AgentAccount account, String platformUserId, String candidateName, String jobTitle, String template);

    boolean hasResumeInChat(AgentAccount account, String platformUserId);

    PlatformResume fetchResume(AgentAccount account, String platformUserId, String candidateName);
}
