package com.recruitos.evolution.controller;

import com.recruitos.common.result.PageResult;
import com.recruitos.common.result.R;
import com.recruitos.evolution.dto.AbTestCreateDTO;
import com.recruitos.evolution.dto.AbTestQueryDTO;
import com.recruitos.evolution.dto.AbTestVO;
import com.recruitos.evolution.service.AbTestService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.Valid;

/**
 * A/B test management controller
 */
@Api(tags = "Evolution A/B Test Management")
@RestController
@RequestMapping("/api/evolution/abtest")
public class AbTestController {

    @Resource
    private AbTestService abTestService;

    @ApiOperation("Create a new A/B test")
    @PostMapping
    public R<AbTestVO> createTest(@Valid @RequestBody AbTestCreateDTO dto) {
        AbTestVO vo = abTestService.createTest(dto);
        return R.ok(vo);
    }

    @ApiOperation("Start an A/B test")
    @PostMapping("/{id}/start")
    public R<AbTestVO> startTest(@PathVariable Long id) {
        AbTestVO vo = abTestService.startTest(id);
        return R.ok(vo);
    }

    @ApiOperation("Stop an A/B test and declare winner")
    @PostMapping("/{id}/stop")
    public R<AbTestVO> stopTest(@PathVariable Long id, @RequestParam String winnerVariant) {
        AbTestVO vo = abTestService.stopTest(id, winnerVariant);
        return R.ok(vo);
    }

    @ApiOperation("Get paginated list of A/B tests")
    @GetMapping("/list")
    public R<PageResult<AbTestVO>> getTestList(AbTestQueryDTO query) {
        PageResult<AbTestVO> result = abTestService.getTestList(query);
        return R.ok(result);
    }

    @ApiOperation("Get A/B test detail")
    @GetMapping("/{id}")
    public R<AbTestVO> getTestDetail(@PathVariable Long id) {
        AbTestVO vo = abTestService.getTestDetail(id);
        return R.ok(vo);
    }
}
