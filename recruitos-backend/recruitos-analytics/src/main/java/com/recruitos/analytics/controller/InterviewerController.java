package com.recruitos.analytics.controller;

import com.recruitos.analytics.dto.InterviewerEfficiencyVO;
import com.recruitos.analytics.service.InterviewerService;
import com.recruitos.common.result.R;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.List;

/**
 * Interviewer efficiency analytics controller
 */
@Api(tags = "Interviewer Efficiency Analytics")
@RestController
@RequestMapping("/api/analytics/interviewer")
public class InterviewerController {

    @Resource
    private InterviewerService interviewerService;

    @ApiOperation("Get interviewer efficiency data")
    @GetMapping
    public R<List<InterviewerEfficiencyVO>> getInterviewerData() {
        List<InterviewerEfficiencyVO> list = interviewerService.getInterviewerData();
        return R.ok(list);
    }
}
