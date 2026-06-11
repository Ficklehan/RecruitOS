-- Align ab_test table with AbTest entity (variant-based schema)
DROP TABLE IF EXISTS `ab_test`;

CREATE TABLE `ab_test` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `tenant_id` BIGINT NOT NULL,
    `job_id` BIGINT NOT NULL,
    `test_name` VARCHAR(128) NOT NULL,
    `test_type` VARCHAR(16) DEFAULT 'WEIGHT' COMMENT 'WEIGHT/MATCH/SEARCH',
    `job_title` VARCHAR(128) DEFAULT NULL,
    `variant_a` JSON DEFAULT NULL,
    `variant_b` JSON DEFAULT NULL,
    `status` VARCHAR(16) DEFAULT 'DRAFT' COMMENT 'DRAFT/RUNNING/COMPLETED/CANCELLED',
    `winner_variant` VARCHAR(8) DEFAULT 'NONE' COMMENT 'A/B/NONE',
    `start_date` DATE DEFAULT NULL,
    `end_date` DATE DEFAULT NULL,
    `sample_size_a` INT DEFAULT 0,
    `sample_size_b` INT DEFAULT 0,
    `conversion_rate_a` DOUBLE DEFAULT 0,
    `conversion_rate_b` DOUBLE DEFAULT 0,
    `created_by` BIGINT DEFAULT NULL,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    INDEX `idx_job` (`tenant_id`, `job_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='A/B测试表';

INSERT INTO `ab_test` (`tenant_id`, `job_id`, `test_name`, `test_type`, `job_title`, `variant_a`, `variant_b`, `status`, `start_date`, `sample_size_a`, `sample_size_b`, `conversion_rate_a`, `conversion_rate_b`, `created_at`) VALUES
(1, 1, 'Vue3权重提升实验', 'WEIGHT', '高级前端工程师',
 '{"matchWeight":{"Vue3":0.35,"TypeScript":0.25}}',
 '{"matchWeight":{"Vue3":0.40,"TypeScript":0.28}}',
 'RUNNING', '2026-06-01', 120, 118, 23.4, 27.1, '2026-06-01 10:00:00'),
(1, 2, '微服务权重实验', 'WEIGHT', 'Java后端工程师',
 '{"matchWeight":{"Java":0.30,"微服务":0.20}}',
 '{"matchWeight":{"Java":0.33,"微服务":0.25}}',
 'DRAFT', NULL, 0, 0, 0, 0, '2026-06-05 09:00:00');
