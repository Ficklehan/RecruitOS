package com.recruitos.job.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface RecruitDemandReadMapper {

    @Select("SELECT status FROM recruit_demand WHERE id = #{demandId} AND tenant_id = #{tenantId}")
    String selectStatus(@Param("demandId") Long demandId, @Param("tenantId") Long tenantId);

    @Select("SELECT approved_head_count FROM recruit_demand WHERE id = #{demandId} AND tenant_id = #{tenantId}")
    Integer selectApprovedHeadCount(@Param("demandId") Long demandId, @Param("tenantId") Long tenantId);

    @Select("SELECT filled_count FROM recruit_demand WHERE id = #{demandId} AND tenant_id = #{tenantId}")
    Integer selectFilledCount(@Param("demandId") Long demandId, @Param("tenantId") Long tenantId);

    @Update("UPDATE recruit_demand SET status = #{status}, updated_at = NOW() WHERE id = #{demandId} AND tenant_id = #{tenantId}")
    int updateStatus(@Param("demandId") Long demandId, @Param("tenantId") Long tenantId, @Param("status") String status);

    @Update("UPDATE recruit_demand SET filled_count = #{filledCount}, updated_at = NOW() WHERE id = #{demandId} AND tenant_id = #{tenantId}")
    int updateFilledCount(@Param("demandId") Long demandId, @Param("tenantId") Long tenantId, @Param("filledCount") Integer filledCount);
}
