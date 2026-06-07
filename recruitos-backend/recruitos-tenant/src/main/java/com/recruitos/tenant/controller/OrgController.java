package com.recruitos.tenant.controller;

import com.recruitos.common.result.R;
import com.recruitos.tenant.entity.Organization;
import com.recruitos.tenant.service.OrgService;
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
 * Organization management controller
 */
@Api(tags = "Organization Management")
@RestController
@RequestMapping("/api/org")
@RequiredArgsConstructor
public class OrgController {

    private final OrgService orgService;

    @ApiOperation("Get organization tree")
    @GetMapping("/tree")
    public R<List<Organization>> tree() {
        return R.ok(orgService.tree());
    }

    @ApiOperation("Get organization list")
    @GetMapping("/list")
    public R<List<Organization>> list() {
        return R.ok(orgService.list());
    }

    @ApiOperation("Create organization")
    @PostMapping
    public R<Organization> create(@RequestBody Organization org) {
        return R.ok(orgService.create(org));
    }

    @ApiOperation("Update organization")
    @PutMapping("/{id}")
    public R<Organization> update(@PathVariable Long id, @RequestBody Organization org) {
        return R.ok(orgService.update(id, org));
    }

    @ApiOperation("Delete organization")
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        orgService.delete(id);
        return R.ok();
    }
}
