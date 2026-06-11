package com.recruitos.communication.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.recruitos.common.evolution.ModuleEvolutionEmitter;
import com.recruitos.common.tenant.TenantContext;
import com.recruitos.communication.entity.Conversation;
import com.recruitos.communication.entity.ConversationMessage;
import com.recruitos.communication.mapper.ConversationMapper;
import com.recruitos.communication.mapper.ConversationMessageMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 扫描沉默对话：触发复聊消息 + L4 进化信号（Phase 2 T4.2）。
 */
@Service
public class ConversationSilenceScheduler {

    private static final Logger log = LoggerFactory.getLogger(ConversationSilenceScheduler.class);

    @Value("${recruitos.communication.rechat.interval-hours:48}")
    private int intervalHours;

    @Value("${recruitos.communication.rechat.max-attempts:2}")
    private int maxAttempts;

    @Resource
    private ConversationMapper conversationMapper;

    @Resource
    private ConversationMessageMapper messageMapper;

    @Resource
    private ConversationService conversationService;

    @Resource
    private ModuleEvolutionEmitter moduleEvolutionEmitter;

    @Scheduled(fixedDelay = 120000)
    public void scanSilentConversations() {
        LocalDateTime threshold = LocalDateTime.now().minusHours(intervalHours);
        LambdaQueryWrapper<Conversation> w = new LambdaQueryWrapper<>();
        w.eq(Conversation::getStatus, "ACTIVE")
                .lt(Conversation::getLastMessageAt, threshold)
                .isNotNull(Conversation::getLastMessageAt);
        List<Conversation> candidates = conversationMapper.selectList(w);
        for (Conversation c : candidates) {
            try {
                TenantContext.setTenantId(c.getTenantId());
                processOne(c);
            } catch (Exception e) {
                log.warn("Silent conversation scan failed id={}: {}", c.getId(), e.getMessage());
            } finally {
                TenantContext.clear();
            }
        }
    }

    private void processOne(Conversation conversation) {
        List<ConversationMessage> messages = listMessagesDesc(conversation.getId());
        if (messages.isEmpty()) {
            return;
        }
        ConversationMessage latest = messages.get(0);
        if (!"AGENT".equals(latest.getSenderType())) {
            return;
        }
        int consecutiveAgent = 0;
        for (ConversationMessage m : messages) {
            if ("AGENT".equals(m.getSenderType())) {
                consecutiveAgent++;
            } else {
                break;
            }
        }
        if (consecutiveAgent > maxAttempts) {
            moduleEvolutionEmitter.emitCandidateSilent(
                    conversation.getJobId(), conversation.getCandidateId(), conversation.getId());
            return;
        }
        if (consecutiveAgent >= maxAttempts) {
            return;
        }
        String rechatContent = "您好，想再跟您确认一下是否方便聊聊这个机会？如有兴趣欢迎回复～";
        conversationService.sendMessageInternal(conversation, rechatContent, null, false);
        moduleEvolutionEmitter.emitRechatSent(
                conversation.getJobId(),
                conversation.getCandidateId(),
                conversation.getId(),
                consecutiveAgent);
    }

    private List<ConversationMessage> listMessagesDesc(Long conversationId) {
        LambdaQueryWrapper<ConversationMessage> w = new LambdaQueryWrapper<>();
        w.eq(ConversationMessage::getConversationId, conversationId)
                .orderByDesc(ConversationMessage::getSentAt);
        return messageMapper.selectList(w);
    }
}
