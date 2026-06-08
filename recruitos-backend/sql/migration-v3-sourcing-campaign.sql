-- ============================================================
-- RecruitOS v3 岗位寻源活动 + Agent 表结构对齐
-- 在 init + v1 + v2 + test-data 之后执行
-- ============================================================
USE `recruit_os`;

-- ---------- agent_account 对齐 Java 实体 ----------
ALTER TABLE `agent_account`
  ADD COLUMN `account_id` VARCHAR(64) DEFAULT NULL COMMENT '平台侧账号ID' AFTER `account_name`,
  ADD COLUMN `daily_limit` INT DEFAULT NULL COMMENT '日操作上限' AFTER `daily_search_limit`,
  ADD COLUMN `used_today` INT DEFAULT 0 COMMENT '今日已用' AFTER `daily_limit`,
  ADD COLUMN `remark` VARCHAR(256) DEFAULT NULL AFTER `last_health_check_at`,
  ADD COLUMN `created_by` BIGINT DEFAULT NULL AFTER `remark`;

UPDATE `agent_account`
SET `daily_limit` = COALESCE(`daily_msg_limit`, 50),
    `used_today` = COALESCE(`daily_msg_count`, 0),
    `account_id` = CONCAT(LOWER(`platform`), '-', `id`)
WHERE `daily_limit` IS NULL;

-- ---------- agent_task 对齐 Java 实体 ----------
ALTER TABLE `agent_task`
  ADD COLUMN `campaign_id` BIGINT DEFAULT NULL COMMENT '寻源活动ID' AFTER `tenant_id`,
  ADD COLUMN `job_title` VARCHAR(128) DEFAULT NULL AFTER `job_id`,
  ADD COLUMN `platform` VARCHAR(16) DEFAULT NULL AFTER `job_title`,
  ADD COLUMN `priority` INT DEFAULT 2 AFTER `status`,
  ADD COLUMN `target_count` INT DEFAULT NULL AFTER `priority`,
  ADD COLUMN `completed_count` INT DEFAULT 0 AFTER `target_count`,
  ADD COLUMN `failed_count` INT DEFAULT 0 AFTER `completed_count`,
  ADD COLUMN `created_by` BIGINT DEFAULT NULL AFTER `completed_at`,
  ADD COLUMN `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP AFTER `created_at`,
  ADD INDEX `idx_campaign` (`campaign_id`);

-- ---------- agent_behavior_log 对齐 Java 实体 ----------
ALTER TABLE `agent_behavior_log`
  ADD COLUMN `agent_task_id` BIGINT DEFAULT NULL AFTER `tenant_id`,
  ADD COLUMN `action_type` VARCHAR(32) DEFAULT NULL AFTER `agent_account_id`,
  ADD COLUMN `target_name` VARCHAR(128) DEFAULT NULL AFTER `action_type`,
  ADD COLUMN `target_platform` VARCHAR(16) DEFAULT NULL AFTER `target_name`,
  ADD COLUMN `action_detail` TEXT DEFAULT NULL AFTER `target_platform`,
  ADD COLUMN `is_success` TINYINT DEFAULT 1 AFTER `action_detail`,
  ADD COLUMN `error_message` VARCHAR(512) DEFAULT NULL AFTER `is_success`,
  ADD COLUMN `random_delay` INT DEFAULT NULL AFTER `error_message`,
  ADD COLUMN `executed_at` DATETIME DEFAULT NULL AFTER `random_delay`;

UPDATE `agent_behavior_log`
SET `action_type` = `action`,
    `is_success` = COALESCE(`success`, 1),
    `error_message` = `error_detail`,
    `executed_at` = `created_at`,
    `action_detail` = `target_url`
WHERE `action_type` IS NULL;

-- ---------- 岗位寻源活动 ----------
CREATE TABLE IF NOT EXISTS `job_sourcing_campaign` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `tenant_id` BIGINT NOT NULL,
    `job_id` BIGINT NOT NULL,
    `name` VARCHAR(128) NOT NULL,
    `mode` VARCHAR(32) NOT NULL DEFAULT 'SEMI_AUTO' COMMENT 'FULL_AUTO/SEMI_AUTO/PUBLISH_SEARCH_ONLY',
    `status` VARCHAR(16) NOT NULL DEFAULT 'DRAFT' COMMENT 'DRAFT/RUNNING/PAUSED/COMPLETED/FAILED',
    `config_json` JSON DEFAULT NULL COMMENT '平台账号、关键词、日上限、模板等',
    `stats_json` JSON DEFAULT NULL COMMENT '漏斗统计',
    `resume_confirm_required` TINYINT DEFAULT 0,
    `publish_confirm_required` TINYINT DEFAULT 0,
    `created_by` BIGINT DEFAULT NULL,
    `started_at` DATETIME DEFAULT NULL,
    `completed_at` DATETIME DEFAULT NULL,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    INDEX `idx_tenant_job` (`tenant_id`, `job_id`),
    INDEX `idx_tenant_status` (`tenant_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='岗位寻源活动';

