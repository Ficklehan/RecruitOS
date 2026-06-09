package com.recruitos.communication.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.recruitos.common.exception.BizException;
import com.recruitos.common.result.PageResult;
import com.recruitos.common.tenant.TenantContext;
import com.recruitos.communication.dto.ConversationMessageVO;
import com.recruitos.communication.dto.ConversationQueryDTO;
import com.recruitos.communication.dto.ConversationVO;
import com.recruitos.communication.entity.Conversation;
import com.recruitos.communication.entity.ConversationMessage;
import com.recruitos.communication.entity.MessageTemplate;
import com.recruitos.communication.mapper.ConversationMapper;
import com.recruitos.communication.mapper.ConversationMessageMapper;
import com.recruitos.communication.mapper.MessageTemplateMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Conversation service - business logic for conversation and message management
 */
@Service
public class ConversationService {

    @Resource
    private ConversationMapper conversationMapper;

    @Resource
    private ConversationMessageMapper messageMapper;

    @Resource
    private MessageTemplateMapper templateMapper;

    /**
     * Get paginated conversation list with filters
     */
    public PageResult<ConversationVO> getConversationList(ConversationQueryDTO query) {
        Long tenantId = TenantContext.getTenantId();

        Page<Conversation> page = new Page<>(query.getPageNum(), query.getPageSize());

        LambdaQueryWrapper<Conversation> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Conversation::getTenantId, tenantId);

        if (query.getCandidateId() != null) {
            wrapper.eq(Conversation::getCandidateId, query.getCandidateId());
        }
        if (query.getJobId() != null) {
            wrapper.eq(Conversation::getJobId, query.getJobId());
        }
        if (StringUtils.hasText(query.getChannel())) {
            wrapper.eq(Conversation::getChannel, query.getChannel());
        }
        if (StringUtils.hasText(query.getStatus())) {
            wrapper.eq(Conversation::getStatus, query.getStatus());
        }

        wrapper.orderByDesc(Conversation::getLastMessageAt);

        Page<Conversation> result = conversationMapper.selectPage(page, wrapper);

        List<ConversationVO> voList = new ArrayList<>();
        for (Conversation conversation : result.getRecords()) {
            voList.add(convertToVO(conversation, false));
        }

