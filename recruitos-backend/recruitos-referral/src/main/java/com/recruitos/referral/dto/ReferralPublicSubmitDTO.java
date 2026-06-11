package com.recruitos.referral.dto;

import javax.validation.constraints.NotBlank;
import java.io.Serializable;

public class ReferralPublicSubmitDTO implements Serializable {

    @NotBlank
    private String token;
    @NotBlank
    private String candidateName;
    private String phone;
    private String email;
    private String remark;

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }
    public String getCandidateName() { return candidateName; }
    public void setCandidateName(String candidateName) { this.candidateName = candidateName; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
}
