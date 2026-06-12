-- ================================================================
-- RecruitOS v8: AI 认知层 (Cognitive Layer)
-- 维度 E — Memory + Judgment + Self-Model
-- ================================================================

CREATE TABLE cognitive_event_memory (
  id              BIGINT PRIMARY KEY AUTO_INCREMENT,
  tenant_id       BIGINT NOT NULL,
  event_type      ENUM('HIRE','REJECT','DEPARTURE','OFFER_NEGOTIATION',
                       'CANDIDATE_DISENGAGE','PIPELINE_BLOCKAGE',
                       'INTERVIEW_DISAGREEMENT','SCREENING_OVERRIDE',
                       'INTERVIEWER_ASSESSMENT') NOT NULL,
  event_subject   VARCHAR(64) NOT NULL,
  subject_id      BIGINT NOT NULL,
  context_json    JSON NOT NULL,
  outcome         VARCHAR(64) NOT NULL,
  outcome_reason  TEXT,
  decision_quality ENUM('GOOD','NEUTRAL','POOR','UNKNOWN') DEFAULT 'UNKNOWN',
  occurred_at     DATETIME NOT NULL,
  recorded_at     DATETIME DEFAULT NOW(),
  INDEX idx_tenant_subject (tenant_id, event_subject, subject_id),
  INDEX idx_tenant_type_time (tenant_id, event_type, occurred_at)
) COMMENT '认知层：事件记忆';

CREATE TABLE cognitive_pattern_memory (
  id              BIGINT PRIMARY KEY AUTO_INCREMENT,
  tenant_id       BIGINT NOT NULL,
  pattern_type    ENUM('CANDIDATE_SOURCE_PERFORMANCE','INTERVIEWER_SCORING_BIAS',
                       'SKILL_RETENTION_CORRELATION','OFFER_ACCEPTANCE_FACTOR',
                       'DEPARTURE_EARLY_SIGNAL','TEAM_COMPOSITION_RISK',
                       'HIRING_MANAGER_PATTERN',
                       'PIPELINE_BOTTLENECK') NOT NULL,
  pattern_name    VARCHAR(128) NOT NULL,
  pattern_rule    JSON NOT NULL,
  evidence_events JSON,
  confidence      DECIMAL(4,3) NOT NULL DEFAULT 0.000,
  sample_size     INT NOT NULL DEFAULT 0,
  status          ENUM('ACTIVE','STALE','INVALIDATED') DEFAULT 'ACTIVE',
  discovered_at   DATETIME DEFAULT NOW(),
  last_validated_at DATETIME,
  INDEX idx_tenant_type (tenant_id, pattern_type),
  INDEX idx_tenant_confidence (tenant_id, confidence)
) COMMENT '认知层：模式记忆';

CREATE TABLE cognitive_object_memory (
  id              BIGINT PRIMARY KEY AUTO_INCREMENT,
  tenant_id       BIGINT NOT NULL,
  object_type     ENUM('CANDIDATE','JOB','INTERVIEWER','HIRING_MANAGER','TEAM') NOT NULL,
  object_id       BIGINT NOT NULL,
  summary_tldr    VARCHAR(256),
  evolving_profile JSON,
  key_signals     JSON,
  risk_flags      JSON,
  last_updated    DATETIME DEFAULT NOW(),
  UNIQUE KEY uk_tenant_object (tenant_id, object_type, object_id),
  INDEX idx_tenant_type (tenant_id, object_type)
) COMMENT '认知层：对象记忆';

