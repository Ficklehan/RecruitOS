package com.recruitos.evolution.controller;

import com.recruitos.common.result.PageResult;
import com.recruitos.common.result.R;
import com.recruitos.evolution.dto.ProposalQueryDTO;
import com.recruitos.evolution.dto.ProposalVO;
import com.recruitos.evolution.service.EvolutionProposalService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.Map;

@Api(tags = "Evolution Proposals")
@RestController
@RequestMapping("/api/evolution/proposals")
public class EvolutionProposalController {

    @Resource
    private EvolutionProposalService proposalService;

    @ApiOperation("待确认进化建议列表")
    @GetMapping
    public R<PageResult<ProposalVO>> list(ProposalQueryDTO query) {
        return R.ok(proposalService.list(query));
    }

    @ApiOperation("进化建议详情")
    @GetMapping("/{id}")
    public R<ProposalVO> get(@PathVariable Long id) {
        return R.ok(proposalService.getById(id));
    }

    @ApiOperation("HR 确认采纳建议")
    @PostMapping("/{id}/confirm")
    public R<ProposalVO> confirm(@PathVariable Long id) {
        return R.ok(proposalService.confirm(id));
    }

    @ApiOperation("HR 驳回建议")
    @PostMapping("/{id}/reject")
    public R<ProposalVO> reject(@PathVariable Long id, @RequestBody(required = false) Map<String, String> body) {
        String reason = body != null ? body.get("reason") : null;
        return R.ok(proposalService.reject(id, reason));
    }
}
