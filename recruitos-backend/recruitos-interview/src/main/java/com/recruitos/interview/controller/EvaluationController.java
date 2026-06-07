package com.recruitos.interview.controller;

import com.recruitos.common.result.R;
import com.recruitos.interview.dto.EvaluationSubmitDTO;
import com.recruitos.interview.dto.InterviewVO;
import com.recruitos.interview.service.InterviewService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.Valid;

/**
 * Interview evaluation controller
 */
@Api(tags = "Interview Evaluation Management")
@RestController
@RequestMapping("/api/evaluation")
public class EvaluationController {

    @Resource
    private InterviewService interviewService;

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
}
