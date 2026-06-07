package com.recruitos.common.auth;

import java.io.Serializable;
import java.util.List;

/**
 * ThreadLocal holder for current logged-in user info
 */
public class CurrentUser implements Serializable {

    private static final long serialVersionUID = 1L;
    private static final ThreadLocal<CurrentUser> HOLDER = new ThreadLocal<>();

    private Long userId;
    private Long tenantId;
    private String username;
    private String realName;
    private List<String> roleCodes;

    public static void set(CurrentUser user) {
        HOLDER.set(user);
    }

    public static CurrentUser get() {
        return HOLDER.get();
    }

    public static Long getCurrentUserId() {
        CurrentUser user = HOLDER.get();
        return user != null ? user.getUserId() : null;
    }

    public static Long getCurrentTenantId() {
        CurrentUser user = HOLDER.get();
        return user != null ? user.getTenantId() : null;
    }

    public static void clear() {
        HOLDER.remove();
    }

    // Getters and Setters
    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getTenantId() {
        return tenantId;
    }

    public void setTenantId(Long tenantId) {
        this.tenantId = tenantId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getRealName() {
        return realName;
    }

    public void setRealName(String realName) {
        this.realName = realName;
    }

    public List<String> getRoleCodes() {
        return roleCodes;
    }

    public void setRoleCodes(List<String> roleCodes) {
        this.roleCodes = roleCodes;
    }
}
