package com.recruitos.interview.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface SysUserMapper {

    @Select("SELECT real_name FROM sys_user WHERE id = #{id}")
    String selectRealNameById(@Param("id") Long id);
}
