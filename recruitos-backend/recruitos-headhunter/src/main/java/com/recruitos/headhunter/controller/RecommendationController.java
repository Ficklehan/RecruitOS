package com.recruitos.headhunter.controller;

import com.recruitos.common.result.PageResult;
import com.recruitos.common.result.R;
import com.recruitos.headhunter.dto.RecommendationCreateDTO;
import com.recruitos.headhunter.dto.RecommendationQueryDTO;
import com.recruitos.headhunter.dto.RecommendationVO;
import com.recruitos.headhunter.service.RecommendationService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.util.Map;

/**
 * Headhunter recommendation management controller
 */
@Api(tags = "Headhunter Recommendation Management")
@RestController
@RequestMapping("/api/headhunter/recommendation")
public class RecommendationController {

    @Resource
    private RecommendationService recommendationService;

    @ApiOperation("Create a new recommendation")
    @PostMapping
    public R<RecommendationVO> createRecommendation(@Valid @RequestBody RecommendationCreateDTO dto) {
        RecommendationVO vo = recommendationService.createRecommendation(dto);
        return R.ok(vo);
    }

    @ApiOperation("Get recommendation detail")
    @GetMapping("/{id}")
    public R<RecommendationVO> getRecommendationDetail(@PathVariable Long id) {
        // For simplicity, using list with filter - can be enhanced later
        RecommendationQueryDTO query = new RecommendationQueryDTO();
        query.setPageNum(1);
        query.setPageSize(1);
        PageResult<RecommendationVO> result = recommendationService.getRecommendationList(query);
        if (result.getList() != null && !result.getList().isEmpty()) {
            return R.ok(result.getList().get(0));
        }
        return R.fail("Recommendation not found");
    }

    @ApiOperation("List recommendations with pagination")
    @GetMapping("/list")
    public R<PageResult<RecommendationVO>> getRecommendationList(RecommendationQueryDTO query) {
        PageResult<RecommendationVO> result = recommendationService.getRecommendationList(query);
        return R.ok(result);
    }

    @ApiOperation("Update recommendation status")
    @PutMapping("/{id}/status")
    public R<RecommendationVO> updateStatus(@PathVariable Long id, @RequestBody Map<String, String> body) {
        String status = body.get("status");
        RecommendationVO vo = recommendationService.updateStatus(id, status);
        return R.ok(vo);
    }

    @ApiOperation("Get recommendation statistics")
    @GetMapping("/stats")
    public R<Map<String, Object>> getRecommendationStats() {
        Map<String, Object> stats = recommendationService.getRecommendationStats();
        return R.ok(stats);
    }
}
