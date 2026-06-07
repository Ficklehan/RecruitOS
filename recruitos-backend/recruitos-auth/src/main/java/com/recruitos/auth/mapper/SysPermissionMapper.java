package com.recruitos.auth.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface SysPermissionMapper {

    @Select("SELECT DISTINCT p.perm_code FROM sys_role_permission rp " +
            "JOIN sys_permission p ON rp.permission_id = p.id " +
            "WHERE rp.role_id IN (${roleIds})")
    List<String> selectPermissionCodesByRoleIds(@Param("roleIds") String roleIds);
}
