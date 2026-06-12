package com.recruitos.brain.service;

import com.recruitos.brain.mapper.CognitiveMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.*;

/**
 * 认知层：记忆服务
 * 管理事件写入、对象画像更新、模式挖掘触发、AI判断发布、观察推送
 */
@Service
public class CognitiveMemoryService {

    @Resource
    private CognitiveMapper cognitiveMapper;

    private static final ObjectMapper om = new ObjectMapper();

    // ================================================================
    // 事件记录
    // ================================================================

    @Transactional
    public Long recordEvent(Long tenantId, String eventType, String eventSubject, Long subjectId,
                            Map<String, Object> context, String outcome, String outcomeReason) {
        Long id = cognitiveMapper.nextEventId();
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("id", id);
        params.put("tenantId", tenantId);
        params.put("eventType", eventType);
        params.put("eventSubject", eventSubject);
        params.put("subjectId", subjectId);
        params.put("contextJson", toJson(context));
        params.put("outcome", outcome);
        params.put("outcomeReason", outcomeReason);
        params.put("decisionQuality", "UNKNOWN");
        params.put("occurredAt", LocalDateTime.now().toString());
        cognitiveMapper.insertEvent(params);
        return id;
    }

    @Transactional
    public void markDecisionQuality(Long eventId, String quality, String reason) {
        cognitiveMapper.updateEventQuality(eventId, quality, reason);
    }

    public List<Map<String, Object>> getObjectEvents(Long tenantId, String eventSubject, Long subjectId) {
        return cognitiveMapper.findEventsBySubject(tenantId, eventSubject, subjectId);
    }

    // ================================================================
    // 对象记忆
    // ================================================================

    @Transactional
    public void updateObjectProfile(Long tenantId, String objectType, Long objectId,
                                     String summary, Map<String, Object> profile,
                                     List<Map<String, Object>> signals, List<String> risks) {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("id", cognitiveMapper.nextObjectId());
        params.put("tenantId", tenantId);
        params.put("objectType", objectType);
        params.put("objectId", objectId);
        params.put("summaryTldr", summary);
        params.put("evolvingProfile", toJson(profile));
        params.put("keySignals", toJson(signals));
        params.put("riskFlags", toJson(risks));
        cognitiveMapper.upsertObject(params);
    }

    public Map<String, Object> getObjectProfile(Long tenantId, String objectType, Long objectId) {
        return cognitiveMapper.findObject(tenantId, objectType, objectId);
    }

    // ================================================================
    // 模式记忆
    // ================================================================

    @Transactional
    public Long discoverPattern(Long tenantId, String patternType, String patternName,
                                 Map<String, Object> rule, List<Long> evidenceEventIds,
                                 double confidence, int sampleSize) {
        Long id = cognitiveMapper.nextPatternId();
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("id", id);
        params.put("tenantId", tenantId);
        params.put("patternType", patternType);
        params.put("patternName", patternName);
        params.put("patternRule", toJson(rule));
        params.put("evidenceEvents", toJson(evidenceEventIds));
        params.put("confidence", confidence);
        params.put("sampleSize", sampleSize);
        cognitiveMapper.insertPattern(params);
        return id;
    }

    public List<Map<String, Object>> getPatternsByType(Long tenantId, String patternType) {
        return cognitiveMapper.findPatternsByType(tenantId, patternType);
    }

    @Transactional
    public void invalidatePattern(Long patternId) {
        cognitiveMapper.updatePatternStatus(patternId, "INVALIDATED");
    }

    // ================================================================
    // 教训记忆
    // ================================================================

    @Transactional
    public Long recordLesson(Long tenantId, String lessonType, String title, String description,
                              Map<String, Object> evidence, String correctiveAction, String severity) {
        Long id = cognitiveMapper.nextLessonId();
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("id", id);
        params.put("tenantId", tenantId);
        params.put("lessonType", lessonType);
        params.put("title", title);
        params.put("description", description);
        params.put("evidence", toJson(evidence));
        params.put("correctiveAction", correctiveAction);
        params.put("severity", severity);
        cognitiveMapper.insertLesson(params);
        return id;
    }

    public List<Map<String, Object>> getActiveLessons(Long tenantId) {
        return cognitiveMapper.findActiveLessons(tenantId);
    }

    @Transactional
    public void addressLesson(Long lessonId) {
        cognitiveMapper.updateLessonStatus(lessonId, "ADDRESSED");
    }

    // ================================================================
    // 判断
    // ================================================================

