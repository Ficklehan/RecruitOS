package com.recruitos.common.tenant;

/**
 * ThreadLocal holder for current tenant ID
 */
public class TenantContext {

    private static final ThreadLocal<Long> TENANT_ID = new ThreadLocal<>();

    public static void setTenantId(Long tenantId) {
        TENANT_ID.set(tenantId);
    }

    public static Long getTenantId() {
        Long id = TENANT_ID.get();
        return id != null ? id : 1L; // Default to tenant 1
    }

    public static void clear() {
        TENANT_ID.remove();
    }
}
