package com.recruitos.tenant.entity;

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
 * Tenant entity
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("tenant")
public class Tenant extends BaseEntity {

    @TableField(exist = false)
    private Long tenantId;

    @TableField("tenant_code")
    private String tenantCode;

    @TableField("company_name")
    private String companyName;

    @TableField("credit_code")
    private String creditCode;

    @TableField("plan")
    private String plan;

    @TableField("status")
    private Integer status;

    @TableField("trial_end_time")
    private LocalDateTime trialEndTime;

    @TableField("config_json")
    private String configJson;
}