        return new PageResult<>(result.getTotal(), voList, query.getPageNum(), query.getPageSize());
    }

    /**
     * Get conversation detail by ID, including messages
     */
    public ConversationVO getConversationDetail(Long id) {
        Long tenantId = TenantContext.getTenantId();

        Conversation conversation = conversationMapper.selectById(id);
        if (conversation == null) {
            throw new BizException("Conversation not found");
        }
        if (!conversation.getTenantId().equals(tenantId)) {
            throw new BizException("Access denied");
        }

        return convertToVO(conversation, true);
    }

    /**
     * Send a message in a conversation
     */
    @Transactional
    public ConversationVO sendMessage(Long conversationId, String content, Long templateId) {
        Long tenantId = TenantContext.getTenantId();

        Conversation conversation = conversationMapper.selectById(conversationId);
        if (conversation == null) {
            throw new BizException("Conversation not found");
        }
        if (!conversation.getTenantId().equals(tenantId)) {
            throw new BizException("Access denied");
        }
        if ("CLOSED".equals(conversation.getStatus())) {
            throw new BizException("Cannot send message to a closed conversation");
        }

        // If templateId is provided, use template content
        String messageContent = content;
        if (templateId != null) {
            MessageTemplate template = templateMapper.selectById(templateId);
            if (template != null && template.getTenantId().equals(tenantId)) {
                messageContent = template.getContent();
                // Increment template usage count
                template.setSendCount(template.getSendCount() != null ? template.getSendCount() + 1 : 1);
                templateMapper.updateById(template);
            }
        }

        // Create message
        ConversationMessage message = new ConversationMessage();
        message.setTenantId(tenantId);
        message.setConversationId(conversationId);
        message.setSenderType("AGENT");
        message.setContent(messageContent);
        message.setTemplateId(templateId);
        message.setSentAt(LocalDateTime.now());
        message.setStatus("SENT");
        messageMapper.insert(message);

        // Update conversation
        conversation.setLastMessageAt(LocalDateTime.now());
        conversation.setMessageCount(conversation.getMessageCount() != null ? conversation.getMessageCount() + 1 : 1);
        conversationMapper.updateById(conversation);

        return convertToVO(conversation, true);
    }

    /**
     * Close a conversation
     */
    @Transactional
    public ConversationVO closeConversation(Long id) {
        Long tenantId = TenantContext.getTenantId();

        Conversation conversation = conversationMapper.selectById(id);
        if (conversation == null) {
            throw new BizException("Conversation not found");
        }
        if (!conversation.getTenantId().equals(tenantId)) {
            throw new BizException("Access denied");
        }
        if ("CLOSED".equals(conversation.getStatus())) {
            throw new BizException("Conversation is already closed");
        }

        conversation.setStatus("CLOSED");
        conversationMapper.updateById(conversation);

        return convertToVO(conversation, false);
    }

    /**
     * Get count of unread messages across all conversations
     */
    public long getUnreadCount() {
        Long tenantId = TenantContext.getTenantId();

        // Count messages with status SENT or DELIVERED that are from CANDIDATE
        LambdaQueryWrapper<ConversationMessage> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ConversationMessage::getTenantId, tenantId)
                .eq(ConversationMessage::getSenderType, "CANDIDATE")
                .in(ConversationMessage::getStatus, "SENT", "DELIVERED");

        return messageMapper.selectCount(wrapper);
    }

    /**
     * Convert Conversation entity to ConversationVO
     * @param includeMessages whether to load and include messages
     */
    private ConversationVO convertToVO(Conversation conversation, boolean includeMessages) {
        ConversationVO vo = new ConversationVO();
        vo.setId(conversation.getId());
        vo.setCandidateId(conversation.getCandidateId());
        vo.setCandidateName(conversation.getCandidateName());
        vo.setJobId(conversation.getJobId());
        vo.setJobTitle(conversation.getJobTitle());
        vo.setChannel(conversation.getChannel());
        vo.setStatus(conversation.getStatus());
        vo.setLastMessageAt(conversation.getLastMessageAt());
        vo.setMessageCount(conversation.getMessageCount());
        vo.setCreatedBy(conversation.getCreatedBy());
        vo.setCreatedAt(conversation.getCreatedAt());
        vo.setUpdatedAt(conversation.getUpdatedAt());

        if (includeMessages) {
            LambdaQueryWrapper<ConversationMessage> msgWrapper = new LambdaQueryWrapper<>();
            msgWrapper.eq(ConversationMessage::getConversationId, conversation.getId())
                    .orderByAsc(ConversationMessage::getSentAt);
            List<ConversationMessage> messages = messageMapper.selectList(msgWrapper);

            List<ConversationMessageVO> messageVOs = new ArrayList<>();
            for (ConversationMessage msg : messages) {
                messageVOs.add(convertMessageToVO(msg));
            }
            vo.setMessages(messageVOs);
        }

        return vo;
    }

    /**
     * Convert ConversationMessage entity to ConversationMessageVO
     */
    private ConversationMessageVO convertMessageToVO(ConversationMessage message) {
        ConversationMessageVO vo = new ConversationMessageVO();
        vo.setId(message.getId());
        vo.setConversationId(message.getConversationId());
        vo.setSenderType(message.getSenderType());
        vo.setContent(message.getContent());
        vo.setTemplateId(message.getTemplateId());
        vo.setSentAt(message.getSentAt());
        vo.setReadAt(message.getReadAt());
        vo.setStatus(message.getStatus());
        vo.setCreatedAt(message.getCreatedAt());
        return vo;
    }
}