CREATE TABLE cognitive_lesson_memory (
  id              BIGINT PRIMARY KEY AUTO_INCREMENT,
  tenant_id       BIGINT NOT NULL,
  lesson_type     ENUM('BAD_HIRE_PATTERN','MISSED_GOOD_CANDIDATE',
                       'INTERVIEW_BLIND_SPOT','PROCESS_FAILURE') NOT NULL,
  title           VARCHAR(256) NOT NULL,
  description     TEXT NOT NULL,
  evidence        JSON NOT NULL,
  corrective_action TEXT,
  severity        ENUM('CRITICAL','IMPORTANT','NOTABLE') DEFAULT 'IMPORTANT',
  status          ENUM('ACTIVE','ADDRESSED','STALE') DEFAULT 'ACTIVE',
  learned_at      DATETIME DEFAULT NOW(),
  addressed_at    DATETIME,
  INDEX idx_tenant_type (tenant_id, lesson_type)
) COMMENT '认知层：教训记忆';

CREATE TABLE cognitive_judgment (
  id              BIGINT PRIMARY KEY AUTO_INCREMENT,
  tenant_id       BIGINT NOT NULL,
  judgment_type   ENUM('CANDIDATE_OPINION','PIPELINE_HEALTH','HIRING_RISK',
                       'INTERVIEW_QUALITY','DECISION_CONSISTENCY',
                       'TEAM_COMPOSITION','PROCESS_BOTTLENECK') NOT NULL,
  subject_type    VARCHAR(64) NOT NULL,
  subject_id      BIGINT NOT NULL,
  judgment_text   TEXT NOT NULL,
  judgment_json   JSON NOT NULL,
  confidence      DECIMAL(4,3) NOT NULL,
  evidence_memory JSON,
  contradiction   JSON,
  status          ENUM('DRAFT','PUBLISHED','SUPERSEDED','WITHDRAWN') DEFAULT 'PUBLISHED',
  created_at      DATETIME DEFAULT NOW(),
  superseded_by   BIGINT,
  INDEX idx_tenant_subject (tenant_id, judgment_type, subject_type, subject_id),
  INDEX idx_tenant_created (tenant_id, created_at)
) COMMENT '认知层：AI判断';

CREATE TABLE cognitive_user_model (
  id              BIGINT PRIMARY KEY AUTO_INCREMENT,
  tenant_id       BIGINT NOT NULL,
  user_id         BIGINT NOT NULL,
  role            ENUM('HIRING_MANAGER','INTERVIEWER','RECRUITER','HRBP','BAR_RAISER') NOT NULL,
  decision_speed  DECIMAL(3,2),
  risk_tolerance  DECIMAL(3,2),
  standard_rigidity DECIMAL(3,2),
  scoring_bias_json JSON,
  leniency_index  DECIMAL(4,3),
  bias_awareness  JSON,
  blind_spots_json JSON,
  decision_quality_trend JSON,
  pattern_stability DECIMAL(3,2),
  total_decisions INT DEFAULT 0,
  last_evaluated_at DATETIME,
  UNIQUE KEY uk_tenant_user (tenant_id, user_id),
  INDEX idx_tenant_role (tenant_id, role)
) COMMENT '认知层：用户画像';

CREATE TABLE cognitive_observation (
  id              BIGINT PRIMARY KEY AUTO_INCREMENT,
  tenant_id       BIGINT NOT NULL,
  observation_type ENUM('ALERT','INSIGHT','SUGGESTION','QUESTION') NOT NULL,
  severity        ENUM('CRITICAL','WARNING','INFO','CURIOSITY') NOT NULL,
  title           VARCHAR(256) NOT NULL,
  body            TEXT NOT NULL,
  related_objects JSON,
  suggested_action JSON,
  action_taken    ENUM('PENDING','EXECUTED','DISMISSED','DEFERRED') DEFAULT 'PENDING',
  action_taken_by BIGINT,
  action_taken_at DATETIME,
  feedback        ENUM('HELPFUL','NOT_HELPFUL','PARTIALLY','NONE') DEFAULT 'NONE',
  created_at      DATETIME DEFAULT NOW(),
  expires_at      DATETIME,
  INDEX idx_tenant_severity (tenant_id, severity, created_at),
  INDEX idx_tenant_status (tenant_id, action_taken)
) COMMENT '认知层：AI主动观察';
