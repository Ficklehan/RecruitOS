package com.recruitos.interview.controller;

import com.recruitos.common.result.R;
import com.recruitos.common.llm.LlmChatRequest;
import com.recruitos.common.llm.LlmClient;
import com.recruitos.interview.dto.EvaluationSubmitDTO;
import com.recruitos.interview.dto.InterviewVO;
import com.recruitos.interview.service.InterviewService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.util.*;

/**
 * Interview evaluation controller
 */
@Api(tags = "Interview Evaluation Management")
@RestController
@RequestMapping("/api/evaluation")
public class EvaluationController {

    @Resource
    private InterviewService interviewService;
    @Resource
    private LlmClient llmClient;

    @ApiOperation("Submit interview evaluation")
    @PostMapping
    public R<InterviewVO> submitEvaluation(@Valid @RequestBody EvaluationSubmitDTO dto) {
        InterviewVO vo = interviewService.submitEvaluation(dto);
        return R.ok(vo);
    }

    @ApiOperation("Get evaluation by interview ID")
    @GetMapping("/interview/{interviewId}")
    public R<InterviewVO> getEvaluationByInterviewId(@PathVariable Long interviewId) {
        InterviewVO vo = interviewService.getInterviewDetail(interviewId);
        return R.ok(vo);
    }

    @ApiOperation("Generate AI follow-up questions for an interview")
    @GetMapping("/interview/{interviewId}/ai-questions")
    public R<Map<String, Object>> getAiQuestions(@PathVariable Long interviewId) {
        InterviewVO interview = interviewService.getInterviewDetail(interviewId);
        if (interview == null) {
            return R.fail("面试不存在");
        }

        StringBuilder ctx = new StringBuilder();
        ctx.append("候选人: ").append(interview.getCandidateName() != null ? interview.getCandidateName() : "未知").append("\n");
        ctx.append("岗位: ").append(interview.getJobTitle() != null ? interview.getJobTitle() : "未知").append("\n");
        ctx.append("面试轮次: ").append(interview.getRound()).append("\n");

        LlmChatRequest req = new LlmChatRequest();
        req.setSystemPrompt("你是资深面试官。根据候选人简历和岗位要求，生成3-5个有针对性的追问问题。"
            + "每个问题应包含：问题内容、考察维度、追问目的。输出JSON数组格式。");
        req.setUserPrompt(ctx.toString());
        req.setScenario("interview_questions");
        String llmResult = llmClient.chat(req);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("interviewId", interviewId);
        result.put("candidateName", interview.getCandidateName());
        result.put("jobTitle", interview.getJobTitle());
        result.put("questions", llmResult);
        return R.ok(result);
    }
}
