package com.recruitos.job.controller;

import com.recruitos.common.result.PageResult;
import com.recruitos.common.result.R;
import com.recruitos.common.llm.LlmChatRequest;
import com.recruitos.common.llm.LlmClient;
import com.recruitos.job.dto.*;
import com.recruitos.job.service.JobService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.util.*;

/**
 * Job position management controller
 */
@Api(tags = "Job Position Management")
@RestController
@RequestMapping("/api/job")
public class JobController {

    @Resource
    private JobService jobService;
    @Resource
    private LlmClient llmClient;

    @ApiOperation("Create a new job position")
    @PostMapping
    public R<JobVO> createJob(@Valid @RequestBody JobCreateDTO dto) {
        JobVO vo = jobService.createJob(dto);
        return R.ok(vo);
    }

    @ApiOperation("Update a job position")
    @PutMapping("/{id}")
    public R<JobVO> updateJob(@PathVariable Long id, @RequestBody JobUpdateDTO dto) {
        JobVO vo = jobService.updateJob(id, dto);
        return R.ok(vo);
    }

    @ApiOperation("Get job position detail")
    @GetMapping("/{id}")
    public R<JobVO> getJobDetail(@PathVariable Long id) {
        JobVO vo = jobService.getJobDetail(id);
        return R.ok(vo);
    }

    @ApiOperation("List job positions with pagination")
    @GetMapping("/list")
    public R<PageResult<JobVO>> listJobs(JobQueryDTO query) {
        PageResult<JobVO> result = jobService.listJobs(query);
        return R.ok(result);
    }

    @ApiOperation("Activate a job position (DRAFT -> ACTIVE)")
    @PostMapping("/{id}/activate")
    public R<JobVO> activateJob(@PathVariable Long id) {
        JobVO vo = jobService.activateJob(id);
        return R.ok(vo);
    }

    @ApiOperation("Pause a job position (ACTIVE -> PAUSED)")
    @PostMapping("/{id}/pause")
    public R<JobVO> pauseJob(@PathVariable Long id) {
        JobVO vo = jobService.pauseJob(id);
        return R.ok(vo);
    }

    @ApiOperation("Close a job position")
    @PostMapping("/{id}/close")
    public R<JobVO> closeJob(@PathVariable Long id, @RequestParam(required = false) String reason) {
        JobVO vo = jobService.closeJob(id, reason);
        return R.ok(vo);
    }

    @ApiOperation("Parse JD text (simulated LLM)")
    @PostMapping("/{id}/parse-jd")
    public R<JdParseResultVO> parseJd(@PathVariable Long id) {
        JdParseResultVO result = jobService.parseJd(id);
        return R.ok(result);
    }

    @ApiOperation("Get tags for a job position")
    @GetMapping("/{id}/tags")
    public R<List<TagDTO>> getTags(@PathVariable Long id) {
        List<TagDTO> tags = jobService.getTags(id);
        return R.ok(tags);
    }

    @ApiOperation("Update tags for a job position")
    @PutMapping("/{id}/tags")
    public R<List<TagDTO>> updateTags(@PathVariable Long id, @RequestBody List<TagDTO> tagDTOs) {
        List<TagDTO> tags = jobService.updateTags(id, tagDTOs);
        return R.ok(tags);
    }

    @ApiOperation("AI draft JD from natural language description")
    @PostMapping("/ai-draft")
    public R<Map<String, Object>> aiDraftJd(@RequestBody Map<String, String> body) {
        String nlInput = body.get("description");
        if (nlInput == null || nlInput.isBlank()) {
            return R.fail("请输入职位需求描述");
        }
        LlmChatRequest req = new LlmChatRequest();
        req.setSystemPrompt("你是资深招聘专家。根据用人经理的自然语言描述，生成一份结构化的JD草稿，"
            + "包含：职位名称、岗位职责(3-5条)、任职要求(必备3-5项+加分2-3项)。"
            + "同时提取关键技能标签。输出JSON格式：{title, responsibilities:[], "
            + "mustHave:[], niceToHave:[], tags:[{name,type(MUST/NICE),weight}]}");
        req.setUserPrompt(nlInput);
        req.setScenario("jd_draft");
        String llmResult = llmClient.chat(req);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("rawDescription", nlInput);
        result.put("llmGenerated", llmResult);
        return R.ok(result);
    }
}
