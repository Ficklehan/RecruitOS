package com.recruitos.offer.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface CandidateJobWriteMapper {

    @Update("UPDATE candidate_job SET pipeline_stage = #{stage}, updated_at = NOW() " +
            "WHERE tenant_id = #{tenantId} AND candidate_id = #{candidateId} AND job_id = #{jobId}")
    int updatePipelineStage(@Param("tenantId") Long tenantId,
                            @Param("candidateId") Long candidateId,
                            @Param("jobId") Long jobId,
                            @Param("stage") String stage);
}
