-- ============================================================
-- RecruitOS v11 safety_log 字段与 Java 实体对齐
-- ============================================================
USE `recruit_os`;

ALTER TABLE `safety_log`
  ADD COLUMN `conversation_id` BIGINT DEFAULT NULL COMMENT '会话ID' AFTER `tenant_id`,
  ADD COLUMN `check_result` VARCHAR(16) DEFAULT 'PASS' COMMENT 'PASS/BLOCK/WARN' AFTER `check_type`,
  ADD COLUMN `matched_content` TEXT DEFAULT NULL COMMENT '命中内容' AFTER `check_result`,
  ADD COLUMN `checked_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '审查时间' AFTER `action`;

UPDATE `safety_log`
SET `check_result` = CASE
    WHEN `action` IN ('PASS', 'ALLOW') THEN 'PASS'
    WHEN `action` IN ('BLOCK', 'BLOCKED') THEN 'BLOCK'
    ELSE 'WARN'
  END,
  `checked_at` = COALESCE(`checked_at`, `created_at`)
WHERE `check_result` IS NULL OR `check_result` = '';
