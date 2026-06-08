package com.recruitos.candidate.controller;

import com.recruitos.candidate.service.ResumeService;
import com.recruitos.common.result.PageResult;
import com.recruitos.common.result.R;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;

@Api(tags = "Resume Management")
@RestController
@RequestMapping("/api/resume")
public class ResumeController {

    @Resource
    private ResumeService resumeService;

    @PostMapping("/upload")
    public R<Map<String, Object>> upload(@RequestParam("file") MultipartFile file) {
        return R.ok(resumeService.upload(file));
    }

    @PostMapping("/batch-upload")
    public R<List<Map<String, Object>>> batchUpload(@RequestParam("files") MultipartFile[] files) {
        java.util.List<Map<String, Object>> list = new java.util.ArrayList<>();
        for (MultipartFile file : files) {
            list.add(resumeService.upload(file));
        }
        return R.ok(list);
    }

    @GetMapping("/list")
    public R<PageResult<Map<String, Object>>> list(@RequestParam(defaultValue = "1") Integer pageNum,
                                                    @RequestParam(defaultValue = "20") Integer pageSize,
                                                    @RequestParam(required = false) String parseStatus) {
        return R.ok(resumeService.list(pageNum, pageSize, parseStatus));
    }

    @GetMapping("/{id}")
    public R<Map<String, Object>> detail(@PathVariable Long id) {
        return R.ok(resumeService.detail(id));
    }

    @PostMapping("/{id}/parse")
    public R<Map<String, Object>> parse(@PathVariable Long id) {
        return R.ok(resumeService.parse(id));
    }

    @PostMapping("/{id}/import")
    public R<Map<String, Object>> importToPool(@PathVariable Long id) {
        return R.ok(resumeService.importToTalentPool(id));
    }

    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        resumeService.delete(id);
        return R.ok();
    }
}
