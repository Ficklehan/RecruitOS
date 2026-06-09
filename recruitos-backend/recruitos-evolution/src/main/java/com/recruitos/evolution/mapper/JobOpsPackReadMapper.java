package com.recruitos.evolution.mapper;

import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.Map;

public interface JobOpsPackReadMapper {

    @Select("SELECT id, version, pack_json AS packJson, status FROM job_sourcing_ops_pack " +
            "WHERE tenant_id = #{tenantId} AND job_id = #{jobId} AND status = 'ACTIVE' " +
            "ORDER BY version DESC LIMIT 1")
    Map<String, Object> selectActivePack(@Param("tenantId") Long tenantId, @Param("jobId") Long jobId);

    @Select("SELECT id, version, pack_json AS packJson FROM job_sourcing_ops_pack " +
            "WHERE tenant_id = #{tenantId} AND job_id = #{jobId} AND status = 'ARCHIVED' " +
            "ORDER BY version DESC LIMIT 1")
    Map<String, Object> selectLatestArchivedPack(@Param("tenantId") Long tenantId, @Param("jobId") Long jobId);
}
