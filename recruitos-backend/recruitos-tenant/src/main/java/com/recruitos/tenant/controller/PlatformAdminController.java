package com.recruitos.tenant.controller;

import com.recruitos.common.result.PageResult;
import com.recruitos.common.result.R;
import com.recruitos.tenant.dto.PlatformAdminLoginDTO;
import com.recruitos.tenant.dto.TenantCreateDTO;
import com.recruitos.tenant.dto.TenantDetailVO;
import com.recruitos.tenant.entity.TenantLicense;
import com.recruitos.tenant.service.PlatformAdminService;
import com.recruitos.tenant.service.TenantManagementService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.Map;

@Api(tags = "Platform Admin")
@RestController
@RequestMapping("/api/platform")
@RequiredArgsConstructor
public class PlatformAdminController {

    private final PlatformAdminService platformAdminService;
    private final TenantManagementService tenantManagementService;

    // ============ Auth ============

    @ApiOperation("平台管理员登录")
    @PostMapping("/login")
    public R<Map<String, Object>> login(@Valid @RequestBody PlatformAdminLoginDTO dto) {
        return R.ok(platformAdminService.login(dto));
    }

    @ApiOperation("平台管理员登出")
    @PostMapping("/logout")
    public R<Void> logout() {
        platformAdminService.logout();
        return R.ok();
    }

    @ApiOperation("获取当前管理员信息")
    @GetMapping("/me")
    public R<Map<String, Object>> me() {
        return R.ok(platformAdminService.getCurrentAdmin());
    }

    // ============ Tenant Management ============

    @ApiOperation("租户列表（分页）")
    @GetMapping("/tenants")
    public R<PageResult<TenantDetailVO>> listTenants(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer status) {
        return R.ok(tenantManagementService.listTenants(pageNum, pageSize, keyword, status));
    }

    @ApiOperation("租户详情")
    @GetMapping("/tenants/{id}")
    public R<TenantDetailVO> getTenant(@PathVariable Long id) {
        return R.ok(tenantManagementService.getTenantDetail(id));
    }

    @ApiOperation("创建租户")
    @PostMapping("/tenants")
    public R<TenantDetailVO> createTenant(@Valid @RequestBody TenantCreateDTO dto) {
        return R.ok(tenantManagementService.createTenantWithAdmin(dto));
    }

    @ApiOperation("更新租户状态（启停）")
    @PutMapping("/tenants/{id}/status")
    public R<TenantDetailVO> updateStatus(@PathVariable Long id, @RequestBody Map<String, Integer> body) {
        return R.ok(tenantManagementService.updateTenantStatus(id, body.get("status")));
    }

    @ApiOperation("变更租户套餐")
    @PutMapping("/tenants/{id}/plan")
    public R<TenantDetailVO> updatePlan(@PathVariable Long id, @RequestBody Map<String, String> body) {
        return R.ok(tenantManagementService.updateTenantPlan(id, body.get("plan")));
    }

    @ApiOperation("获取租户License")
    @GetMapping("/tenants/{id}/license")
    public R<TenantLicense> getLicense(@PathVariable Long id) {
        return R.ok(tenantManagementService.getTenantLicense(id));
    }

    @ApiOperation("更新租户License配额")
    @PutMapping("/tenants/{id}/license")
    public R<TenantLicense> updateLicense(@PathVariable Long id, @RequestBody TenantLicense license) {
        return R.ok(tenantManagementService.updateTenantLicense(id, license));
    }
}
