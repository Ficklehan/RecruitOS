package com.recruitos.auth.service;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.crypto.digest.BCrypt;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.recruitos.auth.dto.LoginDTO;
import com.recruitos.auth.dto.RoleVO;
import com.recruitos.auth.dto.UserInfoVO;
import com.recruitos.auth.entity.SysUser;
import com.recruitos.auth.mapper.OrganizationMapper;
import com.recruitos.auth.mapper.SysPermissionMapper;
import com.recruitos.auth.mapper.SysRoleMapper;
import com.recruitos.auth.mapper.SysUserMapper;
import com.recruitos.auth.mapper.SysUserRoleMapper;
import com.recruitos.common.auth.CurrentUser;
import com.recruitos.common.exception.BizException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Authentication service
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final SysUserMapper sysUserMapper;
    private final SysUserRoleMapper sysUserRoleMapper;
    private final SysRoleMapper sysRoleMapper;
    private final SysPermissionMapper sysPermissionMapper;
    private final OrganizationMapper organizationMapper;

    /**
     * Login with username, password and tenantId
     */
    public Map<String, Object> login(LoginDTO loginDTO) {
        // Find user by username and tenantId
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysUser::getUsername, loginDTO.getUsername())
                .eq(SysUser::getTenantId, Long.parseLong(loginDTO.getTenantId()));
        SysUser user = sysUserMapper.selectOne(wrapper);

        if (user == null) {
            throw new BizException(401, "Invalid username or password");
        }

        // Check user status
        if (user.getStatus() == null || user.getStatus() != 1) {
            throw new BizException(403, "Account is disabled");
        }

        // Verify password using BCrypt
        if (!BCrypt.checkpw(loginDTO.getPassword(), user.getPasswordHash())) {
            throw new BizException(401, "Invalid username or password");
        }

        // Sa-Token login
        StpUtil.login(user.getId());
        String tokenValue = StpUtil.getTokenValue();

        // Store extra info in session
        StpUtil.getSession()
                .set("userId", user.getId())
                .set("tenantId", user.getTenantId())
                .set("username", user.getUsername());

        // Update last login info
        user.setLastLoginAt(LocalDateTime.now());
        sysUserMapper.updateById(user);

        // Set current user context
        CurrentUser currentUser = new CurrentUser();
        currentUser.setUserId(user.getId());
        currentUser.setTenantId(user.getTenantId());
        currentUser.setUsername(user.getUsername());
        currentUser.setRealName(user.getRealName());
        currentUser.setRoleCodes(new ArrayList<>());
        CurrentUser.set(currentUser);

        Map<String, Object> result = new HashMap<>();
        result.put("token", tokenValue);
        result.put("userId", user.getId());
        result.put("username", user.getUsername());
        return result;
    }

    /**
     * Logout
     */
    public void logout() {
        StpUtil.logout();
        CurrentUser.clear();
    }

    /**
     * Refresh token
     */
    public String refreshToken() {
        StpUtil.checkLogin();
        // Sa-Token auto-renews on activity, just return current token
        return StpUtil.getTokenValue();
    }

    /**
     * Get current user info with roles and permissions
     */
    public UserInfoVO getCurrentUser() {
        Long userId = StpUtil.getLoginIdAsLong();
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null) {
            throw new BizException(404, "User not found");
        }

        // Query roles
        List<RoleVO> roles = new ArrayList<>();
        List<Long> roleIds = sysUserRoleMapper.selectRoleIdsByUserId(userId);
        if (roleIds != null && !roleIds.isEmpty()) {
            String idsStr = roleIds.stream().map(String::valueOf).collect(Collectors.joining(","));
            List<Map<String, Object>> roleRows = sysRoleMapper.selectRolesByIds(idsStr);
            for (Map<String, Object> row : roleRows) {
                roles.add(RoleVO.builder()
                        .roleId(row.get("roleId") != null ? Long.valueOf(row.get("roleId").toString()) : null)
                        .roleCode((String) row.get("roleCode"))
                        .roleName((String) row.get("roleName"))
                        .build());
            }
        }

        // Query permissions from roles
        List<String> permissions = new ArrayList<>();
        if (roleIds != null && !roleIds.isEmpty()) {
            String idsStr = roleIds.stream().map(String::valueOf).collect(Collectors.joining(","));
            List<String> permCodes = sysPermissionMapper.selectPermissionCodesByRoleIds(idsStr);
            if (permCodes != null) {
                permissions = permCodes;
            }
        }

        // Resolve org name
        String orgName = null;
        if (user.getOrgId() != null) {
            orgName = organizationMapper.selectNameById(user.getOrgId());
        }

        return UserInfoVO.builder()
                .userId(user.getId())
                .tenantId(user.getTenantId())
                .username(user.getUsername())
                .realName(user.getRealName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .avatar(user.getAvatar())
                .orgId(user.getOrgId())
                .orgName(orgName)
                .roles(roles)
                .permissions(permissions)
                .build();
    }
}
