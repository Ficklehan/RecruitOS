package com.recruitos.auth.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface OrganizationMapper {

    @Select("SELECT name FROM organization WHERE id = #{orgId}")
    String selectNameById(@Param("orgId") Long orgId);
}
