package com.recruitos.tenant.service;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.recruitos.common.exception.BizException;
import com.recruitos.tenant.dto.PlatformAdminLoginDTO;
import com.recruitos.tenant.entity.PlatformAdmin;
import com.recruitos.tenant.mapper.PlatformAdminMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PlatformAdminService {

    private final PlatformAdminMapper platformAdminMapper;
    private static final BCryptPasswordEncoder ENCODER = new BCryptPasswordEncoder();

    public Map<String, Object> login(PlatformAdminLoginDTO dto) {
        LambdaQueryWrapper<PlatformAdmin> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PlatformAdmin::getUsername, dto.getUsername());
        PlatformAdmin admin = platformAdminMapper.selectOne(wrapper);

        if (admin == null) {
            throw new BizException(401, "用户名或密码错误");
        }
        if (admin.getStatus() != 1) {
            throw new BizException(403, "账号已禁用");
        }
        if (!ENCODER.matches(dto.getPassword(), admin.getPasswordHash())) {
            throw new BizException(401, "用户名或密码错误");
        }

        // Update last login time
        admin.setLastLoginAt(LocalDateTime.now());
        platformAdminMapper.updateById(admin);

        // Sa-Token login with platform admin ID (prefix with 999999 to avoid collision with tenant users)
        long loginId = 999999000000L + admin.getId();
        StpUtil.login(loginId);
        StpUtil.getSession().set("isPlatformAdmin", true);
        StpUtil.getSession().set("platformAdminId", admin.getId());
        StpUtil.getSession().set("platformAdminUsername", admin.getUsername());
        StpUtil.getSession().set("platformAdminRealName", admin.getRealName());

        Map<String, Object> result = new HashMap<>();
        result.put("token", StpUtil.getTokenValue());
        result.put("adminId", admin.getId());
        result.put("username", admin.getUsername());
        result.put("realName", admin.getRealName());
        return result;
    }

    public Map<String, Object> getCurrentAdmin() {
        try {
            Object isPlatformAdmin = StpUtil.getSession().get("isPlatformAdmin");
            if (!Boolean.TRUE.equals(isPlatformAdmin)) {
                throw new BizException(401, "未登录平台管理后台");
            }
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            throw new BizException(401, "未登录平台管理后台");
        }

        Map<String, Object> info = new HashMap<>();
        info.put("adminId", StpUtil.getSession().get("platformAdminId"));
        info.put("username", StpUtil.getSession().get("platformAdminUsername"));
        info.put("realName", StpUtil.getSession().get("platformAdminRealName"));
        info.put("isPlatformAdmin", true);
        return info;
    }

    public void logout() {
        try {
            StpUtil.logout();
        } catch (Exception ignored) {
        }
    }
}
