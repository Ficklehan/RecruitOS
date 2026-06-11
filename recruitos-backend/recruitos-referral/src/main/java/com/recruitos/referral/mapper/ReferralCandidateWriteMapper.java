package com.recruitos.referral.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@org.apache.ibatis.annotations.Mapper
public interface ReferralCandidateWriteMapper {

    @Select("SELECT title FROM job_position WHERE id = #{jobId} AND tenant_id = #{tenantId}")
    String selectJobTitle(@Param("tenantId") Long tenantId, @Param("jobId") Long jobId);

    @Insert("INSERT INTO candidate (id, tenant_id, name, phone, email, source, source_detail, status, created_at, updated_at) " +
            "VALUES (#{id}, #{tenantId}, #{name}, #{phone}, #{email}, 'REFERRAL', #{sourceDetail}, 'NEW', NOW(), NOW())")
    int insertCandidate(@Param("id") Long id, @Param("tenantId") Long tenantId, @Param("name") String name,
                        @Param("phone") String phone, @Param("email") String email, @Param("sourceDetail") String sourceDetail);

    @Insert("INSERT INTO candidate_job (id, tenant_id, candidate_id, job_id, screening_status, pipeline_stage, created_at, updated_at) " +
            "VALUES (#{id}, #{tenantId}, #{candidateId}, #{jobId}, 'PENDING', 'SOURCED', NOW(), NOW())")
    int insertCandidateJob(@Param("id") Long id, @Param("tenantId") Long tenantId,
                           @Param("candidateId") Long candidateId, @Param("jobId") Long jobId);
}
