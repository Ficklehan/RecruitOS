package com.recruitos.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * Current user info view object
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserInfoVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long userId;
    private Long tenantId;
    private String username;
    private String realName;
    private String email;
    private String phone;
    private String avatar;
    private Long orgId;
    private String orgName;
    private List<RoleVO> roles;
    private List<String> permissions;
}
