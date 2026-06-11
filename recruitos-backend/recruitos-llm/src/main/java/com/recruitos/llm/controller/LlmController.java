package com.recruitos.llm.controller;

import com.recruitos.common.result.R;
import com.recruitos.llm.dto.LlmChatDTO;
import com.recruitos.llm.dto.LlmChatVO;
import com.recruitos.llm.service.LlmService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

@Api(tags = "LLM")
@RestController
@RequestMapping("/api/llm")
public class LlmController {

    @Resource
    private LlmService llmService;

    @ApiOperation("Chat completion (OpenAI-compatible gateway)")
    @PostMapping("/chat")
    public R<LlmChatVO> chat(@RequestBody LlmChatDTO dto) {
        return R.ok(llmService.chat(dto));
    }
}
