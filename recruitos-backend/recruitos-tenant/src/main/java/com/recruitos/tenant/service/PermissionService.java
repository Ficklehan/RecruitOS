package com.recruitos.tenant.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.recruitos.common.exception.BizException;
import com.recruitos.tenant.entity.SysPermission;
import com.recruitos.tenant.mapper.SysPermissionMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Permission service with tree structure
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PermissionService {

    private final SysPermissionMapper sysPermissionMapper;

    /**
     * List all permissions
     */
    public List<SysPermission> list() {
        LambdaQueryWrapper<SysPermission> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByAsc(SysPermission::getSortOrder);
        return sysPermissionMapper.selectList(wrapper);
    }

    /**
     * Get permission tree
     */
    public List<SysPermission> tree() {
        List<SysPermission> allPerms = list();
        return buildTree(allPerms, 0L);
    }

    /**
     * Build tree structure from flat list
     */
    private List<SysPermission> buildTree(List<SysPermission> allPerms, Long parentId) {
        List<SysPermission> tree = new ArrayList<>();
        for (SysPermission perm : allPerms) {
            Long pid = perm.getParentId() == null ? 0L : perm.getParentId();
            if (parentId.equals(pid)) {
                perm.setChildren(buildTree(allPerms, perm.getId()));
                tree.add(perm);
            }
        }
        return tree;
    }

    /**
     * Get permission by ID
     */
    public SysPermission getById(Long id) {
        SysPermission perm = sysPermissionMapper.selectById(id);
        if (perm == null) {
            throw new BizException(404, "Permission not found");
        }
        return perm;
    }

    /**
     * Create permission
     */
    public SysPermission create(SysPermission permission) {
        // Check duplicate perm code
        LambdaQueryWrapper<SysPermission> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysPermission::getPermCode, permission.getPermCode());
        if (sysPermissionMapper.selectCount(wrapper) > 0) {
            throw new BizException(400, "Permission code already exists");
        }
        if (permission.getParentId() == null) {
            permission.setParentId(0L);
        }
        if (permission.getStatus() == null) {
            permission.setStatus(1);
        }
        sysPermissionMapper.insert(permission);
        return permission;
    }

    /**
     * Update permission
     */
    public SysPermission update(Long id, SysPermission permission) {
        SysPermission existing = getById(id);
        existing.setPermName(permission.getPermName());
        existing.setType(permission.getType());
        existing.setPath(permission.getPath());
        existing.setIcon(permission.getIcon());
        existing.setSortOrder(permission.getSortOrder());
        existing.setStatus(permission.getStatus());
        sysPermissionMapper.updateById(existing);
        return existing;
    }

    /**
     * Delete permission
     */
    public void delete(Long id) {
        getById(id);
        // Check if has children
        LambdaQueryWrapper<SysPermission> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysPermission::getParentId, id);
        if (sysPermissionMapper.selectCount(wrapper) > 0) {
            throw new BizException(400, "Cannot delete permission with children");
        }
        sysPermissionMapper.deleteById(id);
    }
}
