package com.recruitos.candidate.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface JobPositionReadMapper {

    @Select("SELECT title FROM job_position WHERE id = #{id} AND tenant_id = #{tenantId}")
    String selectTitle(@Param("id") Long id, @Param("tenantId") Long tenantId);

    @Select("SELECT status FROM job_position WHERE id = #{id} AND tenant_id = #{tenantId}")
    String selectStatus(@Param("id") Long id, @Param("tenantId") Long tenantId);

    @Select("SELECT tags FROM job_position WHERE id = #{id} AND tenant_id = #{tenantId}")
    String selectTags(@Param("id") Long id, @Param("tenantId") Long tenantId);
}
