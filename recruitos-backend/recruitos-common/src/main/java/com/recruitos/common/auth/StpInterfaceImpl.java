package com.recruitos.common.auth;

import cn.dev33.satoken.stp.StpInterface;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Sa-Token StpInterface implementation
 * Resolves permissions and roles from CurrentUser
 */
@Component
public class StpInterfaceImpl implements StpInterface {

    @Override
    public List<String> getPermissionList(Object loginId, String loginType) {
        // Check platform admin session
        try {
            Object isPlatformAdmin = cn.dev33.satoken.stp.StpUtil.getSession().get("isPlatformAdmin");
            if (Boolean.TRUE.equals(isPlatformAdmin)) {
                return Collections.singletonList("PLATFORM_ADMIN");
            }
        } catch (Exception ignored) {}
        CurrentUser user = CurrentUser.get();
        if (user != null && user.getRoleCodes() != null) {
            return new ArrayList<>(user.getRoleCodes());
        }
        return Collections.emptyList();
    }

    @Override
    public List<String> getRoleList(Object loginId, String loginType) {
        // Check platform admin session
        try {
            Object isPlatformAdmin = cn.dev33.satoken.stp.StpUtil.getSession().get("isPlatformAdmin");
            if (Boolean.TRUE.equals(isPlatformAdmin)) {
                return Collections.singletonList("PLATFORM_ADMIN");
            }
        } catch (Exception ignored) {}
        CurrentUser user = CurrentUser.get();
        if (user != null && user.getRoleCodes() != null) {
            return new ArrayList<>(user.getRoleCodes());
        }
        return Collections.emptyList();
    }
}
