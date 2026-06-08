package com.recruitos.agent.mapper;

import org.apache.ibatis.annotations.*;

@Mapper
public interface CandidateImportMapper {

    @Select("SELECT id FROM candidate WHERE tenant_id = #{tenantId} AND phone = #{phone} LIMIT 1")
    Long findCandidateIdByPhone(@Param("tenantId") Long tenantId, @Param("phone") String phone);

    @Select("SELECT id FROM candidate WHERE tenant_id = #{tenantId} AND email = #{email} LIMIT 1")
    Long findCandidateIdByEmail(@Param("tenantId") Long tenantId, @Param("email") String email);

    @Select("SELECT COUNT(1) FROM candidate_job WHERE tenant_id = #{tenantId} AND candidate_id = #{candidateId} AND job_id = #{jobId}")
    int countCandidateJob(@Param("tenantId") Long tenantId, @Param("candidateId") Long candidateId, @Param("jobId") Long jobId);

    @Select("SELECT match_detail FROM candidate_job WHERE tenant_id = #{tenantId} AND candidate_id = #{candidateId} AND job_id = #{jobId} LIMIT 1")
    String selectMatchDetail(@Param("tenantId") Long tenantId, @Param("candidateId") Long candidateId, @Param("jobId") Long jobId);

    @Select("SELECT COUNT(1) FROM candidate_job WHERE tenant_id = #{tenantId} AND job_id = #{jobId} AND match_score IS NOT NULL")
    int countJobRanked(@Param("tenantId") Long tenantId, @Param("jobId") Long jobId);

    @Select("SELECT COUNT(1) FROM candidate_job WHERE tenant_id = #{tenantId} AND job_id = #{jobId} AND match_score > #{score}")
    int countHigherScores(@Param("tenantId") Long tenantId, @Param("jobId") Long jobId, @Param("score") double score);

    @Insert("INSERT INTO candidate (id, tenant_id, name, phone, email, current_company, current_title, work_years, source, source_detail, status, created_at, updated_at) " +
            "VALUES (#{id}, #{tenantId}, #{name}, #{phone}, #{email}, #{company}, #{title}, #{workYears}, 'PLATFORM', #{sourceDetail}, 'NEW', NOW(), NOW())")
    int insertCandidate(@Param("id") Long id, @Param("tenantId") Long tenantId, @Param("name") String name,
                        @Param("phone") String phone, @Param("email") String email,
                        @Param("company") String company, @Param("title") String title,
                        @Param("workYears") Integer workYears, @Param("sourceDetail") String sourceDetail);

    @Insert("INSERT INTO resume (id, tenant_id, candidate_id, file_name, file_url, file_type, parsed_json, raw_text, parse_status, version, created_at) " +
            "VALUES (#{id}, #{tenantId}, #{candidateId}, #{fileName}, #{fileUrl}, 'pdf', #{parsedJson}, #{rawText}, #{parseStatus}, 1, NOW())")
    int insertResume(@Param("id") Long id, @Param("tenantId") Long tenantId, @Param("candidateId") Long candidateId,
                     @Param("fileName") String fileName, @Param("fileUrl") String fileUrl,
                     @Param("parsedJson") String parsedJson, @Param("rawText") String rawText,
                     @Param("parseStatus") String parseStatus);

    @Insert("INSERT INTO candidate_job (id, tenant_id, candidate_id, job_id, match_score, match_detail, screening_status, pipeline_stage, created_at, updated_at) " +
            "VALUES (#{id}, #{tenantId}, #{candidateId}, #{jobId}, #{matchScore}, #{matchDetail}, 'PENDING', 'SOURCED', NOW(), NOW())")
    int insertCandidateJob(@Param("id") Long id, @Param("tenantId") Long tenantId, @Param("candidateId") Long candidateId,
                           @Param("jobId") Long jobId, @Param("matchScore") java.math.BigDecimal matchScore,
                           @Param("matchDetail") String matchDetail);

    @Select("SELECT title FROM job_position WHERE id = #{jobId} AND tenant_id = #{tenantId}")
    String selectJobTitle(@Param("jobId") Long jobId, @Param("tenantId") Long tenantId);

    @Select("SELECT jd_text FROM job_position WHERE id = #{jobId} AND tenant_id = #{tenantId}")
    String selectJobJdText(@Param("jobId") Long jobId, @Param("tenantId") Long tenantId);

    @Select("SELECT tags FROM job_position WHERE id = #{jobId} AND tenant_id = #{tenantId}")
    String selectJobTags(@Param("jobId") Long jobId, @Param("tenantId") Long tenantId);
}
