package com.recruitos.agent.controller;

import com.recruitos.agent.dto.BehaviorLogQueryDTO;
import com.recruitos.agent.dto.BehaviorLogVO;
import com.recruitos.agent.service.BehaviorLogService;
import com.recruitos.common.result.PageResult;
import com.recruitos.common.result.R;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.Map;

/**
 * Behavior log management controller
 */
@Api(tags = "Agent Behavior Log Management")
@RestController
@RequestMapping("/api/agent/log")
public class BehaviorLogController {

    @Resource
    private BehaviorLogService behaviorLogService;

    @ApiOperation("Get paginated behavior log list")
    @GetMapping("/list")
    public R<PageResult<BehaviorLogVO>> getLogList(BehaviorLogQueryDTO query) {
        PageResult<BehaviorLogVO> result = behaviorLogService.getLogList(query);
        return R.ok(result);
    }

    @ApiOperation("Get behavior log statistics")
    @GetMapping("/stats")
    public R<Map<String, Object>> getLogStats() {
        Map<String, Object> stats = behaviorLogService.getLogStats();
        return R.ok(stats);
    }

    @ApiOperation("Get activity timeline for an account")
    @GetMapping("/account/{accountId}/activity")
    public R<PageResult<BehaviorLogVO>> getAccountActivity(@PathVariable Long accountId) {
        PageResult<BehaviorLogVO> result = behaviorLogService.getAccountActivity(accountId);
        return R.ok(result);
    }
}