    @Transactional
    public Long publishJudgment(Long tenantId, String judgmentType, String subjectType, Long subjectId,
                                 String judgmentText, Map<String, Object> judgmentData,
                                 double confidence, List<Long> evidenceIds, Map<String, Object> contradiction) {
        // 取代旧判断
        Map<String, Object> existing = cognitiveMapper.findLatestJudgment(tenantId, subjectType, subjectId);
        Long newId = cognitiveMapper.nextJudgmentId();
        if (existing != null) {
            cognitiveMapper.supersedeJudgment((Long) existing.get("id"), newId);
        }
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("id", newId);
        params.put("tenantId", tenantId);
        params.put("judgmentType", judgmentType);
        params.put("subjectType", subjectType);
        params.put("subjectId", subjectId);
        params.put("judgmentText", judgmentText);
        params.put("judgmentJson", toJson(judgmentData));
        params.put("confidence", confidence);
        params.put("evidenceMemory", toJson(evidenceIds));
        params.put("contradiction", toJson(contradiction));
        cognitiveMapper.insertJudgment(params);
        return newId;
    }

    public Map<String, Object> getLatestJudgment(Long tenantId, String subjectType, Long subjectId) {
        return cognitiveMapper.findLatestJudgment(tenantId, subjectType, subjectId);
    }

    // ================================================================
    // 用户画像
    // ================================================================

    @Transactional
    public void updateUserModel(Long tenantId, Long userId, String role, Map<String, Object> modelData) {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("id", cognitiveMapper.nextUserModelId());
        params.put("tenantId", tenantId);
        params.put("userId", userId);
        params.put("role", role);
        params.put("decisionSpeed", modelData.getOrDefault("decisionSpeed", 0.0));
        params.put("riskTolerance", modelData.getOrDefault("riskTolerance", 0.0));
        params.put("standardRigidity", modelData.getOrDefault("standardRigidity", 0.0));
        params.put("scoringBiasJson", toJson(modelData.get("scoringBias")));
        params.put("leniencyIndex", modelData.getOrDefault("leniencyIndex", 1.0));
        params.put("biasAwareness", toJson(modelData.get("biasAwareness")));
        params.put("blindSpotsJson", toJson(modelData.get("blindSpots")));
        params.put("decisionQualityTrend", toJson(modelData.get("decisionQualityTrend")));
        params.put("patternStability", modelData.getOrDefault("patternStability", 0.5));
        params.put("totalDecisions", modelData.getOrDefault("totalDecisions", 0));
        cognitiveMapper.upsertUserModel(params);
    }

    public Map<String, Object> getUserModel(Long tenantId, Long userId) {
        return cognitiveMapper.findUserModel(tenantId, userId);
    }

    // ================================================================
    // 主动观察
    // ================================================================

    @Transactional
    public Long createObservation(Long tenantId, String type, String severity,
                                   String title, String body, List<Map<String, Object>> relatedObjects,
                                   Map<String, Object> suggestedAction, LocalDateTime expiresAt) {
        Long id = cognitiveMapper.nextObservationId();
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("id", id);
        params.put("tenantId", tenantId);
        params.put("observationType", type);
        params.put("severity", severity);
        params.put("title", title);
        params.put("body", body);
        params.put("relatedObjects", toJson(relatedObjects));
        params.put("suggestedAction", toJson(suggestedAction));
        params.put("expiresAt", expiresAt != null ? expiresAt.toString() : null);
        cognitiveMapper.insertObservation(params);
        return id;
    }

    public List<Map<String, Object>> getPendingObservations(Long tenantId) {
        return cognitiveMapper.findPendingObservations(tenantId);
    }

    public Map<String, Integer> getObservationCounts(Long tenantId) {
        Map<String, Integer> counts = new LinkedHashMap<>();
        counts.put("critical", cognitiveMapper.countPendingBySeverity(tenantId, "CRITICAL"));
        counts.put("warning", cognitiveMapper.countPendingBySeverity(tenantId, "WARNING"));
        counts.put("info", cognitiveMapper.countPendingBySeverity(tenantId, "INFO"));
        counts.put("total", counts.get("critical") + counts.get("warning") + counts.get("info"));
        return counts;
    }

    @Transactional
    public void actionObservation(Long observationId, String action, Long userId) {
        cognitiveMapper.updateObservationAction(observationId, action, userId);
    }

    @Transactional
    public void feedbackObservation(Long observationId, String feedback) {
        cognitiveMapper.updateObservationFeedback(observationId, feedback);
    }

    // ================================================================
    // 工具
    // ================================================================

    private String toJson(Object obj) {
        if (obj == null) return null;
        if (obj instanceof String) return (String) obj;
        try { return om.writeValueAsString(obj); }
        catch (Exception e) { return String.valueOf(obj); }
    }
}
