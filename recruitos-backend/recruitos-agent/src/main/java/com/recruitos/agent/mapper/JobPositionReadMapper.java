package com.recruitos.agent.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface JobPositionReadMapper {

    @Select("SELECT status FROM job_position WHERE id = #{jobId} AND tenant_id = #{tenantId}")
    String selectStatus(@Param("jobId") Long jobId, @Param("tenantId") Long tenantId);
}
