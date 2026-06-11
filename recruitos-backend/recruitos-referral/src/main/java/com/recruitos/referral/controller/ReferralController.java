package com.recruitos.referral.controller;

import com.recruitos.common.result.PageResult;
import com.recruitos.common.result.R;
import com.recruitos.referral.dto.ReferralCreateDTO;
import com.recruitos.referral.dto.ReferralLinkCreateDTO;
import com.recruitos.referral.dto.ReferralLinkVO;
import com.recruitos.referral.dto.ReferralQueryDTO;
import com.recruitos.referral.dto.ReferralVO;
import com.recruitos.referral.service.ReferralService;
import com.recruitos.referral.service.ReferralShareService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.util.Map;

/**
 * Referral management controller
 */
@Api(tags = "Referral Management")
@RestController
@RequestMapping("/api/referral")
public class ReferralController {

    @Resource
    private ReferralService referralService;

    @Resource
    private ReferralShareService shareService;

    @ApiOperation("Create share link for employee referral")
    @PostMapping("/link")
    public R<ReferralLinkVO> createShareLink(@Valid @RequestBody ReferralLinkCreateDTO dto) {
        return R.ok(shareService.createShareLink(dto));
    }

    @ApiOperation("Create a new referral")
    @PostMapping
    public R<ReferralVO> createReferral(@Valid @RequestBody ReferralCreateDTO dto) {
        ReferralVO vo = referralService.createReferral(dto);
        return R.ok(vo);
    }

    @ApiOperation("Get referral detail")
    @GetMapping("/{id}")
    public R<ReferralVO> getReferralDetail(@PathVariable Long id) {
        ReferralVO vo = referralService.getReferralDetail(id);
        return R.ok(vo);
    }

    @ApiOperation("List referrals with pagination")
    @GetMapping("/list")
    public R<PageResult<ReferralVO>> getReferralList(ReferralQueryDTO query) {
        PageResult<ReferralVO> result = referralService.getReferralList(query);
        return R.ok(result);
    }

    @ApiOperation("Update referral status")
    @PutMapping("/{id}/status")
    public R<ReferralVO> updateStatus(@PathVariable Long id, @RequestBody Map<String, String> body) {
        String status = body.get("status");
        ReferralVO vo = referralService.updateStatus(id, status);
        return R.ok(vo);
    }
}
