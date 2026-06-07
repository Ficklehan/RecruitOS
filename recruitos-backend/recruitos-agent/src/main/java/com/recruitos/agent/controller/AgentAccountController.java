package com.recruitos.agent.controller;

import com.recruitos.agent.dto.AgentAccountCreateDTO;
import com.recruitos.agent.dto.AgentAccountQueryDTO;
import com.recruitos.agent.dto.AgentAccountVO;
import com.recruitos.agent.service.AgentAccountService;
import com.recruitos.common.result.PageResult;
import com.recruitos.common.result.R;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

/**
 * Agent account management controller
 */
@Api(tags = "Agent Account Management")
@RestController
@RequestMapping("/api/agent/account")
public class AgentAccountController {

    @Resource
    private AgentAccountService agentAccountService;

    @ApiOperation("Create agent account")
    @PostMapping
    public R<AgentAccountVO> createAccount(@RequestBody AgentAccountCreateDTO dto) {
        AgentAccountVO vo = agentAccountService.createAccount(dto);
        return R.ok(vo);
    }

    @ApiOperation("Update agent account")
    @PutMapping("/{id}")
    public R<AgentAccountVO> updateAccount(@PathVariable Long id, @RequestBody AgentAccountCreateDTO dto) {
        AgentAccountVO vo = agentAccountService.updateAccount(id, dto);
        return R.ok(vo);
    }

    @ApiOperation("Get paginated agent account list")
    @GetMapping("/list")
    public R<PageResult<AgentAccountVO>> getAccountList(AgentAccountQueryDTO query) {
        PageResult<AgentAccountVO> result = agentAccountService.getAccountList(query);
        return R.ok(result);
    }

    @ApiOperation("Get agent account detail")
    @GetMapping("/{id}")
    public R<AgentAccountVO> getAccountDetail(@PathVariable Long id) {
        AgentAccountVO vo = agentAccountService.getAccountDetail(id);
        return R.ok(vo);
    }

    @ApiOperation("Update account status")
    @PutMapping("/{id}/status")
    public R<AgentAccountVO> updateStatus(@PathVariable Long id, @RequestParam String status) {
        AgentAccountVO vo = agentAccountService.updateStatus(id, status);
        return R.ok(vo);
    }

    @ApiOperation("Refresh account health score")
    @PostMapping("/{id}/refresh-health")
    public R<AgentAccountVO> refreshHealth(@PathVariable Long id) {
        AgentAccountVO vo = agentAccountService.refreshHealth(id);
        return R.ok(vo);
    }
}
