package com.recruitos.tenant.controller;

import com.recruitos.common.result.R;
import com.recruitos.tenant.entity.SysPermission;
import com.recruitos.tenant.service.PermissionService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Permission management controller
 */
@Api(tags = "Permission Management")
@RestController
@RequestMapping("/api/permission")
@RequiredArgsConstructor
public class PermissionController {

    private final PermissionService permissionService;

    @ApiOperation("Get permission tree")
    @GetMapping("/tree")
    public R<List<SysPermission>> tree() {
        return R.ok(permissionService.tree());
    }

    @ApiOperation("Get permission list")
    @GetMapping("/list")
    public R<List<SysPermission>> list() {
        return R.ok(permissionService.list());
    }

    @ApiOperation("Get permission detail")
    @GetMapping("/{id}")
    public R<SysPermission> getById(@PathVariable Long id) {
        return R.ok(permissionService.getById(id));
    }

    @ApiOperation("Create permission")
    @PostMapping
    public R<SysPermission> create(@RequestBody SysPermission permission) {
        return R.ok(permissionService.create(permission));
    }

    @ApiOperation("Update permission")
    @PutMapping("/{id}")
    public R<SysPermission> update(@PathVariable Long id, @RequestBody SysPermission permission) {
        return R.ok(permissionService.update(id, permission));
    }

    @ApiOperation("Delete permission")
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        permissionService.delete(id);
        return R.ok();
    }
}
