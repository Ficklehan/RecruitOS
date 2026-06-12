package com.recruitos.brain.mapper;

import org.apache.ibatis.annotations.*;
import java.util.*;

/**
 * 认知层 MyBatis Mapper
 * 操作 7 张 cognitive_* 表
 */
@Mapper
public interface CognitiveMapper {

    // ================================================================
    // cognitive_event_memory
    // ================================================================
    @Insert("INSERT INTO cognitive_event_memory (id, tenant_id, event_type, event_subject, subject_id, context_json, outcome, outcome_reason, decision_quality, occurred_at, recorded_at) " +
            "VALUES (#{id}, #{tenantId}, #{eventType}, #{eventSubject}, #{subjectId}, #{contextJson}, #{outcome}, #{outcomeReason}, #{decisionQuality}, #{occurredAt}, NOW())")
    void insertEvent(Map<String, Object> params);

    @Select("SELECT * FROM cognitive_event_memory WHERE tenant_id = #{tenantId} AND event_subject = #{eventSubject} AND subject_id = #{subjectId} ORDER BY occurred_at DESC")
    List<Map<String, Object>> findEventsBySubject(@Param("tenantId") Long tenantId, @Param("eventSubject") String eventSubject, @Param("subjectId") Long subjectId);

    @Select("SELECT * FROM cognitive_event_memory WHERE tenant_id = #{tenantId} AND event_type = #{eventType} ORDER BY occurred_at DESC LIMIT #{limit}")
    List<Map<String, Object>> findEventsByType(@Param("tenantId") Long tenantId, @Param("eventType") String eventType, @Param("limit") int limit);

    @Select("SELECT * FROM cognitive_event_memory WHERE tenant_id = #{tenantId} AND decision_quality = 'POOR' ORDER BY occurred_at DESC")
    List<Map<String, Object>> findPoorDecisions(@Param("tenantId") Long tenantId);

    @Update("UPDATE cognitive_event_memory SET decision_quality = #{quality}, outcome_reason = #{reason} WHERE id = #{id}")
    void updateEventQuality(@Param("id") Long id, @Param("quality") String quality, @Param("reason") String reason);

    // ================================================================
    // cognitive_pattern_memory
    // ================================================================
    @Insert("INSERT INTO cognitive_pattern_memory (id, tenant_id, pattern_type, pattern_name, pattern_rule, evidence_events, confidence, sample_size, status, discovered_at) " +
            "VALUES (#{id}, #{tenantId}, #{patternType}, #{patternName}, #{patternRule}, #{evidenceEvents}, #{confidence}, #{sampleSize}, 'ACTIVE', NOW())")
    void insertPattern(Map<String, Object> params);

    @Select("SELECT * FROM cognitive_pattern_memory WHERE tenant_id = #{tenantId} AND status = 'ACTIVE' ORDER BY confidence DESC")
    List<Map<String, Object>> findActivePatterns(@Param("tenantId") Long tenantId);

    @Select("SELECT * FROM cognitive_pattern_memory WHERE tenant_id = #{tenantId} AND pattern_type = #{patternType} AND status = 'ACTIVE'")
    List<Map<String, Object>> findPatternsByType(@Param("tenantId") Long tenantId, @Param("patternType") String patternType);

    @Update("UPDATE cognitive_pattern_memory SET status = #{status}, last_validated_at = NOW() WHERE id = #{id}")
    void updatePatternStatus(@Param("id") Long id, @Param("status") String status);

    // ================================================================
    // cognitive_object_memory
    // ================================================================
    @Insert("INSERT INTO cognitive_object_memory (id, tenant_id, object_type, object_id, summary_tldr, evolving_profile, key_signals, risk_flags, last_updated) " +
            "VALUES (#{id}, #{tenantId}, #{objectType}, #{objectId}, #{summaryTldr}, #{evolvingProfile}, #{keySignals}, #{riskFlags}, NOW()) " +
            "ON DUPLICATE KEY UPDATE summary_tldr = VALUES(summary_tldr), evolving_profile = VALUES(evolving_profile), key_signals = VALUES(key_signals), risk_flags = VALUES(risk_flags), last_updated = NOW()")
    void upsertObject(Map<String, Object> params);

