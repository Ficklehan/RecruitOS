package com.recruitos.agent.controller;

import com.recruitos.agent.dto.AgentTaskCreateDTO;
import com.recruitos.agent.dto.AgentTaskQueryDTO;
import com.recruitos.agent.dto.AgentTaskVO;
import com.recruitos.agent.service.AgentTaskService;
import com.recruitos.common.result.PageResult;
import com.recruitos.common.result.R;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

/**
 * Agent task management controller
 */
@Api(tags = "Agent Task Management")
@RestController
@RequestMapping("/api/agent/task")
public class AgentTaskController {

    @Resource
    private AgentTaskService agentTaskService;

    @ApiOperation("Create agent task")
    @PostMapping
    public R<AgentTaskVO> createTask(@RequestBody AgentTaskCreateDTO dto) {
        AgentTaskVO vo = agentTaskService.createTask(dto);
        return R.ok(vo);
    }

    @ApiOperation("Start a task")
    @PostMapping("/{id}/start")
    public R<AgentTaskVO> startTask(@PathVariable Long id) {
        AgentTaskVO vo = agentTaskService.startTask(id);
        return R.ok(vo);
    }

    @ApiOperation("Pause a task")
    @PostMapping("/{id}/pause")
    public R<AgentTaskVO> pauseTask(@PathVariable Long id) {
        AgentTaskVO vo = agentTaskService.pauseTask(id);
        return R.ok(vo);
    }

    @ApiOperation("Resume a paused task")
    @PostMapping("/{id}/resume")
    public R<AgentTaskVO> resumeTask(@PathVariable Long id) {
        AgentTaskVO vo = agentTaskService.resumeTask(id);
        return R.ok(vo);
    }

    @ApiOperation("Get paginated task list")
    @GetMapping("/list")
    public R<PageResult<AgentTaskVO>> getTaskList(AgentTaskQueryDTO query) {
        PageResult<AgentTaskVO> result = agentTaskService.getTaskList(query);
        return R.ok(result);
    }

    @ApiOperation("Get task detail with recent logs")
    @GetMapping("/{id}")
    public R<AgentTaskVO> getTaskDetail(@PathVariable Long id) {
        AgentTaskVO vo = agentTaskService.getTaskDetail(id);
        return R.ok(vo);
    }
}
