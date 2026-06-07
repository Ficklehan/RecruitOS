package com.recruitos.demand.controller;

import com.recruitos.common.result.PageResult;
import com.recruitos.common.result.R;
import com.recruitos.demand.dto.DemandBoardVO;
import com.recruitos.demand.dto.DemandCreateDTO;
import com.recruitos.demand.dto.DemandQueryDTO;
import com.recruitos.demand.dto.DemandVO;
import com.recruitos.demand.service.DemandService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.Valid;

/**
 * Demand management controller
 */
@Api(tags = "Recruitment Demand Management")
@RestController
@RequestMapping("/api/demand")
public class DemandController {

    @Resource
    private DemandService demandService;

    @ApiOperation("Create a new recruitment demand")
    @PostMapping
    public R<DemandVO> createDemand(@Valid @RequestBody DemandCreateDTO dto) {
        DemandVO vo = demandService.createDemand(dto);
        return R.ok(vo);
    }

    @ApiOperation("Update a recruitment demand")
    @PutMapping("/{id}")
    public R<DemandVO> updateDemand(@PathVariable Long id, @RequestBody DemandCreateDTO dto) {
        DemandVO vo = demandService.updateDemand(id, dto);
        return R.ok(vo);
    }

    @ApiOperation("Get demand detail")
    @GetMapping("/{id}")
    public R<DemandVO> getDemandDetail(@PathVariable Long id) {
        DemandVO vo = demandService.getDemandDetail(id);
        if (vo == null) {
            return R.fail("Demand not found");
        }
        return R.ok(vo);
    }

    @ApiOperation("List demands with pagination")
    @GetMapping("/list")
    public R<PageResult<DemandVO>> listDemands(DemandQueryDTO query) {
        PageResult<DemandVO> result = demandService.listDemands(query);
        return R.ok(result);
    }

    @ApiOperation("Submit demand for approval")
    @PostMapping("/{id}/submit")
    public R<DemandVO> submitDemand(@PathVariable Long id) {
        DemandVO vo = demandService.submitDemand(id);
        return R.ok(vo);
    }

    @ApiOperation("Close a demand")
    @PostMapping("/{id}/close")
    public R<DemandVO> closeDemand(@PathVariable Long id) {
        DemandVO vo = demandService.closeDemand(id);
        return R.ok(vo);
    }

    @ApiOperation("Get demand dashboard statistics")
    @GetMapping("/board")
    public R<DemandBoardVO> getDemandBoard() {
        DemandBoardVO board = demandService.getDemandBoard();
        return R.ok(board);
    }
}
