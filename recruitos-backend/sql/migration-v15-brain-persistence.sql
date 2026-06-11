-- ============================================================
-- RecruitOS v15: Brain Engine Persistence Tables
-- 补充 v14 未覆盖的 AI 引擎持久化表
-- ============================================================

-- 1. brain_demand_diagnosis（需求诊断历史）
CREATE TABLE IF NOT EXISTS brain_demand_diagnosis (
    id                  BIGINT          NOT NULL PRIMARY KEY,
    tenant_id           BIGINT          NOT NULL,
    demand_id           BIGINT,
    business_objective  TEXT            NOT NULL,
    diagnosis_json      JSON            NOT NULL COMMENT '完整诊断结果JSON',
    confidence          DECIMAL(3,2)    DEFAULT 0.70,
    llm_model           VARCHAR(50),
    llm_prompt_hash     VARCHAR(64),
    status              VARCHAR(20)     DEFAULT 'GENERATED' COMMENT 'GENERATED/ACCEPTED/REJECTED/EXPIRED',
    accepted_by         BIGINT,
    created_at          DATETIME        DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_bdd_tenant (tenant_id),
    INDEX idx_bdd_demand (demand_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='需求诊断历史 — AI诊断的完整记录';

-- 2. brain_candidate_intent（候选人意向评分历史）
CREATE TABLE IF NOT EXISTS brain_candidate_intent (
    id                  BIGINT          NOT NULL PRIMARY KEY,
    tenant_id           BIGINT          NOT NULL,
    candidate_id        BIGINT          NOT NULL,
    job_id              BIGINT          NOT NULL,
    intent_score        DECIMAL(5,2)    NOT NULL COMMENT '0-100',
    intent_level        VARCHAR(10)     NOT NULL COMMENT 'HIGH/MEDIUM/LOW',
    feature_vector_json JSON            COMMENT '特征值快照（用于模型训练）',
    model_version       VARCHAR(30)     COMMENT '模型版本号',
    confidence          DECIMAL(3,2)    DEFAULT 0.50,
    risk_factors_json   JSON            COMMENT '风险因子列表',
    interventions_json  JSON            COMMENT '干预建议列表',
    created_at          DATETIME        DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_bci_tenant_candidate (tenant_id, candidate_id),
    INDEX idx_bci_job (job_id),
    INDEX idx_bci_created (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='候选人意向评分 — 用于ML模型训练';

-- 3. brain_calibration_session（校准会记录）
CREATE TABLE IF NOT EXISTS brain_calibration_session (
    id                  BIGINT          NOT NULL PRIMARY KEY,
    tenant_id           BIGINT          NOT NULL,
    candidate_id        BIGINT          NOT NULL,
    job_id              BIGINT,
    evaluators_json     JSON            NOT NULL COMMENT '参与面试官及原始评分',
    dimension_stats_json JSON           COMMENT '各维度统计（均值/方差/最大分歧/Kappa）',
    cohen_kappa_json    JSON            COMMENT '各维度Cohen Kappa值',
    moderator_script    TEXT            COMMENT 'AI主持脚本',
    final_decision      VARCHAR(30)     COMMENT 'STRONG_HIRE/HIRE/LEANING_HIRE/LEANING_NO/NO_HIRE',
    consensus_score     DECIMAL(3,2),
    bias_detections_json JSON           COMMENT '偏差检测结果',
    silent_dimensions_json JSON         COMMENT '未被覆盖的维度',
    created_at          DATETIME        DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_bcs_tenant (tenant_id),
    INDEX idx_bcs_candidate (candidate_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='校准会记录 — 可回溯的校准决策';

-- 4. brain_cycle_prediction（周期预测快照）
CREATE TABLE IF NOT EXISTS brain_cycle_prediction (
    id                  BIGINT          NOT NULL PRIMARY KEY,
    tenant_id           BIGINT          NOT NULL,
    job_id              BIGINT          NOT NULL,
    estimated_days      INT             NOT NULL,
    min_days            INT,
    max_days            INT,
    risk_level          VARCHAR(10)     DEFAULT 'MEDIUM' COMMENT 'LOW/MEDIUM/HIGH',
    bottleneck_json     JSON            COMMENT '瓶颈详情',
    intervention_json   JSON            COMMENT '干预建议',
    pipeline_snapshot_json JSON         COMMENT '当时的管道快照',
    confidence          DECIMAL(3,2)    DEFAULT 0.60,
    created_at          DATETIME        DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_bcp_tenant_job (tenant_id, job_id),
    INDEX idx_bcp_created (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='周期预测 — 预测 vs 实际的对照基础';

-- 5. brain_talent_density_snapshot（人才密度快照）
CREATE TABLE IF NOT EXISTS brain_talent_density_snapshot (
    id                  BIGINT          NOT NULL PRIMARY KEY,
    tenant_id           BIGINT          NOT NULL,
    org_id              BIGINT          NOT NULL,
    density_score       DECIMAL(4,2)    NOT NULL,
    density_level       VARCHAR(10)     NOT NULL COMMENT 'HIGH/MEDIUM/LOW',
    heatmap_json        JSON            NOT NULL COMMENT '能力热力图',
    bar_raiser_verdict  TEXT            COMMENT 'Bar Raiser视角判断',
    snapshot_period     VARCHAR(20)     COMMENT '快照周期 2026-Q3',
    confidence          DECIMAL(3,2)    DEFAULT 0.65,
    created_at          DATETIME        DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_btds_tenant_org (tenant_id, org_id),
    INDEX idx_btds_period (snapshot_period)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='人才密度快照 — 定期评估团队能力水位';

-- 6. brain_llm_trace（LLM调用追踪）
CREATE TABLE IF NOT EXISTS brain_llm_trace (
    id                  BIGINT          NOT NULL PRIMARY KEY,
    tenant_id           BIGINT          NOT NULL,
    scenario            VARCHAR(50)     NOT NULL COMMENT 'demand_diagnosis/intent_prediction等',
    prompt_hash         VARCHAR(64)     NOT NULL,
    prompt_preview      VARCHAR(500)    COMMENT '输入prompt前500字符',
    raw_output          MEDIUMTEXT      COMMENT 'LLM原始输出',
    parsed_success      BOOLEAN         DEFAULT FALSE,
    parse_error         VARCHAR(500),
    latency_ms          INT,
    model               VARCHAR(50),
    tokens_used         INT,
    created_at          DATETIME        DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_blt_tenant_scenario (tenant_id, scenario),
    INDEX idx_blt_hash (prompt_hash),
    INDEX idx_blt_success (parsed_success)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='LLM调用追踪 — 用于prompt优化和成本分析';