    @Select("SELECT * FROM cognitive_object_memory WHERE tenant_id = #{tenantId} AND object_type = #{objectType} AND object_id = #{objectId}")
    Map<String, Object> findObject(@Param("tenantId") Long tenantId, @Param("objectType") String objectType, @Param("objectId") Long objectId);

    @Select("SELECT * FROM cognitive_object_memory WHERE tenant_id = #{tenantId} AND object_type = #{objectType} ORDER BY last_updated DESC LIMIT #{limit}")
    List<Map<String, Object>> findObjectsByType(@Param("tenantId") Long tenantId, @Param("objectType") String objectType, @Param("limit") int limit);

    // ================================================================
    // cognitive_lesson_memory
    // ================================================================
    @Insert("INSERT INTO cognitive_lesson_memory (id, tenant_id, lesson_type, title, description, evidence, corrective_action, severity, status, learned_at) " +
            "VALUES (#{id}, #{tenantId}, #{lessonType}, #{title}, #{description}, #{evidence}, #{correctiveAction}, #{severity}, 'ACTIVE', NOW())")
    void insertLesson(Map<String, Object> params);

    @Select("SELECT * FROM cognitive_lesson_memory WHERE tenant_id = #{tenantId} AND status = 'ACTIVE' ORDER BY FIELD(severity, 'CRITICAL','IMPORTANT','NOTABLE'), learned_at DESC")
    List<Map<String, Object>> findActiveLessons(@Param("tenantId") Long tenantId);

    @Update("UPDATE cognitive_lesson_memory SET status = #{status}, addressed_at = NOW() WHERE id = #{id}")
    void updateLessonStatus(@Param("id") Long id, @Param("status") String status);

    // ================================================================
    // cognitive_judgment
    // ================================================================
    @Insert("INSERT INTO cognitive_judgment (id, tenant_id, judgment_type, subject_type, subject_id, judgment_text, judgment_json, confidence, evidence_memory, contradiction, status, created_at) " +
            "VALUES (#{id}, #{tenantId}, #{judgmentType}, #{subjectType}, #{subjectId}, #{judgmentText}, #{judgmentJson}, #{confidence}, #{evidenceMemory}, #{contradiction}, 'PUBLISHED', NOW())")
    void insertJudgment(Map<String, Object> params);

    @Select("SELECT * FROM cognitive_judgment WHERE tenant_id = #{tenantId} AND subject_type = #{subjectType} AND subject_id = #{subjectId} AND status = 'PUBLISHED' ORDER BY created_at DESC LIMIT 1")
    Map<String, Object> findLatestJudgment(@Param("tenantId") Long tenantId, @Param("subjectType") String subjectType, @Param("subjectId") Long subjectId);

    @Select("SELECT * FROM cognitive_judgment WHERE tenant_id = #{tenantId} AND judgment_type = #{judgmentType} AND status = 'PUBLISHED' ORDER BY created_at DESC LIMIT #{limit}")
    List<Map<String, Object>> findJudgmentsByType(@Param("tenantId") Long tenantId, @Param("judgmentType") String judgmentType, @Param("limit") int limit);

    @Update("UPDATE cognitive_judgment SET status = 'SUPERSEDED', superseded_by = #{supersededBy} WHERE id = #{id}")
    void supersedeJudgment(@Param("id") Long id, @Param("supersededBy") Long supersededBy);

