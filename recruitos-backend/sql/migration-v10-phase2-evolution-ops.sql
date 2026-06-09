-- ============================================================
-- RecruitOS v10 Phase2: 渠道运营包 + 进化建议 + 信号扩展
-- 在 init + v1..v9 之后执行
-- ============================================================
USE `recruit_os`;

-- ---------- evolution_signal 扩展（对齐 PRD emit 元数据）----------
ALTER TABLE `evolution_signal`
  ADD COLUMN `source_module` VARCHAR(32) DEFAULT NULL COMMENT '信号来源模块' AFTER `candidate_id`,
  ADD COLUMN `source_event` VARCHAR(64) DEFAULT NULL COMMENT '业务事件名' AFTER `source_module`,
  ADD COLUMN `campaign_id` BIGINT DEFAULT NULL COMMENT '关联寻源活动' AFTER `source_event`,
  ADD COLUMN `trace_id` BIGINT DEFAULT NULL COMMENT '关联候选人轨迹' AFTER `campaign_id`;

-- ---------- 进化建议待确认队列 ----------
CREATE TABLE IF NOT EXISTS `evolution_proposal` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `tenant_id` BIGINT NOT NULL,
    `job_id` BIGINT NOT NULL,
    `proposal_type` VARCHAR(32) NOT NULL COMMENT 'OPS_PACK/WEIGHT_ONLY/SCRIPT_ONLY/ROLLBACK',
    `status` VARCHAR(16) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/APPLIED/REJECTED/EXPIRED',
    `title` VARCHAR(256) DEFAULT NULL COMMENT '人可读摘要',
    `diff_json` JSON DEFAULT NULL COMMENT '变更前后 diff',
    `evidence_json` JSON DEFAULT NULL COMMENT '信号统计与依据',
    `ab_result_json` JSON DEFAULT NULL COMMENT 'A/B 结果标注',
    `proposed_ops_pack_json` JSON DEFAULT NULL COMMENT '建议运营包全文',
    `base_ops_pack_version` INT DEFAULT NULL COMMENT '基于的版本号',
    `reviewed_by` BIGINT DEFAULT NULL,
    `reviewed_at` DATETIME DEFAULT NULL,
    `reject_reason` VARCHAR(512) DEFAULT NULL,
    `expires_at` DATETIME DEFAULT NULL,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    INDEX `idx_tenant_job_status` (`tenant_id`, `job_id`, `status`),
    INDEX `idx_expires` (`tenant_id`, `status`, `expires_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='策略进化建议(须HR确认)';

-- ---------- 岗位渠道运营包 ----------
CREATE TABLE IF NOT EXISTS `job_sourcing_ops_pack` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `tenant_id` BIGINT NOT NULL,
    `job_id` BIGINT NOT NULL,
    `version` INT NOT NULL DEFAULT 1,
    `status` VARCHAR(16) NOT NULL DEFAULT 'DRAFT' COMMENT 'DRAFT/ACTIVE/ARCHIVED',
    `pack_json` JSON NOT NULL COMMENT 'SourcingOpsPack 全文',
    `proposal_id` BIGINT DEFAULT NULL COMMENT '来自哪条进化建议',
    `confirmed_by` BIGINT DEFAULT NULL,
    `confirmed_at` DATETIME DEFAULT NULL,
    `created_by` BIGINT DEFAULT NULL,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_job_version` (`tenant_id`, `job_id`, `version`),
    INDEX `idx_job_status` (`tenant_id`, `job_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='岗位渠道运营包版本';

-- ---------- 寻源活动绑定运营包 ----------
ALTER TABLE `job_sourcing_campaign`
  ADD COLUMN `ops_pack_id` BIGINT DEFAULT NULL COMMENT '绑定的运营包ID' AFTER `job_id`,
  ADD COLUMN `ops_pack_version` INT DEFAULT NULL COMMENT '绑定的运营包版本(钉死)' AFTER `ops_pack_id`;

-- ---------- 候选人轨迹：两阶段筛选 ----------
ALTER TABLE `campaign_candidate_trace`
  ADD COLUMN `screen_stage` VARCHAR(32) DEFAULT NULL COMMENT 'CARD/FULL_RESUME/PASSED' AFTER `trace_status`,
  ADD COLUMN `skip_reason_json` JSON DEFAULT NULL COMMENT '结构化淘汰原因' AFTER `skip_reason`,
  ADD COLUMN `greet_strategy_applied` VARCHAR(32) DEFAULT NULL AFTER `skip_reason_json`,
  ADD COLUMN `ops_pack_version` INT DEFAULT NULL COMMENT '执行时运营包版本' AFTER `greet_strategy_applied`;

-- ---------- 渠道暂存库 ----------
CREATE TABLE IF NOT EXISTS `channel_staging_candidate` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `tenant_id` BIGINT NOT NULL,
    `job_id` BIGINT NOT NULL,
    `campaign_id` BIGINT DEFAULT NULL,
    `trace_id` BIGINT DEFAULT NULL,
    `platform` VARCHAR(16) NOT NULL,
    `platform_user_id` VARCHAR(64) DEFAULT NULL,
    `candidate_name` VARCHAR(64) DEFAULT NULL,
    `match_score` DECIMAL(5,2) DEFAULT NULL,
    `status` VARCHAR(16) NOT NULL DEFAULT 'STAGED' COMMENT 'STAGED/GREETED/IMPORTED/REJECTED',
    `screenshot_url` VARCHAR(512) DEFAULT NULL,
    `extracted_fields_json` JSON DEFAULT NULL COMMENT '自定义字段提取结果',
    `resume_text` TEXT DEFAULT NULL,
    `candidate_id` BIGINT DEFAULT NULL,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    INDEX `idx_job_status` (`tenant_id`, `job_id`, `status`),
    INDEX `idx_campaign` (`campaign_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='渠道暂存候选人';

-- ---------- 沟通 Profile（租户/岗位六模块）----------
CREATE TABLE IF NOT EXISTS `communication_profile` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `tenant_id` BIGINT NOT NULL,
    `job_id` BIGINT DEFAULT NULL COMMENT 'NULL=租户默认',
    `persona` TEXT DEFAULT NULL,
    `company_background` TEXT DEFAULT NULL,
    `communication_logic` TEXT DEFAULT NULL,
    `proactive_triggers_json` JSON DEFAULT NULL,
    `guardrails` TEXT DEFAULT NULL,
    `created_by` BIGINT DEFAULT NULL,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_tenant_job` (`tenant_id`, `job_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='沟通配置Profile';

-- ---------- 平台职位映射 ----------
CREATE TABLE IF NOT EXISTS `job_platform_mapping` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `tenant_id` BIGINT NOT NULL,
    `job_id` BIGINT NOT NULL,
    `platform` VARCHAR(16) NOT NULL,
    `platform_job_id` VARCHAR(64) DEFAULT NULL,
    `platform_job_name` VARCHAR(256) NOT NULL,
    `platform_job_url` VARCHAR(512) DEFAULT NULL,
    `verified` TINYINT DEFAULT 0,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_job_platform` (`tenant_id`, `job_id`, `platform`),
    INDEX `idx_platform_name` (`tenant_id`, `platform`, `platform_job_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='内部岗位与平台职位映射';
