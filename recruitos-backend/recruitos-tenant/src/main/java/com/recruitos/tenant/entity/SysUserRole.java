package com.recruitos.tenant.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.recruitos.common.mybatis.BaseEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * User-Role relation entity
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("sys_user_role")
public class SysUserRole extends BaseEntity {

    @TableField("user_id")
    private Long userId;

    @TableField("role_id")
    private Long roleId;

    @TableField("scope_type")
    private String scopeType;

    @TableField("scope_ids")
    private String scopeIds;
}
