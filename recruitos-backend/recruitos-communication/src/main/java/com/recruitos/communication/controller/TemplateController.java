package com.recruitos.communication.controller;

import com.recruitos.common.result.PageResult;
import com.recruitos.common.result.R;
import com.recruitos.communication.dto.TemplateCreateDTO;
import com.recruitos.communication.dto.TemplateQueryDTO;
import com.recruitos.communication.dto.TemplateVO;
import com.recruitos.communication.service.TemplateService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.util.Map;

/**
 * Message template management controller
 */
@Api(tags = "Message Template Management")
@RestController
@RequestMapping("/api/communication/template")
public class TemplateController {

    @Resource
    private TemplateService templateService;

    @ApiOperation("Create a new message template")
    @PostMapping
    public R<TemplateVO> createTemplate(@Valid @RequestBody TemplateCreateDTO dto) {
        TemplateVO vo = templateService.createTemplate(dto);
        return R.ok(vo);
    }

    @ApiOperation("Update an existing message template")
    @PutMapping("/{id}")
    public R<TemplateVO> updateTemplate(@PathVariable Long id, @Valid @RequestBody TemplateCreateDTO dto) {
        TemplateVO vo = templateService.updateTemplate(id, dto);
        return R.ok(vo);
    }

    @ApiOperation("Get paginated template list")
    @GetMapping("/list")
    public R<PageResult<TemplateVO>> getTemplateList(TemplateQueryDTO query) {
        PageResult<TemplateVO> result = templateService.getTemplateList(query);
        return R.ok(result);
    }

    @ApiOperation("Get template detail")
    @GetMapping("/{id}")
    public R<TemplateVO> getTemplateDetail(@PathVariable Long id) {
        TemplateVO vo = templateService.getTemplateDetail(id);
        return R.ok(vo);
    }

    @ApiOperation("Delete a template (soft delete)")
    @DeleteMapping("/{id}")
    public R<Void> deleteTemplate(@PathVariable Long id) {
        templateService.deleteTemplate(id);
        return R.ok();
    }

    @ApiOperation("Get template usage statistics")
    @GetMapping("/stats")
    public R<Map<String, Object>> getTemplateStats() {
        Map<String, Object> stats = templateService.getTemplateStats();
        return R.ok(stats);
    }
}
