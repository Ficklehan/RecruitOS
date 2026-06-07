package com.recruitos.headhunter.controller;

import com.recruitos.common.result.PageResult;
import com.recruitos.common.result.R;
import com.recruitos.headhunter.dto.VendorCreateDTO;
import com.recruitos.headhunter.dto.VendorQueryDTO;
import com.recruitos.headhunter.dto.VendorVO;
import com.recruitos.headhunter.service.VendorService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.util.Map;

/**
 * Headhunter vendor management controller
 */
@Api(tags = "Headhunter Vendor Management")
@RestController
@RequestMapping("/api/headhunter/vendor")
public class VendorController {

    @Resource
    private VendorService vendorService;

    @ApiOperation("Create a new headhunter vendor")
    @PostMapping
    public R<VendorVO> createVendor(@Valid @RequestBody VendorCreateDTO dto) {
        VendorVO vo = vendorService.createVendor(dto);
        return R.ok(vo);
    }

    @ApiOperation("Update a headhunter vendor")
    @PutMapping("/{id}")
    public R<VendorVO> updateVendor(@PathVariable Long id, @Valid @RequestBody VendorCreateDTO dto) {
        VendorVO vo = vendorService.updateVendor(id, dto);
        return R.ok(vo);
    }

    @ApiOperation("Get vendor detail")
    @GetMapping("/{id}")
    public R<VendorVO> getVendorDetail(@PathVariable Long id) {
        VendorVO vo = vendorService.getVendorDetail(id);
        return R.ok(vo);
    }

    @ApiOperation("List vendors with pagination")
    @GetMapping("/list")
    public R<PageResult<VendorVO>> getVendorList(VendorQueryDTO query) {
        PageResult<VendorVO> result = vendorService.getVendorList(query);
        return R.ok(result);
    }

    @ApiOperation("Delete a vendor (set status to INACTIVE)")
    @DeleteMapping("/{id}")
    public R<Void> deleteVendor(@PathVariable Long id) {
        vendorService.deleteVendor(id);
        return R.ok();
    }

    @ApiOperation("Get vendor statistics")
    @GetMapping("/stats")
    public R<Map<String, Object>> getVendorStats() {
        Map<String, Object> stats = vendorService.getVendorStats();
        return R.ok(stats);
    }
}
