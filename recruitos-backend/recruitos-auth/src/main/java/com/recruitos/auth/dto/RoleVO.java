package com.recruitos.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * Role view object
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoleVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long roleId;
    private String roleCode;
    private String roleName;
}
