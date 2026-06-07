package com.recruitos.communication.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.recruitos.common.mybatis.BaseEntity;

import java.time.LocalDateTime;

/**
 * Safety log entity - records content safety checks on messages
 */
@TableName("safety_log")
public class SafetyLog extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /** Conversation ID */
    private Long conversationId;

    /** Message ID */
    private Long messageId;

    /** Check type: KEYWORD / AI / SENSITIVE */
    private String checkType;

    /** Check result: PASS / BLOCK / WARN */
    private String checkResult;

    /** Matched content */
    private String matchedContent;

    /** Risk level: LOW / MEDIUM / HIGH */
    private String riskLevel;

    /** Action taken: ALLOW / BLOCKED / REVIEW */
    private String action;

    /** Checked timestamp */
    private LocalDateTime checkedAt;

    // Getters and Setters

    public Long getConversationId() {
        return conversationId;
    }

    public void setConversationId(Long conversationId) {
        this.conversationId = conversationId;
    }

    public Long getMessageId() {
        return messageId;
    }

    public void setMessageId(Long messageId) {
        this.messageId = messageId;
    }

    public String getCheckType() {
        return checkType;
    }

    public void setCheckType(String checkType) {
        this.checkType = checkType;
    }

    public String getCheckResult() {
        return checkResult;
    }

    public void setCheckResult(String checkResult) {
        this.checkResult = checkResult;
    }

    public String getMatchedContent() {
        return matchedContent;
    }

    public void setMatchedContent(String matchedContent) {
        this.matchedContent = matchedContent;
    }

    public String getRiskLevel() {
        return riskLevel;
    }

    public void setRiskLevel(String riskLevel) {
        this.riskLevel = riskLevel;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public LocalDateTime getCheckedAt() {
        return checkedAt;
    }

    public void setCheckedAt(LocalDateTime checkedAt) {
        this.checkedAt = checkedAt;
    }
}
