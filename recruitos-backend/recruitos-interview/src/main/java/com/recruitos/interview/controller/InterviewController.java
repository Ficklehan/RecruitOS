package com.recruitos.interview.controller;

import com.recruitos.common.result.PageResult;
import com.recruitos.common.result.R;
import com.recruitos.interview.dto.*;
import com.recruitos.interview.service.InterviewService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Interview management controller
 */
@Api(tags = "Interview Management")
@RestController
@RequestMapping("/api/interview")
public class InterviewController {

    @Resource
    private InterviewService interviewService;

    @ApiOperation("Arrange an interview")
    @PostMapping
    public R<InterviewVO> arrangeInterview(@Valid @RequestBody InterviewArrangeDTO dto) {
        InterviewVO vo = interviewService.arrangeInterview(dto);
        return R.ok(vo);
    }

    @ApiOperation("Get interview detail")
    @GetMapping("/{id}")
    public R<InterviewVO> getInterviewDetail(@PathVariable Long id) {
        InterviewVO vo = interviewService.getInterviewDetail(id);
        return R.ok(vo);
    }

    @ApiOperation("List interviews with pagination")
    @GetMapping("/list")
    public R<PageResult<InterviewVO>> listInterviews(InterviewQueryDTO query) {
        PageResult<InterviewVO> result = interviewService.listInterviews(query);
        return R.ok(result);
    }

    @ApiOperation("Confirm interview time slot")
    @PostMapping("/{id}/confirm")
    public R<InterviewVO> confirmInterview(@PathVariable Long id, @RequestParam Long slotId) {
        InterviewVO vo = interviewService.confirmInterview(id, slotId);
        return R.ok(vo);
    }

    @ApiOperation("Start interview")
    @PostMapping("/{id}/start")
    public R<InterviewVO> startInterview(@PathVariable Long id) {
        InterviewVO vo = interviewService.startInterview(id);
        return R.ok(vo);
    }

    @ApiOperation("Complete interview")
    @PostMapping("/{id}/complete")
    public R<InterviewVO> completeInterview(@PathVariable Long id) {
        InterviewVO vo = interviewService.completeInterview(id);
        return R.ok(vo);
    }

    @ApiOperation("Cancel interview")
    @PostMapping("/{id}/cancel")
    public R<InterviewVO> cancelInterview(@PathVariable Long id, @RequestParam(required = false) String reason) {
        InterviewVO vo = interviewService.cancelInterview(id, reason);
        return R.ok(vo);
    }

    @ApiOperation("Get interview calendar for an interviewer")
    @GetMapping("/calendar")
    public R<List<InterviewCalendarVO>> getInterviewCalendar(
            @RequestParam Long interviewerId,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startDate,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endDate) {
        List<InterviewCalendarVO> calendar = interviewService.getInterviewCalendar(interviewerId, startDate, endDate);
        return R.ok(calendar);
    }

    @ApiOperation("Trigger next round interview")
    @PostMapping("/{id}/trigger-next")
    public R<InterviewVO> triggerNextRound(@PathVariable Long id) {
        InterviewVO vo = interviewService.triggerNextRound(id);
        return R.ok(vo);
    }
}
