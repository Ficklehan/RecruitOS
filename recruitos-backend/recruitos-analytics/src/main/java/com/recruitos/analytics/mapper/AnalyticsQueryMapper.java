package com.recruitos.analytics.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface AnalyticsQueryMapper {

    // Funnel: count candidates by status
    @Select("SELECT status, COUNT(*) as cnt FROM candidate WHERE tenant_id = #{tenantId} " +
            "AND (#{dateFrom} IS NULL OR created_at >= #{dateFrom}) " +
            "AND (#{dateTo} IS NULL OR created_at <= #{dateTo}) " +
            "GROUP BY status")
    List<Map<String, Object>> countCandidatesByStatus(@Param("tenantId") Long tenantId,
                                                       @Param("dateFrom") String dateFrom,
                                                       @Param("dateTo") String dateTo);

    // Funnel: count interviews by round
    @Select("SELECT round, COUNT(*) as cnt FROM interview WHERE tenant_id = #{tenantId} " +
            "AND (#{dateFrom} IS NULL OR created_at >= #{dateFrom}) " +
            "AND (#{dateTo} IS NULL OR created_at <= #{dateTo}) " +
            "GROUP BY round")
    List<Map<String, Object>> countInterviewsByRound(@Param("tenantId") Long tenantId,
                                                      @Param("dateFrom") String dateFrom,
                                                      @Param("dateTo") String dateTo);

    // Funnel: count offers by status
    @Select("SELECT status, COUNT(*) as cnt FROM offer WHERE tenant_id = #{tenantId} " +
            "AND (#{dateFrom} IS NULL OR created_at >= #{dateFrom}) " +
            "AND (#{dateTo} IS NULL OR created_at <= #{dateTo}) " +
            "GROUP BY status")
    List<Map<String, Object>> countOffersByStatus(@Param("tenantId") Long tenantId,
                                                   @Param("dateFrom") String dateFrom,
                                                   @Param("dateTo") String dateTo);

    // Funnel: count onboard records
    @Select("SELECT COUNT(*) as cnt FROM onboard WHERE tenant_id = #{tenantId} " +
            "AND (#{dateFrom} IS NULL OR created_at >= #{dateFrom}) " +
            "AND (#{dateTo} IS NULL OR created_at <= #{dateTo})")
    int countOnboard(@Param("tenantId") Long tenantId,
                     @Param("dateFrom") String dateFrom,
                     @Param("dateTo") String dateTo);

    // ROI: count candidates by source
    @Select("SELECT source, COUNT(*) as cnt FROM candidate WHERE tenant_id = #{tenantId} " +
            "AND (#{dateFrom} IS NULL OR created_at >= #{dateFrom}) " +
            "AND (#{dateTo} IS NULL OR created_at <= #{dateTo}) " +
            "GROUP BY source")
    List<Map<String, Object>> countCandidatesBySource(@Param("tenantId") Long tenantId,
                                                       @Param("dateFrom") String dateFrom,
                                                       @Param("dateTo") String dateTo);

    // Interviewer: stats from interview + evaluation
    @Select("SELECT u.real_name as interviewerName, COUNT(DISTINCT i.id) as interviewCount, " +
            "SUM(CASE WHEN e.decision = 'PASS' THEN 1 ELSE 0 END) as passCount, " +
            "AVG(e.overall_score) as avgScore " +
            "FROM interview i " +
            "LEFT JOIN sys_user u ON i.interviewer_id = u.id " +
            "LEFT JOIN interview_evaluation e ON e.interview_id = i.id " +
            "WHERE i.tenant_id = #{tenantId} AND i.interviewer_id IS NOT NULL " +
            "GROUP BY i.interviewer_id, u.real_name")
    List<Map<String, Object>> getInterviewerStats(@Param("tenantId") Long tenantId);

    // Cycle: average days from demand creation to approval
    @Select("SELECT AVG(TIMESTAMPDIFF(HOUR, d.created_at, a.created_at) / 24.0) as avgDays " +
            "FROM recruit_demand d " +
            "JOIN approval_instance ai ON ai.biz_type = 'DEMAND' AND ai.biz_id = d.id AND ai.status = 'APPROVED' " +
            "JOIN approval_record a ON a.instance_id = ai.id AND a.action = 'APPROVE' " +
            "WHERE d.tenant_id = #{tenantId} " +
            "AND (#{dateFrom} IS NULL OR d.created_at >= #{dateFrom}) " +
            "AND (#{dateTo} IS NULL OR d.created_at <= #{dateTo})")
    Double getAvgDemandApprovalDays(@Param("tenantId") Long tenantId,
                                     @Param("dateFrom") String dateFrom,
                                     @Param("dateTo") String dateTo);

    // Cycle: average days from interview arrange to complete
    @Select("SELECT AVG(TIMESTAMPDIFF(HOUR, scheduled_start_time, actual_end_time) / 24.0) as avgDays " +
            "FROM interview " +
            "WHERE tenant_id = #{tenantId} AND status = 'COMPLETED' " +
            "AND actual_end_time IS NOT NULL " +
            "AND (#{dateFrom} IS NULL OR created_at >= #{dateFrom}) " +
            "AND (#{dateTo} IS NULL OR created_at <= #{dateTo})")
    Double getAvgInterviewCycleDays(@Param("tenantId") Long tenantId,
                                     @Param("dateFrom") String dateFrom,
                                     @Param("dateTo") String dateTo);

    // Cycle: average days from offer to onboard
    @Select("SELECT AVG(TIMESTAMPDIFF(HOUR, o.created_at, ob.onboard_date) / 24.0) as avgDays " +
            "FROM offer o " +
            "JOIN onboard ob ON ob.offer_id = o.id " +
            "WHERE o.tenant_id = #{tenantId} AND o.status = 'ACCEPTED' " +
            "AND ob.onboard_date IS NOT NULL " +
            "AND (#{dateFrom} IS NULL OR o.created_at >= #{dateFrom}) " +
            "AND (#{dateTo} IS NULL OR o.created_at <= #{dateTo})")
    Double getAvgOfferToOnboardDays(@Param("tenantId") Long tenantId,
                                     @Param("dateFrom") String dateFrom,
                                     @Param("dateTo") String dateTo);
}
