package com.recruitos.demand.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface JobDraftMapper {

    @Select("SELECT status FROM recruit_demand WHERE id = #{demandId} AND tenant_id = #{tenantId}")
    String selectDemandStatus(@Param("demandId") Long demandId, @Param("tenantId") Long tenantId);

    @Insert("INSERT INTO job_position (tenant_id, demand_id, job_no, title, jd_text, head_count, filled_count, status, created_at, updated_at) " +
            "VALUES (#{tenantId}, #{demandId}, #{jobNo}, #{title}, #{jdText}, #{headCount}, 0, 'DRAFT', NOW(), NOW())")
    int insertDraft(@Param("tenantId") Long tenantId,
                    @Param("demandId") Long demandId,
                    @Param("jobNo") String jobNo,
                    @Param("title") String title,
                    @Param("jdText") String jdText,
                    @Param("headCount") Integer headCount);
}
