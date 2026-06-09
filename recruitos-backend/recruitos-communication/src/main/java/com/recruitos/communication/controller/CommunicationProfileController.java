package com.recruitos.communication.controller;

import com.recruitos.common.result.R;
import com.recruitos.communication.dto.CommunicationProfileSaveDTO;
import com.recruitos.communication.dto.CommunicationProfileVO;
import com.recruitos.communication.service.CommunicationProfileService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

@Api(tags = "Communication Profile")
@RestController
@RequestMapping("/api/communication/profile")
public class CommunicationProfileController {

    @Resource
    private CommunicationProfileService profileService;

    @ApiOperation("租户默认 Profile")
    @GetMapping("/tenant-default")
    public R<CommunicationProfileVO> getTenantDefault() {
        return R.ok(profileService.getTenantDefault());
    }

    @ApiOperation("保存租户默认 Profile")
    @PutMapping("/tenant-default")
    public R<CommunicationProfileVO> saveTenantDefault(@RequestBody CommunicationProfileSaveDTO dto) {
        return R.ok(profileService.saveTenantDefault(dto));
    }

    @ApiOperation("岗位覆盖 Profile")
    @GetMapping("/job/{jobId}")
    public R<CommunicationProfileVO> getJobOverride(@PathVariable Long jobId) {
        return R.ok(profileService.getJobOverride(jobId));
    }

    @ApiOperation("保存岗位覆盖 Profile")
    @PutMapping("/job/{jobId}")
    public R<CommunicationProfileVO> saveJobOverride(@PathVariable Long jobId,
                                                     @RequestBody CommunicationProfileSaveDTO dto) {
        return R.ok(profileService.saveJobOverride(jobId, dto));
    }

    @ApiOperation("解析合并 Profile（租户 + 岗位）")
    @GetMapping("/resolve")
    public R<CommunicationProfileVO> resolve(@RequestParam(required = false) Long jobId) {
        return R.ok(profileService.resolveForJob(jobId));
    }
}
