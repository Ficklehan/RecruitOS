-- ============================================================
-- RecruitOS v6 招聘渠道管理 + 渠道账号关联
-- ============================================================
USE `recruit_os`;

CREATE TABLE IF NOT EXISTS `recruitment_channel` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `tenant_id` BIGINT NOT NULL,
    `channel_code` VARCHAR(32) NOT NULL COMMENT '渠道编码 BOSS/LIEPIN/REFERRAL 等',
    `channel_name` VARCHAR(64) NOT NULL COMMENT '展示名称',
    `channel_type` VARCHAR(16) NOT NULL COMMENT 'PLATFORM/REFERRAL/HEADHUNTER/DIRECT/CAREERS',
    `platform_code` VARCHAR(16) DEFAULT NULL COMMENT 'PLATFORM 类型对应 BOSS/LIEPIN',
    `description` VARCHAR(512) DEFAULT NULL,
    `status` VARCHAR(16) NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE/DISABLED',
    `sort_order` INT NOT NULL DEFAULT 0,
    `supports_agent` TINYINT NOT NULL DEFAULT 0 COMMENT '是否支持 Agent 自动化',
    `is_system` TINYINT NOT NULL DEFAULT 0 COMMENT '系统预置不可删',
    `config_json` JSON DEFAULT NULL,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_tenant_code` (`tenant_id`, `channel_code`),
    INDEX `idx_tenant_type` (`tenant_id`, `channel_type`),
    INDEX `idx_tenant_status` (`tenant_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='招聘渠道表';

ALTER TABLE `agent_account`
    ADD COLUMN `channel_id` BIGINT DEFAULT NULL COMMENT '所属招聘渠道' AFTER `tenant_id`,
    ADD INDEX `idx_channel` (`channel_id`);

-- 租户 1 预置渠道（当前仅 Boss直聘、猎聘）
INSERT IGNORE INTO `recruitment_channel`
    (`id`, `tenant_id`, `channel_code`, `channel_name`, `channel_type`, `platform_code`, `description`, `status`, `sort_order`, `supports_agent`, `is_system`)
VALUES
    (1001, 1, 'BOSS', 'Boss直聘', 'PLATFORM', 'BOSS', 'Boss直聘平台自动化寻源', 'ACTIVE', 10, 1, 1),
    (1002, 1, 'LIEPIN', '猎聘', 'PLATFORM', 'LIEPIN', '猎聘平台自动化寻源', 'ACTIVE', 20, 1, 1);

-- 已有账号按 platform 关联渠道
UPDATE `agent_account` aa
INNER JOIN `recruitment_channel` rc
    ON aa.tenant_id = rc.tenant_id
   AND aa.platform = rc.platform_code
   AND rc.channel_type = 'PLATFORM'
SET aa.channel_id = rc.id
WHERE aa.channel_id IS NULL;

-- 菜单权限
INSERT IGNORE INTO `sys_permission`
    (`id`, `tenant_id`, `parent_id`, `perm_code`, `perm_name`, `type`, `path`, `icon`, `sort_order`)
VALUES
    (408, 1, 400, 'talent:channel', '渠道与账号', 'MENU', '/talent/channels', 'Share', 2),
    (410, 1, 400, 'talent:channel:manage', '管理渠道', 'BUTTON', NULL, NULL, 99);

-- 角色授权：SUPER_ADMIN(1) HR_MANAGER(2) RECRUITER(3)
INSERT IGNORE INTO `sys_role_permission` (`tenant_id`, `role_id`, `permission_id`)
SELECT 1, r.id, p.id
FROM `sys_role` r
CROSS JOIN `sys_permission` p
WHERE r.tenant_id = 1
  AND r.code IN ('SUPER_ADMIN', 'HR_MANAGER', 'RECRUITER')
  AND p.id IN (408, 409, 410);
