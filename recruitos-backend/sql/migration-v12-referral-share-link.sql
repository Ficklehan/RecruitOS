-- Referral share link for employee referral v1
CREATE TABLE IF NOT EXISTS `referral_share_link` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `tenant_id` BIGINT NOT NULL,
    `token` VARCHAR(64) NOT NULL,
    `job_id` BIGINT NOT NULL,
    `job_title` VARCHAR(128) DEFAULT NULL,
    `referrer_id` BIGINT NOT NULL,
    `referrer_name` VARCHAR(64) DEFAULT NULL,
    `expires_at` DATETIME DEFAULT NULL,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_token` (`token`),
    INDEX `idx_tenant_job` (`tenant_id`, `job_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='内推分享链接';
