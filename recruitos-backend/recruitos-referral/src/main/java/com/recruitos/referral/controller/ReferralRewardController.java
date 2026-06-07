package com.recruitos.referral.controller;

import com.recruitos.common.result.PageResult;
import com.recruitos.common.result.R;
import com.recruitos.referral.dto.RewardCreateDTO;
import com.recruitos.referral.dto.RewardQueryDTO;
import com.recruitos.referral.dto.RewardVO;
import com.recruitos.referral.service.ReferralRewardService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.util.Map;

/**
 * Referral reward management controller
 */
@Api(tags = "Referral Reward Management")
@RestController
@RequestMapping("/api/referral/reward")
public class ReferralRewardController {

    @Resource
    private ReferralRewardService referralRewardService;

    @ApiOperation("Create a new referral reward")
    @PostMapping
    public R<RewardVO> createReward(@Valid @RequestBody RewardCreateDTO dto) {
        RewardVO vo = referralRewardService.createReward(dto);
        return R.ok(vo);
    }

    @ApiOperation("List rewards with pagination")
    @GetMapping("/list")
    public R<PageResult<RewardVO>> getRewardList(RewardQueryDTO query) {
        PageResult<RewardVO> result = referralRewardService.getRewardList(query);
        return R.ok(result);
    }

    @ApiOperation("Approve a reward")
    @PostMapping("/{id}/approve")
    public R<RewardVO> approveReward(@PathVariable Long id) {
        RewardVO vo = referralRewardService.approveReward(id);
        return R.ok(vo);
    }

    @ApiOperation("Mark a reward as paid")
    @PostMapping("/{id}/pay")
    public R<RewardVO> payReward(@PathVariable Long id) {
        RewardVO vo = referralRewardService.payReward(id);
        return R.ok(vo);
    }

    @ApiOperation("Get reward statistics")
    @GetMapping("/stats")
    public R<Map<String, Object>> getRewardStats() {
        Map<String, Object> stats = referralRewardService.getRewardStats();
        return R.ok(stats);
    }
}
