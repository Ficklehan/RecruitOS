package com.recruitos.onboard.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface HeadcountWriteMapper {

    @Select("SELECT demand_id FROM job_position WHERE id = #{jobId} AND tenant_id = #{tenantId}")
    Long selectDemandId(@Param("jobId") Long jobId, @Param("tenantId") Long tenantId);

    @Update("UPDATE job_position SET filled_count = IFNULL(filled_count, 0) + 1, updated_at = NOW() " +
            "WHERE id = #{jobId} AND tenant_id = #{tenantId}")
    int incrementJobFilled(@Param("jobId") Long jobId, @Param("tenantId") Long tenantId);

    @Update("UPDATE recruit_demand SET filled_count = IFNULL(filled_count, 0) + 1, updated_at = NOW() " +
            "WHERE id = #{demandId} AND tenant_id = #{tenantId}")
    int incrementDemandFilled(@Param("demandId") Long demandId, @Param("tenantId") Long tenantId);

    @Update("UPDATE candidate_job SET pipeline_stage = 'HIRED', updated_at = NOW() " +
            "WHERE tenant_id = #{tenantId} AND candidate_id = #{candidateId} AND job_id = #{jobId}")
    int markCandidateHired(@Param("tenantId") Long tenantId,
                           @Param("candidateId") Long candidateId,
                           @Param("jobId") Long jobId);

    @Update("UPDATE candidate SET status = 'ONBOARD', updated_at = NOW() " +
            "WHERE id = #{candidateId} AND tenant_id = #{tenantId}")
    int markCandidateOnboard(@Param("candidateId") Long candidateId, @Param("tenantId") Long tenantId);
}
