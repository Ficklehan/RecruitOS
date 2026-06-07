package com.recruitos.tenant.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.recruitos.common.exception.BizException;
import com.recruitos.common.tenant.TenantContext;
import com.recruitos.tenant.entity.SysRole;
import com.recruitos.tenant.entity.SysRolePermission;
import com.recruitos.tenant.mapper.SysRoleMapper;
import com.recruitos.tenant.mapper.SysRolePermissionMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Role service with CRUD and permission assignment
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RoleService {

    private final SysRoleMapper sysRoleMapper;
    private final SysRolePermissionMapper sysRolePermissionMapper;

    /**
     * List all roles
     */
    public List<SysRole> list() {
        LambdaQueryWrapper<SysRole> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysRole::getTenantId, TenantContext.getTenantId())
                .orderByAsc(SysRole::getSortOrder);
        return sysRoleMapper.selectList(wrapper);
    }

    /**
     * Get role by ID
     */
    public SysRole getById(Long id) {
        SysRole role = sysRoleMapper.selectById(id);
        if (role == null) {
            throw new BizException(404, "Role not found");
        }
        return role;
    }

    /**
     * Create role
     */
    public SysRole create(SysRole role) {
        // Check duplicate role code
        LambdaQueryWrapper<SysRole> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysRole::getRoleCode, role.getRoleCode())
                .eq(SysRole::getTenantId, TenantContext.getTenantId());
        if (sysRoleMapper.selectCount(wrapper) > 0) {
            throw new BizException(400, "Role code already exists");
        }
        if (role.getStatus() == null) {
            role.setStatus(1);
        }
        sysRoleMapper.insert(role);
        return role;
    }

    /**
     * Update role
     */
    public SysRole update(Long id, SysRole role) {
        SysRole existing = getById(id);
        existing.setRoleName(role.getRoleName());
        existing.setDescription(role.getDescription());
        existing.setSortOrder(role.getSortOrder());
        existing.setStatus(role.getStatus());
        sysRoleMapper.updateById(existing);
        return existing;
    }

    /**
     * Delete role
     */
    public void delete(Long id) {
        getById(id);
        sysRoleMapper.deleteById(id);
        // Delete role permissions
        LambdaQueryWrapper<SysRolePermission> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysRolePermission::getRoleId, id);
        sysRolePermissionMapper.delete(wrapper);
    }

    /**
     * Assign permissions to role
     */
    @Transactional(rollbackFor = Exception.class)
    public void assignPermissions(Long roleId, List<Long> permissionIds) {
        getById(roleId);
        // Delete existing permissions
        LambdaQueryWrapper<SysRolePermission> deleteWrapper = new LambdaQueryWrapper<>();
        deleteWrapper.eq(SysRolePermission::getRoleId, roleId);
        sysRolePermissionMapper.delete(deleteWrapper);
        // Insert new permissions
        if (permissionIds != null && !permissionIds.isEmpty()) {
            for (Long permissionId : permissionIds) {
                SysRolePermission rp = new SysRolePermission();
                rp.setRoleId(roleId);
                rp.setPermissionId(permissionId);
                sysRolePermissionMapper.insert(rp);
            }
        }
    }

    /**
     * Get permission IDs of a role
     */
    public List<Long> getPermissionIds(Long roleId) {
        getById(roleId);
        LambdaQueryWrapper<SysRolePermission> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysRolePermission::getRoleId, roleId);
        return sysRolePermissionMapper.selectList(wrapper).stream()
                .map(SysRolePermission::getPermissionId)
                .collect(Collectors.toList());
    }
}
