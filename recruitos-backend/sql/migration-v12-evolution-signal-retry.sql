-- 进化信号发射失败重试队列（跨模块 emit 失败时落库，由 evolution 服务定时重放）
USE `recruit_os`;

CREATE TABLE IF NOT EXISTS `evolution_signal_retry` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `tenant_id` BIGINT NOT NULL,
    `payload_json` JSON NOT NULL COMMENT 'EvolutionEmitRequest JSON',
    `attempts` INT NOT NULL DEFAULT 0,
    `max_attempts` INT NOT NULL DEFAULT 5,
    `next_retry_at` DATETIME NOT NULL,
    `last_error` VARCHAR(512) DEFAULT NULL,
    `status` VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING|DONE|FAILED',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    INDEX `idx_retry_status_next` (`status`, `next_retry_at`),
    INDEX `idx_retry_tenant` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='进化信号发射重试队列';
