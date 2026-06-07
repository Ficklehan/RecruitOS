package com.recruitos.auth.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface SysRoleMapper {

    @Select("SELECT id as roleId, role_code as roleCode, role_name as roleName FROM sys_role WHERE id IN (${roleIds})")
    List<Map<String, Object>> selectRolesByIds(@Param("roleIds") String roleIds);

    @Select("SELECT id as roleId, role_code as roleCode, role_name as roleName FROM sys_role WHERE id = #{roleId}")
    Map<String, Object> selectRoleById(@Param("roleId") Long roleId);
}
