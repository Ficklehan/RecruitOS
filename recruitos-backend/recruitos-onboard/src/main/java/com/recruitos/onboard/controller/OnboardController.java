package com.recruitos.onboard.controller;

import com.recruitos.common.result.PageResult;
import com.recruitos.common.result.R;
import com.recruitos.onboard.dto.*;
import com.recruitos.onboard.service.OnboardService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

/**
 * Onboard management controller
 */
@Api(tags = "Onboard Management")
@RestController
@RequestMapping("/api/onboard")
public class OnboardController {

    @Resource
    private OnboardService onboardService;

    @ApiOperation("Create an onboard record")
    @PostMapping
    public R<OnboardVO> createOnboard(@RequestBody OnboardCreateDTO dto) {
        OnboardVO vo = onboardService.createOnboard(dto);
        return R.ok(vo);
    }

    @ApiOperation("Get onboard list with pagination")
    @GetMapping("/list")
    public R<PageResult<OnboardVO>> getOnboardList(OnboardQueryDTO query) {
        PageResult<OnboardVO> result = onboardService.getOnboardList(query);
        return R.ok(result);
    }

    @ApiOperation("Get onboard detail")
    @GetMapping("/{id}")
    public R<OnboardVO> getOnboardDetail(@PathVariable Long id) {
        OnboardVO vo = onboardService.getOnboardDetail(id);
        return R.ok(vo);
    }

    @ApiOperation("Update onboard status")
    @PutMapping("/{id}/status")
    public R<OnboardVO> updateOnboardStatus(@PathVariable Long id, @RequestParam String status) {
        OnboardVO vo = onboardService.updateOnboardStatus(id, status);
        return R.ok(vo);
    }

    @ApiOperation("Create a task for an onboard record")
    @PostMapping("/task")
    public R<OnboardTaskVO> createTask(@RequestBody OnboardTaskCreateDTO dto) {
        OnboardTaskVO vo = onboardService.createTask(dto);
        return R.ok(vo);
    }

    @ApiOperation("Update task status")
    @PutMapping("/task/{taskId}/status")
    public R<OnboardTaskVO> updateTaskStatus(@PathVariable Long taskId, @RequestParam String status) {
        OnboardTaskVO vo = onboardService.updateTaskStatus(taskId, status);
        return R.ok(vo);
    }

    @ApiOperation("Get tasks for an onboard record")
    @GetMapping("/{onboardId}/tasks")
    public R<List<OnboardTaskVO>> getTaskList(@PathVariable Long onboardId) {
        List<OnboardTaskVO> tasks = onboardService.getTaskList(onboardId);
        return R.ok(tasks);
    }
}
