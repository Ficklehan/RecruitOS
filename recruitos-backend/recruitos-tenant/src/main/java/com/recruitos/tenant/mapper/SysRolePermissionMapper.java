package com.recruitos.tenant.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.recruitos.tenant.entity.SysRolePermission;
import org.apache.ibatis.annotations.Mapper;

/**
 * Role-Permission relation mapper
 */
@Mapper
public interface SysRolePermissionMapper extends BaseMapper<SysRolePermission> {
}
