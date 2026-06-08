-- 补齐渠道与账号菜单权限（v6 可能因字段名未写入）
USE `recruit_os`;

INSERT IGNORE INTO `sys_permission`
    (`id`, `tenant_id`, `parent_id`, `perm_code`, `perm_name`, `type`, `path`, `icon`, `sort_order`)
VALUES
    (408, 1, 400, 'talent:channel', '渠道与账号', 'MENU', '/talent/channels', 'Share', 2),
    (410, 1, 400, 'talent:channel:manage', '管理渠道', 'BUTTON', NULL, NULL, 99);

INSERT IGNORE INTO `sys_role_permission` (`tenant_id`, `role_id`, `permission_id`)
SELECT 1, r.id, p.id
FROM `sys_role` r
CROSS JOIN `sys_permission` p
WHERE r.tenant_id = 1
  AND r.role_code IN ('SUPER_ADMIN', 'HR_MANAGER', 'RECRUITER')
  AND p.id IN (408, 410);
