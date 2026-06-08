-- ============================================================
-- RecruitOS v7 渠道收窄：仅保留 Boss直聘、猎聘
-- 在 v6 已执行后运行
-- ============================================================
USE `recruit_os`;

-- 停用/移除其他招聘平台渠道
UPDATE `recruitment_channel`
SET `status` = 'DISABLED', `supports_agent` = 0
WHERE `channel_type` = 'PLATFORM'
  AND `platform_code` NOT IN ('BOSS', 'LIEPIN');

DELETE FROM `recruitment_channel`
WHERE `channel_type` = 'PLATFORM'
  AND `platform_code` NOT IN ('BOSS', 'LIEPIN')
  AND `is_system` = 0;

-- 确保 Boss / 猎聘 为启用且支持 Agent
UPDATE `recruitment_channel`
SET `status` = 'ACTIVE', `supports_agent` = 1, `is_system` = 1
WHERE `tenant_id` = 1
  AND `channel_code` IN ('BOSS', 'LIEPIN');
