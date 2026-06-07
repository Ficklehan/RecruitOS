package com.recruitos.tenant.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.recruitos.tenant.entity.SysUserRole;
import org.apache.ibatis.annotations.Mapper;

/**
 * User-Role relation mapper
 */
@Mapper
public interface SysUserRoleMapper extends BaseMapper<SysUserRole> {
}
