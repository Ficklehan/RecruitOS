package com.recruitos.offer.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface HiringDecisionReadMapper {

    @Select("SELECT COUNT(1) FROM hiring_decision hd " +
            "JOIN candidate_job cj ON hd.candidate_job_id = cj.id " +
            "WHERE cj.tenant_id = #{tenantId} AND cj.candidate_id = #{candidateId} AND cj.job_id = #{jobId} " +
            "AND hd.decision = 'HIRE'")
    int countHireDecision(@Param("tenantId") Long tenantId,
                          @Param("candidateId") Long candidateId,
                          @Param("jobId") Long jobId);
}
