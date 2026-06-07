package com.recruitos.tenant.service;

import cn.hutool.crypto.digest.BCrypt;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.recruitos.common.exception.BizException;
import com.recruitos.tenant.entity.SysRole;
import com.recruitos.tenant.entity.SysUserRole;
import com.recruitos.tenant.entity.SysUser;
import com.recruitos.tenant.mapper.SysRoleMapper;
import com.recruitos.tenant.mapper.SysUserMapper;
import com.recruitos.tenant.mapper.SysUserRoleMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * User service (uses auth module's SysUser entity via cross-module reference)
 * For simplicity, this service manages user-role assignments
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final SysUserRoleMapper sysUserRoleMapper;
    private final SysRoleMapper sysRoleMapper;
    private final SysUserMapper sysUserMapper;

    /**
     * Page query users with optional org/role filter
     * Note: actual user data is in auth module's sys_user table
     */
    public IPage<Map<String, Object>> page(int pageNum, int pageSize, Long orgId, Long roleId) {
        Page<SysUser> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        if (orgId != null) {
            wrapper.eq(SysUser::getOrgId, orgId);
        }
        wrapper.orderByDesc(SysUser::getCreatedAt);

        IPage<SysUser> userPage = sysUserMapper.selectPage(page, wrapper);

        // Convert to Map list with role info
        Page<Map<String, Object>> result = new Page<>(pageNum, pageSize, userPage.getTotal());
        List<Map<String, Object>> records = new ArrayList<>();
        for (SysUser user : userPage.getRecords()) {
            Map<String, Object> map = new java.util.LinkedHashMap<>();
            map.put("id", user.getId());
            map.put("username", user.getUsername());
            map.put("realName", user.getRealName());
            map.put("email", user.getEmail());
            map.put("phone", user.getPhone());
            map.put("orgId", user.getOrgId());
            map.put("status", user.getStatus());
            map.put("lastLoginAt", user.getLastLoginAt());
            map.put("createdAt", user.getCreatedAt());
            // Filter by roleId if specified
            if (roleId != null) {
                List<SysUserRole> userRoles = sysUserRoleMapper.selectList(
                        new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getUserId, user.getId()));
                boolean hasRole = userRoles.stream().anyMatch(ur -> ur.getRoleId().equals(roleId));
                if (!hasRole) continue;
            }
            records.add(map);
        }
        result.setRecords(records);
        return result;
    }

    /**
     * Get roles of a user
     */
    public List<SysRole> getUserRoles(Long userId) {
        LambdaQueryWrapper<SysUserRole> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysUserRole::getUserId, userId);
        List<SysUserRole> userRoles = sysUserRoleMapper.selectList(wrapper);
        if (userRoles.isEmpty()) {
            return java.util.Collections.emptyList();
        }
        List<Long> roleIds = userRoles.stream()
                .map(SysUserRole::getRoleId)
                .collect(Collectors.toList());
        return sysRoleMapper.selectBatchIds(roleIds);
    }

    /**
     * Assign roles to user
     */
    @Transactional(rollbackFor = Exception.class)
    public void assignRoles(Long userId, List<Long> roleIds) {
        // Delete existing roles
        LambdaQueryWrapper<SysUserRole> deleteWrapper = new LambdaQueryWrapper<>();
        deleteWrapper.eq(SysUserRole::getUserId, userId);
        sysUserRoleMapper.delete(deleteWrapper);
        // Insert new roles
        if (roleIds != null && !roleIds.isEmpty()) {
            for (Long roleId : roleIds) {
                SysUserRole userRole = new SysUserRole();
                userRole.setUserId(userId);
                userRole.setRoleId(roleId);
                sysUserRoleMapper.insert(userRole);
            }
        }
    }

    /**
     * Reset user password
     */
    public void resetPassword(Long userId, String newPassword) {
        if (!StringUtils.hasText(newPassword)) {
            throw new BizException(400, "Password cannot be empty");
        }
        if (newPassword.length() < 6) {
            throw new BizException(400, "Password must be at least 6 characters");
        }
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null) {
            throw new BizException(404, "User not found");
        }
        String encodedPassword = BCrypt.hashpw(newPassword, BCrypt.gensalt());
        user.setPasswordHash(encodedPassword);
        sysUserMapper.updateById(user);
        log.info("Password reset for user {}", userId);
    }
}
