package com.recruitos.tenant.controller;

import com.recruitos.common.result.PageResult;
import com.recruitos.common.result.R;
import com.recruitos.tenant.entity.SysRole;
import com.recruitos.tenant.service.UserService;
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
import java.util.Map;

/**
 * User management controller
 */
@Api(tags = "User Management")
@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @ApiOperation("Get user list (paginated)")
    @GetMapping("/list")
    public R<PageResult<Map<String, Object>>> list(
            @ApiParam("Page number") @RequestParam(defaultValue = "1") int pageNum,
            @ApiParam("Page size") @RequestParam(defaultValue = "10") int pageSize,
            @ApiParam("Organization ID filter") @RequestParam(required = false) Long orgId,
            @ApiParam("Role ID filter") @RequestParam(required = false) Long roleId) {
        com.baomidou.mybatisplus.core.metadata.IPage<Map<String, Object>> page = userService.page(pageNum, pageSize, orgId, roleId);
        PageResult<Map<String, Object>> result = new PageResult<>();
        result.setPageNum(pageNum);
        result.setPageSize(pageSize);
        result.setTotal(page.getTotal());
        result.setList(page.getRecords());
        return R.ok(result);
    }

    @ApiOperation("Get user roles")
    @GetMapping("/{id}/roles")
    public R<List<SysRole>> getUserRoles(@PathVariable Long id) {
        return R.ok(userService.getUserRoles(id));
    }

    @ApiOperation("Assign roles to user")
    @PostMapping("/{id}/roles")
    public R<Void> assignRoles(@PathVariable Long id, @RequestBody Object body) {
        List<Long> roleIds;
        if (body instanceof java.util.Map) {
            Object ids = ((java.util.Map<?, ?>) body).get("roleIds");
            roleIds = ids instanceof List ? (List<Long>) ids : java.util.Collections.emptyList();
        } else if (body instanceof List) {
            roleIds = (List<Long>) body;
        } else {
            roleIds = java.util.Collections.emptyList();
        }
        userService.assignRoles(id, roleIds);
        return R.ok();
    }

    @ApiOperation("Reset user password")
    @PutMapping("/{id}/password")
    public R<Void> resetPassword(@PathVariable Long id, @RequestBody Map<String, String> body) {
        userService.resetPassword(id, body.get("password"));
        return R.ok();
    }
}
