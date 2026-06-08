-- ============================================================
-- RecruitOS v5 内推表结构对齐（实体冗余字段 + referral_reward）
-- ============================================================
USE `recruit_os`;

ALTER TABLE `referral`
  ADD COLUMN `referrer_name` VARCHAR(64) DEFAULT NULL COMMENT '推荐人姓名' AFTER `referrer_id`,
  ADD COLUMN `candidate_name` VARCHAR(64) DEFAULT NULL COMMENT '候选人姓名' AFTER `candidate_id`,
  ADD COLUMN `job_title` VARCHAR(128) DEFAULT NULL COMMENT '岗位名称' AFTER `job_id`,
  ADD COLUMN `created_by` BIGINT DEFAULT NULL COMMENT '创建人' AFTER `remark`;

UPDATE `referral` r
LEFT JOIN `sys_user` u ON u.id = r.referrer_id
LEFT JOIN `candidate` c ON c.id = r.candidate_id
LEFT JOIN `job_position` j ON j.id = r.job_id
SET r.referrer_name = COALESCE(r.referrer_name, u.real_name),
    r.candidate_name = COALESCE(r.candidate_name, c.name),
    r.job_title = COALESCE(r.job_title, j.title),
    r.created_by = COALESCE(r.created_by, r.referrer_id);

CREATE TABLE IF NOT EXISTS `referral_reward` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `tenant_id` BIGINT NOT NULL,
    `referral_id` BIGINT NOT NULL,
    `referrer_id` BIGINT NOT NULL,
    `referrer_name` VARCHAR(64) DEFAULT NULL,
    `reward_type` VARCHAR(16) DEFAULT 'CASH' COMMENT 'CASH/GIFT/OTHER',
    `reward_amount` DECIMAL(12,2) DEFAULT NULL,
    `status` VARCHAR(16) DEFAULT 'PENDING' COMMENT 'PENDING/APPROVED/PAID/CANCELLED',
    `approved_by` BIGINT DEFAULT NULL,
    `approved_at` DATETIME DEFAULT NULL,
    `paid_at` DATETIME DEFAULT NULL,
    `remark` VARCHAR(256) DEFAULT NULL,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    INDEX `idx_tenant_referral` (`tenant_id`, `referral_id`),
    INDEX `idx_tenant_status` (`tenant_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='内推奖励表';
