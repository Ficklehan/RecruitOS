package com.recruitos.communication.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.recruitos.common.mybatis.BaseEntity;

import java.time.LocalDateTime;

/**
 * Conversation message entity
 */
@TableName("conversation_message")
public class ConversationMessage extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /** Conversation ID */
    private Long conversationId;

    /** Sender type: SYSTEM / CANDIDATE / AGENT */
    private String senderType;

    /** Message content */
    private String content;

    /** Template ID (if sent from a template) */
    private Long templateId;

    /** Sent timestamp */
    private LocalDateTime sentAt;

    /** Read timestamp */
    private LocalDateTime readAt;

    /** Status: SENT / DELIVERED / READ / FAILED */
    private String status;

    // Getters and Setters

    public Long getConversationId() {
        return conversationId;
    }

    public void setConversationId(Long conversationId) {
        this.conversationId = conversationId;
    }

    public String getSenderType() {
        return senderType;
    }

    public void setSenderType(String senderType) {
        this.senderType = senderType;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Long getTemplateId() {
        return templateId;
    }

    public void setTemplateId(Long templateId) {
        this.templateId = templateId;
    }

    public LocalDateTime getSentAt() {
        return sentAt;
    }

    public void setSentAt(LocalDateTime sentAt) {
        this.sentAt = sentAt;
    }

    public LocalDateTime getReadAt() {
        return readAt;
    }

    public void setReadAt(LocalDateTime readAt) {
        this.readAt = readAt;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
