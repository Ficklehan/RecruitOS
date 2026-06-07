package com.recruitos.communication.controller;

import com.recruitos.common.result.PageResult;
import com.recruitos.common.result.R;
import com.recruitos.communication.dto.ConversationQueryDTO;
import com.recruitos.communication.dto.ConversationVO;
import com.recruitos.communication.service.ConversationService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

/**
 * Conversation management controller
 */
@Api(tags = "Conversation Management")
@RestController
@RequestMapping("/api/communication/conversation")
public class ConversationController {

    @Resource
    private ConversationService conversationService;

    @ApiOperation("Get paginated conversation list")
    @GetMapping("/list")
    public R<PageResult<ConversationVO>> getConversationList(ConversationQueryDTO query) {
        PageResult<ConversationVO> result = conversationService.getConversationList(query);
        return R.ok(result);
    }

    @ApiOperation("Get conversation detail with messages")
    @GetMapping("/{id}")
    public R<ConversationVO> getConversationDetail(@PathVariable Long id) {
        ConversationVO vo = conversationService.getConversationDetail(id);
        return R.ok(vo);
    }

    @ApiOperation("Send a message in a conversation")
    @PostMapping("/{id}/send")
    public R<ConversationVO> sendMessage(
            @PathVariable Long id,
            @RequestParam String content,
            @RequestParam(required = false) Long templateId) {
        ConversationVO vo = conversationService.sendMessage(id, content, templateId);
        return R.ok(vo);
    }

    @ApiOperation("Close a conversation")
    @PostMapping("/{id}/close")
    public R<ConversationVO> closeConversation(@PathVariable Long id) {
        ConversationVO vo = conversationService.closeConversation(id);
        return R.ok(vo);
    }

    @ApiOperation("Get unread message count")
    @GetMapping("/unread-count")
    public R<Long> getUnreadCount() {
        long count = conversationService.getUnreadCount();
        return R.ok(count);
    }
}
