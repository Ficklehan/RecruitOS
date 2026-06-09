package com.recruitos.agent.mapper;

import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.Map;

public interface OpsPackReadMapper {

    @Select("SELECT id, job_id AS jobId, version, status, pack_json AS packJson " +
            "FROM job_sourcing_ops_pack " +
            "WHERE tenant_id = #{tenantId} AND job_id = #{jobId} AND status = 'ACTIVE' " +
            "ORDER BY version DESC LIMIT 1")
    Map<String, Object> selectActive(@Param("jobId") Long jobId, @Param("tenantId") Long tenantId);

    @Select("SELECT id, job_id AS jobId, version, status, pack_json AS packJson " +
            "FROM job_sourcing_ops_pack " +
            "WHERE tenant_id = #{tenantId} AND id = #{packId} AND job_id = #{jobId} LIMIT 1")
    Map<String, Object> selectById(@Param("jobId") Long jobId, @Param("packId") Long packId,
                                   @Param("tenantId") Long tenantId);
}
