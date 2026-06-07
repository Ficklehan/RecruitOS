package com.recruitos.common.tenant;

import cn.dev33.satoken.stp.StpUtil;
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

        if (StringUtils.hasText(tenantIdStr)) {
            TenantContext.setTenantId(Long.parseLong(tenantIdStr));
        } else {
            // Try to get from Sa-Token session
            try {
                if (StpUtil.isLogin()) {
                    Object tenantId = StpUtil.getSession().get("tenantId");
                    if (tenantId != null) {
                        TenantContext.setTenantId(Long.parseLong(tenantId.toString()));
                    }
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
    }
}
