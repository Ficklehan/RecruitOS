package com.recruitos.tenant.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.recruitos.common.result.PageResult;
import com.recruitos.common.result.R;
import com.recruitos.tenant.entity.Tenant;
import com.recruitos.tenant.service.TenantService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Tenant management controller
 */
@Api(tags = "Tenant Management")
@RestController
@RequestMapping("/api/tenant")
@RequiredArgsConstructor
public class TenantController {

    private final TenantService tenantService;

    @ApiOperation("Get all active tenants (for login dropdown)")
    @GetMapping("/simple-list")
    public R<List<Tenant>> simpleList() {
        return R.ok(tenantService.listAll());
    }

    @ApiOperation("Get tenant list (paginated)")
    @GetMapping("/list")
    public R<PageResult<Tenant>> list(
            @ApiParam("Page number") @RequestParam(defaultValue = "1") int pageNum,
            @ApiParam("Page size") @RequestParam(defaultValue = "10") int pageSize,
            @ApiParam("Search keyword") @RequestParam(required = false) String keyword) {
        IPage<Tenant> page = tenantService.page(pageNum, pageSize, keyword);
        PageResult<Tenant> result = new PageResult<>(page.getTotal(), page.getRecords(),
                (int) page.getCurrent(), (int) page.getSize());
        return R.ok(result);
    }

    @ApiOperation("Get tenant detail")
    @GetMapping("/{id}")
    public R<Tenant> getById(@PathVariable Long id) {
        return R.ok(tenantService.getById(id));
    }

    @ApiOperation("Create tenant")
    @PostMapping
    public R<Tenant> create(@RequestBody Tenant tenant) {
        return R.ok(tenantService.create(tenant));
    }

    @ApiOperation("Update tenant")
    @PutMapping("/{id}")
    public R<Tenant> update(@PathVariable Long id, @RequestBody Tenant tenant) {
        return R.ok(tenantService.update(id, tenant));
    }

    @ApiOperation("Delete tenant")
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        tenantService.delete(id);
        return R.ok();
    }
}
