package com.recruitos.communication.service;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.List;

/**
 * 候选人回复拒绝关键词检测（命中则停止复聊并关闭会话）。
 */
@Component
public class CandidateReplyGuard {

    private static final List<String> DEFAULT_DECLINE_KEYWORDS = Arrays.asList(
            "不考虑", "不感兴趣", "请勿打扰", "别发了", "骚扰", "不需要", "拒绝");

    public boolean isDeclineReply(String content) {
        if (!StringUtils.hasText(content)) {
            return false;
        }
        String normalized = content.toLowerCase();
        for (String kw : DEFAULT_DECLINE_KEYWORDS) {
            if (normalized.contains(kw.toLowerCase())) {
                return true;
            }
        }
        return false;
    }
}
