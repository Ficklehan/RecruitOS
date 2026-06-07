package com.recruitos.auth.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.recruitos.common.mybatis.BaseEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * System user entity
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("sys_user")
public class SysUser extends BaseEntity {

    @TableField("username")
    private String username;

    @TableField("password_hash")
    private String passwordHash;

    @TableField("real_name")
    private String realName;

    @TableField("email")
    private String email;

    @TableField("phone")
    private String phone;

    @TableField("avatar")
    private String avatar;

    @TableField("org_id")
    private Long orgId;

    @TableField("mfa_secret")
    private String mfaSecret;

    @TableField("sso_type")
    private String ssoType;

    @TableField("sso_external_id")
    private String ssoExternalId;

    @TableField("status")
    private Integer status;

    @TableField("last_login_at")
    private LocalDateTime lastLoginAt;

    @TableField("last_login_ip")
    private String lastLoginIp;
}