CREATE TABLE IF NOT EXISTS `campaign_platform_run` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `tenant_id` BIGINT NOT NULL,
    `campaign_id` BIGINT NOT NULL,
    `platform` VARCHAR(16) NOT NULL,
    `primary_account_id` BIGINT NOT NULL,
    `auxiliary_account_ids` JSON DEFAULT NULL,
    `current_step` VARCHAR(32) DEFAULT 'LOGIN' COMMENT 'LOGIN/PUBLISH/SEARCH/GREET/MONITOR/FETCH/DONE',
    `current_account_id` BIGINT DEFAULT NULL,
    `platform_job_url` VARCHAR(512) DEFAULT NULL,
    `status` VARCHAR(16) DEFAULT 'PENDING' COMMENT 'PENDING/RUNNING/PAUSED/COMPLETED/FAILED',
    `stats_json` JSON DEFAULT NULL,
    `error_message` VARCHAR(512) DEFAULT NULL,
    `started_at` DATETIME DEFAULT NULL,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_campaign_platform` (`campaign_id`, `platform`),
    INDEX `idx_tenant_campaign` (`tenant_id`, `campaign_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='活动-平台执行实例';

CREATE TABLE IF NOT EXISTS `candidate_acquire_lock` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `tenant_id` BIGINT NOT NULL,
    `campaign_id` BIGINT NOT NULL,
    `dedup_key` VARCHAR(128) NOT NULL,
    `platform` VARCHAR(16) DEFAULT NULL,
    `platform_user_id` VARCHAR(64) DEFAULT NULL,
    `locked_by_account_id` BIGINT NOT NULL,
    `candidate_id` BIGINT DEFAULT NULL,
    `status` VARCHAR(16) DEFAULT 'LOCKED' COMMENT 'LOCKED/IMPORTED/RELEASED',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_campaign_dedup` (`campaign_id`, `dedup_key`),
    INDEX `idx_tenant_dedup` (`tenant_id`, `dedup_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='候选人获客锁定';

CREATE TABLE IF NOT EXISTS `campaign_candidate_trace` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `tenant_id` BIGINT NOT NULL,
    `campaign_id` BIGINT NOT NULL,
    `platform_run_id` BIGINT DEFAULT NULL,
    `job_id` BIGINT NOT NULL,
    `platform` VARCHAR(16) NOT NULL,
    `account_id` BIGINT DEFAULT NULL,
    `platform_user_id` VARCHAR(64) DEFAULT NULL,
    `candidate_name` VARCHAR(64) DEFAULT NULL,
    `phone` VARCHAR(32) DEFAULT NULL,
    `email` VARCHAR(128) DEFAULT NULL,
    `dedup_key` VARCHAR(128) NOT NULL,
    `trace_status` VARCHAR(32) DEFAULT 'SEARCHED',
    `match_score` DECIMAL(5,2) DEFAULT NULL,
    `candidate_id` BIGINT DEFAULT NULL,
    `resume_id` BIGINT DEFAULT NULL,
    `skip_reason` VARCHAR(64) DEFAULT NULL,
    `locked_by_account_id` BIGINT DEFAULT NULL,
    `timeline_json` JSON DEFAULT NULL,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    INDEX `idx_campaign_status` (`campaign_id`, `trace_status`),
    INDEX `idx_job` (`job_id`),
    INDEX `idx_dedup` (`tenant_id`, `dedup_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='活动候选人轨迹';
