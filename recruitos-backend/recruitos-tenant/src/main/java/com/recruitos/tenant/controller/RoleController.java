package com.recruitos.tenant.controller;

import com.recruitos.common.result.R;
import com.recruitos.tenant.entity.SysRole;
import com.recruitos.tenant.service.RoleService;
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
 * Role management controller
 */
@Api(tags = "Role Management")
@RestController
@RequestMapping("/api/role")
@RequiredArgsConstructor
public class RoleController {

    private final RoleService roleService;

    @ApiOperation("Get role list")
    @GetMapping({"/list", ""})
    public R<List<SysRole>> list() {
        return R.ok(roleService.list());
    }

    @ApiOperation("Get role detail")
    @GetMapping("/{id}")
    public R<SysRole> getById(@PathVariable Long id) {
        return R.ok(roleService.getById(id));
    }

    @ApiOperation("Create role")
    @PostMapping
    public R<SysRole> create(@RequestBody SysRole role) {
        return R.ok(roleService.create(role));
    }

    @ApiOperation("Update role")
    @PutMapping("/{id}")
    public R<SysRole> update(@PathVariable Long id, @RequestBody SysRole role) {
        return R.ok(roleService.update(id, role));
    }

    @ApiOperation("Delete role")
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        roleService.delete(id);
        return R.ok();
    }

    @ApiOperation("Assign permissions to role")
    @PostMapping("/{id}/permissions")
    public R<Void> assignPermissions(@PathVariable Long id, @RequestBody List<Long> permissionIds) {
        roleService.assignPermissions(id, permissionIds);
        return R.ok();
    }

    @ApiOperation("Get role permissions")
    @GetMapping("/{id}/permissions")
    public R<List<Long>> getRolePermissions(@PathVariable Long id) {
        return R.ok(roleService.getPermissionIds(id));
    }
}
