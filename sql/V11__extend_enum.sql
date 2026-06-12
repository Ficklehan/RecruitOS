-- ================================================================
-- RecruitOS V11: 扩展认知层ENUM值
-- 新增 event_type: INTERVIEWER_ASSESSMENT（面试官质量评估事件）
-- 新增 pattern_type: PIPELINE_BOTTLENECK（Pipeline阻塞模式）
-- ================================================================

ALTER TABLE cognitive_event_memory
  MODIFY COLUMN event_type ENUM(
    'HIRE','REJECT','DEPARTURE','OFFER_NEGOTIATION',
    'CANDIDATE_DISENGAGE','PIPELINE_BLOCKAGE',
    'INTERVIEW_DISAGREEMENT','SCREENING_OVERRIDE',
    'INTERVIEWER_ASSESSMENT'   -- 新增：面试官质量评估事件
  ) NOT NULL;

ALTER TABLE cognitive_pattern_memory
  MODIFY COLUMN pattern_type ENUM(
    'CANDIDATE_SOURCE_PERFORMANCE','INTERVIEWER_SCORING_BIAS',
    'SKILL_RETENTION_CORRELATION','OFFER_ACCEPTANCE_FACTOR',
    'DEPARTURE_EARLY_SIGNAL','TEAM_COMPOSITION_RISK',
    'HIRING_MANAGER_PATTERN','PIPELINE_BOTTLENECK'  -- 新增：Pipeline阻塞模式
  ) NOT NULL DEFAULT 'CANDIDATE_SOURCE_PERFORMANCE';
