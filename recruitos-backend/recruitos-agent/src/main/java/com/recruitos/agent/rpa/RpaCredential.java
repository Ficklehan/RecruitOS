package com.recruitos.agent.rpa;

/**
 * 平台账号凭证 JSON（存于 agent_account.encrypted_credential，生产环境应 KMS 加密）
 */
public class RpaCredential {

    /** manual | phone | cookie */
    private String authMode = "manual";

    private String phone;
    private String password;

    public String getAuthMode() {
        return authMode;
    }

    public void setAuthMode(String authMode) {
        this.authMode = authMode;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
