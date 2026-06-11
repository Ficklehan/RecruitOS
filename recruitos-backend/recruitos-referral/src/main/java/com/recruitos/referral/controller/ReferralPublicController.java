package com.recruitos.referral.controller;

import com.recruitos.common.result.R;
import com.recruitos.referral.dto.ReferralLinkVO;
import com.recruitos.referral.dto.ReferralPublicSubmitDTO;
import com.recruitos.referral.dto.ReferralVO;
import com.recruitos.referral.service.ReferralShareService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.Valid;

@Api(tags = "Referral Public")
@RestController
@RequestMapping("/api/referral/public")
public class ReferralPublicController {

    @Resource
    private ReferralShareService shareService;

    @ApiOperation("Get share link job info (no auth)")
    @GetMapping("/link/{token}")
    public R<ReferralLinkVO> getLinkInfo(@PathVariable String token) {
        return R.ok(shareService.getLinkInfo(token));
    }

    @ApiOperation("Submit referral resume via share link (no auth)")
    @PostMapping("/submit")
    public R<ReferralVO> submit(@Valid @RequestBody ReferralPublicSubmitDTO dto) {
        return R.ok(shareService.submitByToken(dto));
    }
}