    // ================================================================
    // cognitive_user_model
    // ================================================================
    @Insert("INSERT INTO cognitive_user_model (id, tenant_id, user_id, role, decision_speed, risk_tolerance, standard_rigidity, scoring_bias_json, leniency_index, bias_awareness, blind_spots_json, decision_quality_trend, pattern_stability, total_decisions, last_evaluated_at) " +
            "VALUES (#{id}, #{tenantId}, #{userId}, #{role}, #{decisionSpeed}, #{riskTolerance}, #{standardRigidity}, #{scoringBiasJson}, #{leniencyIndex}, #{biasAwareness}, #{blindSpotsJson}, #{decisionQualityTrend}, #{patternStability}, #{totalDecisions}, NOW()) " +
            "ON DUPLICATE KEY UPDATE decision_speed = VALUES(decision_speed), risk_tolerance = VALUES(risk_tolerance), standard_rigidity = VALUES(standard_rigidity), scoring_bias_json = VALUES(scoring_bias_json), leniency_index = VALUES(leniency_index), bias_awareness = VALUES(bias_awareness), blind_spots_json = VALUES(blind_spots_json), decision_quality_trend = VALUES(decision_quality_trend), pattern_stability = VALUES(pattern_stability), total_decisions = VALUES(total_decisions), last_evaluated_at = NOW()")
    void upsertUserModel(Map<String, Object> params);

    @Select("SELECT * FROM cognitive_user_model WHERE tenant_id = #{tenantId} AND user_id = #{userId}")
    Map<String, Object> findUserModel(@Param("tenantId") Long tenantId, @Param("userId") Long userId);

    @Select("SELECT * FROM cognitive_user_model WHERE tenant_id = #{tenantId} AND role = #{role} ORDER BY total_decisions DESC")
    List<Map<String, Object>> findUserModelsByRole(@Param("tenantId") Long tenantId, @Param("role") String role);

    // ================================================================
    // cognitive_observation
    // ================================================================
    @Insert("INSERT INTO cognitive_observation (id, tenant_id, observation_type, severity, title, body, related_objects, suggested_action, action_taken, created_at, expires_at) " +
            "VALUES (#{id}, #{tenantId}, #{observationType}, #{severity}, #{title}, #{body}, #{relatedObjects}, #{suggestedAction}, 'PENDING', NOW(), #{expiresAt})")
    void insertObservation(Map<String, Object> params);

    @Select("SELECT * FROM cognitive_observation WHERE tenant_id = #{tenantId} AND action_taken = 'PENDING' AND (expires_at IS NULL OR expires_at > NOW()) ORDER BY FIELD(severity, 'CRITICAL','WARNING','INFO','CURIOSITY'), created_at DESC")
    List<Map<String, Object>> findPendingObservations(@Param("tenantId") Long tenantId);

    @Select("SELECT COUNT(*) FROM cognitive_observation WHERE tenant_id = #{tenantId} AND action_taken = 'PENDING' AND severity = #{severity}")
    int countPendingBySeverity(@Param("tenantId") Long tenantId, @Param("severity") String severity);

    @Update("UPDATE cognitive_observation SET action_taken = #{actionTaken}, action_taken_by = #{userId}, action_taken_at = NOW() WHERE id = #{id}")
    void updateObservationAction(@Param("id") Long id, @Param("actionTaken") String actionTaken, @Param("userId") Long userId);

    @Update("UPDATE cognitive_observation SET feedback = #{feedback} WHERE id = #{id}")
    void updateObservationFeedback(@Param("id") Long id, @Param("feedback") String feedback);

    // ================================================================
    // ID generation
    // ================================================================
    @Select("SELECT COALESCE(MAX(id), 0) + 1 FROM cognitive_event_memory")
    Long nextEventId();

    @Select("SELECT COALESCE(MAX(id), 0) + 1 FROM cognitive_pattern_memory")
    Long nextPatternId();

    @Select("SELECT COALESCE(MAX(id), 0) + 1 FROM cognitive_object_memory")
    Long nextObjectId();

    @Select("SELECT COALESCE(MAX(id), 0) + 1 FROM cognitive_lesson_memory")
    Long nextLessonId();

    @Select("SELECT COALESCE(MAX(id), 0) + 1 FROM cognitive_judgment")
    Long nextJudgmentId();

    @Select("SELECT COALESCE(MAX(id), 0) + 1 FROM cognitive_user_model")
    Long nextUserModelId();

    @Select("SELECT COALESCE(MAX(id), 0) + 1 FROM cognitive_observation")
    Long nextObservationId();
}
