package com.recruitos.common.tenant;

import cn.dev33.satoken.stp.StpUtil;
import com.recruitos.common.auth.CurrentUser;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Tenant interceptor: resolves tenant ID from request header X-Tenant-Id or JWT
 */
@Component
public class TenantInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // First try to get tenant ID from header
        String tenantIdStr = request.getHeader("X-Tenant-Id");

        Long tenantId = null;
        if (StringUtils.hasText(tenantIdStr)) {
            tenantId = Long.parseLong(tenantIdStr);
            TenantContext.setTenantId(tenantId);
        } else {
            try {
                if (StpUtil.isLogin()) {
                    Object tid = StpUtil.getSession().get("tenantId");
                    if (tid != null) {
                        tenantId = Long.parseLong(tid.toString());
                        TenantContext.setTenantId(tenantId);
                    }
                    CurrentUser cu = new CurrentUser();
                    cu.setUserId(StpUtil.getLoginIdAsLong());
                    cu.setTenantId(tenantId);
                    Object username = StpUtil.getSession().get("username");
                    if (username != null) {
                        cu.setUsername(username.toString());
                    }
                    CurrentUser.set(cu);
                }
            } catch (Exception ignored) {
                // Not logged in or no tenant info
            }
        }

        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        TenantContext.clear();
        CurrentUser.clear();
    }
}
