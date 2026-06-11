-- ============================================================
-- RecruitOS AI-Native Foundation Migration
-- Phase A: Data Loop Foundation
-- Created: 2026-06-11
-- ============================================================

-- 1. Probation Check-in (试用期验证 — L6真实化)
-- ============================================================
CREATE TABLE IF NOT EXISTS probation_checkin (
    id              BIGINT          NOT NULL PRIMARY KEY,
    tenant_id       BIGINT          NOT NULL,
    candidate_id    BIGINT          NOT NULL,
    job_id          BIGINT          NOT NULL,
    checkin_day     INT             NOT NULL COMMENT '30/60/90',
    evaluator_id    BIGINT          COMMENT 'Hiring Manager user ID',
    dimensions      JSON            NOT NULL COMMENT '评估维度 [{dimension, interview_score, actual_score, gap_analysis}]',
    overall_rating  DECIMAL(3,2)    COMMENT '综合评分 1.00-5.00',
    would_hire_again BOOLEAN        COMMENT '是否愿意再次录用',
    strengths       TEXT            COMMENT '优势',
    gaps            TEXT            COMMENT '待提升领域',
    recommendation  VARCHAR(32)     COMMENT 'CONFIRM/EXTEND_PROBATION/TERMINATE',
    ai_analysis     JSON            COMMENT 'AI分析：面试vs实际差距、预测准确度',
    submitted_at    DATETIME,
    l6_signal_emitted BOOLEAN       DEFAULT FALSE,
    created_at      DATETIME        DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME        DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_checkin_tenant_job (tenant_id, job_id),
    INDEX idx_checkin_candidate (candidate_id),
    INDEX idx_checkin_day (checkin_day),
    INDEX idx_checkin_l6 (l6_signal_emitted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='试用期验证 — L6进化信号来源';


-- 2. Business Context (业务上下文 — AI理解「为什么招」)
-- ============================================================
CREATE TABLE IF NOT EXISTS business_context (
    id                  BIGINT          NOT NULL PRIMARY KEY,
    tenant_id           BIGINT          NOT NULL,
    department_id       BIGINT,
    demand_id           BIGINT          COMMENT '关联招聘需求',
    business_objective  TEXT            NOT NULL COMMENT '业务目标（自然语言描述）',
    key_results         JSON            COMMENT '关键结果 [{metric, target, deadline}]',
    urgency             VARCHAR(16)     DEFAULT 'MEDIUM' COMMENT 'CRITICAL/HIGH/MEDIUM/LOW',
    budget_approved     DECIMAL(12,2)   COMMENT '已批准预算',
    headcount_allocated INT             COMMENT '分配编制',
    headcount_consumed  INT             DEFAULT 0,
    ai_diagnosis        JSON            COMMENT 'AI诊断 {gaps:[...], recommendations:[...], confidence:0.85}',
    status              VARCHAR(16)     DEFAULT 'ACTIVE' COMMENT 'ACTIVE/ARCHIVED',
    created_at          DATETIME        DEFAULT CURRENT_TIMESTAMP,
    updated_at          DATETIME        DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_bctx_tenant (tenant_id),
    INDEX idx_bctx_demand (demand_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='业务上下文 — AI理解招聘的业务背景';


-- 3. Team Capability Profile (团队能力图谱)
-- ============================================================
CREATE TABLE IF NOT EXISTS team_capability_profile (
    id                  BIGINT          NOT NULL PRIMARY KEY,
    tenant_id           BIGINT          NOT NULL,
    department_id       BIGINT,
    member_id           BIGINT          COMMENT '关联sys_user',
    skill_dimensions    JSON            NOT NULL COMMENT '[{skill, level:1-5, evidence, last_updated}]',
    performance_rating  DECIMAL(3,2)    COMMENT '最近绩效评分',
    career_stage        VARCHAR(16)     COMMENT 'GROWING/PEAK/PLATEAU',
    retention_risk      VARCHAR(8)      DEFAULT 'LOW' COMMENT 'LOW/MEDIUM/HIGH',
    extracted_from      TEXT            COMMENT '数据来源：面试评价/绩效/项目',
    created_at          DATETIME        DEFAULT CURRENT_TIMESTAMP,
    updated_at          DATETIME        DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_member (tenant_id, member_id),
    INDEX idx_tcp_dept (tenant_id, department_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='团队能力图谱 — AI理解团队能力现状';


-- 4. Interview Scorecard Template (结构化面试评分卡模板)
-- ============================================================
CREATE TABLE IF NOT EXISTS interview_scorecard_template (
    id              BIGINT          NOT NULL PRIMARY KEY,
    tenant_id       BIGINT          NOT NULL,
    job_family      VARCHAR(64)     NOT NULL COMMENT '岗位族：BACKEND/FRONTEND/PM/DESIGN/DATA',
    level_range     VARCHAR(32)     COMMENT 'P5-P6/P7-P8/P9+',
    dimensions      JSON            NOT NULL COMMENT '[{name, weight, behavioral_anchors:{1,3,5}, suggested_questions:[...]}]',
    version         INT             DEFAULT 1,
    is_active       BOOLEAN         DEFAULT TRUE,
    created_by      BIGINT,
    created_at      DATETIME        DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME        DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_sct_family (tenant_id, job_family, is_active)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='结构化面试评分卡模板';


-- 5. Interview Scorecard Result (面试评分卡结果)
-- ============================================================
CREATE TABLE IF NOT EXISTS interview_scorecard_result (
    id                      BIGINT          NOT NULL PRIMARY KEY,
    interview_id            BIGINT          NOT NULL,
    template_id             BIGINT,
    evaluator_id            BIGINT          NOT NULL COMMENT '面试官',
    dimensions              JSON            NOT NULL COMMENT '[{name, score:1-5, evidence, confidence:HIGH/MEDIUM/LOW}]',
    overall_score           DECIMAL(3,2)    COMMENT '加权综合分',
    decision                VARCHAR(20)     COMMENT 'STRONG_HIRE/HIRE/LEANING_HIRE/LEANING_NO/NO_HIRE',
    ai_consistency_check    JSON            COMMENT 'AI一致性检查：评分与对话内容对比',
    submitted_at            DATETIME,
    created_at              DATETIME        DEFAULT CURRENT_TIMESTAMP,
    updated_at              DATETIME        DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_interview_eval (interview_id, evaluator_id),
    INDEX idx_scr_interview (interview_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='面试评分卡结果 — 替代旧evaluation裸JSON';


-- 6. Interviewer Calibration (面试官校准)
-- ============================================================
CREATE TABLE IF NOT EXISTS interviewer_calibration (
    id                  BIGINT          NOT NULL PRIMARY KEY,
    tenant_id           BIGINT          NOT NULL,
    interviewer_id      BIGINT          NOT NULL,
    job_family          VARCHAR(64)     COMMENT '岗位族',
    total_evaluations   INT             DEFAULT 0,
    avg_score           DECIMAL(3,2)    COMMENT '平均评分',
    score_stddev        DECIMAL(3,2)    COMMENT '评分标准差',
    leniency_index      DECIMAL(3,2)    COMMENT '宽松指数：>1偏松 <1偏严 =1正常',
    consistency_score   DECIMAL(3,2)    COMMENT '与最终录用结果的一致性',
    calibration_level   VARCHAR(16)     DEFAULT 'L1' COMMENT 'L1=可独立面试 L2=可培训他人',
    last_calibrated_at  DATETIME,
    created_at          DATETIME        DEFAULT CURRENT_TIMESTAMP,
    updated_at          DATETIME        DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_calib (tenant_id, interviewer_id, job_family),
    INDEX idx_calib_tenant (tenant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='面试官校准 — 偏差追踪与认证';


-- 7. AI Diagnosis (AI诊断结果)
-- ============================================================
CREATE TABLE IF NOT EXISTS ai_diagnosis (
    id                  BIGINT          NOT NULL PRIMARY KEY,
    tenant_id           BIGINT          NOT NULL,
    diagnosis_type      VARCHAR(32)     NOT NULL COMMENT 'JOB_HEALTH/FUNNEL_BOTTLENECK/INTERVIEWER_BIAS/CHANNEL_ROI/CANDIDATE_EXPERIENCE',
    target_id           BIGINT          COMMENT '关联job_id/campaign_id/interviewer_id',
    target_type         VARCHAR(32)     COMMENT 'JOB/CAMPAIGN/INTERVIEWER/CHANNEL',
    severity            VARCHAR(16)     NOT NULL DEFAULT 'INFO' COMMENT 'CRITICAL/WARNING/INFO',
    title               VARCHAR(256)    NOT NULL,
    evidence            JSON            COMMENT '诊断依据的数据摘要',
    root_cause          TEXT            COMMENT 'AI推理的根因',
    recommendation      TEXT            COMMENT 'AI建议的行动',
    expected_impact     TEXT            COMMENT '采取行动后的预期效果',
    confidence          DECIMAL(3,2)    DEFAULT 0.50,
    status              VARCHAR(16)     DEFAULT 'PENDING' COMMENT 'PENDING/ACKNOWLEDGED/DISMISSED/ACTIONED',
    acknowledged_by     BIGINT,
    actioned_at         DATETIME,
    created_at          DATETIME        DEFAULT CURRENT_TIMESTAMP,
    updated_at          DATETIME        DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_diag_tenant_type (tenant_id, diagnosis_type),
    INDEX idx_diag_target (target_type, target_id),
    INDEX idx_diag_severity (severity),
    INDEX idx_diag_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI诊断 — 主动巡检发现的问题与建议';


-- 8. AI Decision Log (AI决策审计)
-- ============================================================
CREATE TABLE IF NOT EXISTS ai_decision_log (
    id                  BIGINT          NOT NULL PRIMARY KEY,
    tenant_id           BIGINT          NOT NULL,
    decision_type       VARCHAR(32)     NOT NULL COMMENT 'KEYWORD_OPTIMIZE/SCREENING_ADJUST/GREETING_OPTIMIZE/CHANNEL_SWITCH/CANDIDATE_RANK/OFFER_RECOMMEND',
    target_id           BIGINT          COMMENT '关联对象ID',
    target_type         VARCHAR(32)     COMMENT 'JOB/CAMPAIGN/CANDIDATE',
    decision_detail     JSON            NOT NULL COMMENT '决策内容与完整依据',
    confidence          DECIMAL(3,2)    DEFAULT 0.50,
    auto_executed       BOOLEAN         DEFAULT FALSE COMMENT '是否自动执行（低风险操作）',
    human_confirmed     BOOLEAN         DEFAULT FALSE COMMENT 'HR是否确认（高风险操作）',
    confirmed_by        BIGINT,
    outcome             JSON            COMMENT '决策后的实际结果（回溯填充）',
    outcome_recorded_at DATETIME,
    created_at          DATETIME        DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_adl_tenant (tenant_id),
    INDEX idx_adl_type (decision_type),
    INDEX idx_adl_auto (auto_executed),
    INDEX idx_adl_target (target_type, target_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI决策审计 — 可回溯可复盘';


-- 9. Add weight_stability_evidence column to existing health checks
-- (already added in HealthVO.java; this is for any future DB-level health snapshot)
-- No DDL needed — HealthVO is a runtime VO, not persisted here.

-- 10. Add headcount_consumed to recruit_demand (if it exists)
-- Check if column exists before adding (safe)
SET @col_exists = 0;
SELECT COUNT(*) INTO @col_exists FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'recruit_demand' AND COLUMN_NAME = 'headcount_consumed';
SET @sql = IF(@col_exists = 0,
    'ALTER TABLE recruit_demand ADD COLUMN headcount_consumed INT DEFAULT 0 COMMENT ''已消耗编制数'' AFTER headcount',
    'SELECT ''headcount_consumed already exists''');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
