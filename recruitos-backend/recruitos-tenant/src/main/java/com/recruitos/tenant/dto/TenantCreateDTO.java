package com.recruitos.tenant.dto;

import javax.validation.constraints.NotBlank;
import java.io.Serializable;

public class TenantCreateDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    @NotBlank(message = "租户编码不能为空")
    private String tenantCode;

    @NotBlank(message = "公司名称不能为空")
    private String companyName;

    private String creditCode;
    private String plan = "STARTER";

    private String adminUsername = "admin";
    private String adminPassword = "123";
    private String adminRealName = "管理员";
    private String adminEmail;
    private String adminPhone;

    public String getTenantCode() { return tenantCode; }
    public void setTenantCode(String tenantCode) { this.tenantCode = tenantCode; }
    public String getCompanyName() { return companyName; }
    public void setCompanyName(String companyName) { this.companyName = companyName; }
    public String getCreditCode() { return creditCode; }
    public void setCreditCode(String creditCode) { this.creditCode = creditCode; }
    public String getPlan() { return plan; }
    public void setPlan(String plan) { this.plan = plan; }
    public String getAdminUsername() { return adminUsername; }
    public void setAdminUsername(String adminUsername) { this.adminUsername = adminUsername; }
    public String getAdminPassword() { return adminPassword; }
    public void setAdminPassword(String adminPassword) { this.adminPassword = adminPassword; }
    public String getAdminRealName() { return adminRealName; }
    public void setAdminRealName(String adminRealName) { this.adminRealName = adminRealName; }
    public String getAdminEmail() { return adminEmail; }
    public void setAdminEmail(String adminEmail) { this.adminEmail = adminEmail; }
    public String getAdminPhone() { return adminPhone; }
    public void setAdminPhone(String adminPhone) { this.adminPhone = adminPhone; }
}
