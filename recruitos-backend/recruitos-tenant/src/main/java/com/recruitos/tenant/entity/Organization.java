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
 * Organization entity
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("organization")
public class Organization extends BaseEntity {

    @TableField("parent_id")
    private Long parentId;

    @TableField("name")
    private String name;

    @TableField("type")
    private String type;

    @TableField("leader_id")
    private Long leaderId;

    @TableField("sort_order")
    private Integer sortOrder;

    @TableField("status")
    private Integer status;
}
