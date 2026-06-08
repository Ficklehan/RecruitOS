-- ============================================================
-- RecruitOS v1.0 管道 + 菜单重构迁移
-- 在 init.sql + test-data.sql 之后执行
-- ============================================================
USE `recruit_os`;

-- 1. candidate_job 管道字段
-- 若列已存在可跳过对应语句
ALTER TABLE `candidate_job`
  ADD COLUMN `pipeline_stage` VARCHAR(32) NOT NULL DEFAULT 'SOURCED'
    COMMENT 'SOURCED/SCREENING/CONTACTED/INTERVIEWING/EVALUATED/OFFER/HIRED/ARCHIVED' AFTER `screening_status`;
ALTER TABLE `candidate_job` ADD COLUMN `interview_sub_stage` VARCHAR(32) DEFAULT NULL AFTER `pipeline_stage`;
ALTER TABLE `candidate_job` ADD COLUMN `rejection_reason_code` VARCHAR(32) DEFAULT NULL AFTER `interview_sub_stage`;
ALTER TABLE `candidate_job` ADD COLUMN `rejection_comment` VARCHAR(512) DEFAULT NULL AFTER `rejection_reason_code`;
ALTER TABLE `candidate_job` ADD COLUMN `archived_to_pool` TINYINT DEFAULT 0 COMMENT '淘汰后归入人才库' AFTER `rejection_comment`;

-- 2. job_position 面试方案
ALTER TABLE `job_position` ADD COLUMN `interview_plan` JSON DEFAULT NULL COMMENT '面试方案JSON' AFTER `closed_reason`;

-- 3. 管道审计 + 录用决策
CREATE TABLE IF NOT EXISTS `pipeline_stage_log` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `tenant_id` BIGINT NOT NULL,
  `candidate_job_id` BIGINT NOT NULL,
  `from_stage` VARCHAR(32) DEFAULT NULL,
  `to_stage` VARCHAR(32) NOT NULL,
  `from_sub_stage` VARCHAR(32) DEFAULT NULL,
  `to_sub_stage` VARCHAR(32) DEFAULT NULL,
  `operator_id` BIGINT NOT NULL,
  `reason_code` VARCHAR(32) DEFAULT NULL,
  `comment` VARCHAR(512) DEFAULT NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  INDEX `idx_cj` (`candidate_job_id`),
  INDEX `idx_tenant_time` (`tenant_id`, `created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='管道阶段变更日志';

CREATE TABLE IF NOT EXISTS `hiring_decision` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `tenant_id` BIGINT NOT NULL,
  `candidate_job_id` BIGINT NOT NULL,
  `candidate_id` BIGINT NOT NULL,
  `job_id` BIGINT NOT NULL,
  `decision` VARCHAR(16) NOT NULL COMMENT 'HIRE/REJECT/HOLD',
  `summary` TEXT DEFAULT NULL,
  `decided_by` BIGINT NOT NULL,
  `decided_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_cj` (`candidate_job_id`),
  INDEX `idx_job` (`tenant_id`, `job_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='录用决策汇总';

-- 4. 迁移现有 candidate_job 管道阶段
UPDATE `candidate_job` cj
JOIN `candidate` c ON c.id = cj.candidate_id
SET cj.pipeline_stage = CASE c.status
  WHEN 'NEW' THEN 'SOURCED'
  WHEN 'SCREENING' THEN 'SCREENING'
  WHEN 'INTERVIEWING' THEN 'INTERVIEWING'
  WHEN 'OFFER' THEN 'OFFER'
  WHEN 'ONBOARD' THEN 'HIRED'
  WHEN 'POOL' THEN 'ARCHIVED'
  WHEN 'BLACKLIST' THEN 'ARCHIVED'
  ELSE 'SOURCED'
END,
cj.archived_to_pool = IF(c.status = 'POOL', 1, 0)
WHERE cj.pipeline_stage = 'SOURCED' OR cj.pipeline_stage IS NULL;

-- 5. 替换菜单权限
DELETE FROM `sys_role_permission` WHERE `tenant_id` = 1;
DELETE FROM `sys_permission` WHERE `tenant_id` = 1;

INSERT INTO `sys_permission` (`id`, `tenant_id`, `parent_id`, `perm_code`, `perm_name`, `type`, `path`, `icon`, `sort_order`) VALUES
-- L1
(100, 1, 0, 'workspace', '工作台', 'MENU', '/workspace', 'Monitor', 1),
(200, 1, 0, 'pipeline', '招聘执行', 'MENU', '/pipeline', 'Operation', 2),
(300, 1, 0, 'planning', '招聘规划', 'MENU', '/planning', 'Document', 3),
(400, 1, 0, 'talent', '人才储备', 'MENU', '/talent', 'User', 4),
(500, 1, 0, 'insight', '数据洞察', 'MENU', '/insight', 'DataAnalysis', 5),
(600, 1, 0, 'settings', '设置', 'MENU', '/settings', 'Setting', 6),
-- 工作台 L2/L3
(101, 1, 100, 'workspace:inbox', '收件箱', 'MENU', '/workspace/inbox', 'Message', 1),
(102, 1, 100, 'workspace:today', '今日', 'MENU', '/workspace/today', 'Calendar', 2),
(103, 1, 100, 'workspace:dashboard', '驾驶舱', 'MENU', '/workspace/dashboard', 'Odometer', 3),
-- 招聘执行
(201, 1, 200, 'pipeline:board', '岗位管道', 'MENU', '/pipeline/board', 'Grid', 1),
(202, 1, 200, 'pipeline:candidate', '候选人', 'MENU', '/pipeline/candidates', 'User', 2),
(203, 1, 200, 'pipeline:calendar', '面试日历', 'MENU', '/pipeline/calendar', 'Calendar', 3),
(204, 1, 200, 'pipeline:offer', 'Offer', 'MENU', '/pipeline/offers', 'Tickets', 4),
(205, 1, 200, 'pipeline:onboard', '入职', 'MENU', '/pipeline/onboards', 'Finished', 5),
(206, 1, 200, 'pipeline:candidate:advance', '推进管道', 'BUTTON', NULL, NULL, 99),
(207, 1, 200, 'pipeline:offer:create', '创建Offer', 'BUTTON', NULL, NULL, 99),
-- 招聘规划
(301, 1, 300, 'planning:demand', '需求', 'MENU', '/planning/demands', 'List', 1),
(302, 1, 300, 'planning:demand:board', '需求看板', 'MENU', '/planning/demands/board', 'Grid', 2),
(303, 1, 300, 'planning:approval', '审批', 'MENU', '/planning/approvals/pending', 'Checked', 3),
(304, 1, 300, 'planning:job', '岗位', 'MENU', '/planning/jobs', 'Briefcase', 4),
(305, 1, 300, 'planning:demand:approve', '审批需求', 'BUTTON', NULL, NULL, 99),
(306, 1, 300, 'planning:job:activate', '激活岗位', 'BUTTON', NULL, NULL, 99),
-- 人才储备
(401, 1, 400, 'talent:pool', '人才库', 'MENU', '/talent/pool', 'Files', 1),
(402, 1, 400, 'talent:resume', '简历收件', 'MENU', '/talent/resumes', 'Document', 2),
(403, 1, 400, 'talent:channel:agent', 'Agent任务', 'MENU', '/talent/channels/agents', 'Connection', 3),
(404, 1, 400, 'talent:referral', '内推', 'MENU', '/talent/referral', 'Share', 4),
(405, 1, 400, 'talent:headhunter', '猎头', 'MENU', '/talent/headhunters', 'OfficeBuilding', 5),
(406, 1, 400, 'talent:template', '话术模板', 'MENU', '/talent/templates', 'ChatLineRound', 6),
(407, 1, 400, 'talent:channel:agent:start', '启动Agent', 'BUTTON', NULL, NULL, 99),
-- 数据洞察
(501, 1, 500, 'insight:funnel', '招聘漏斗', 'MENU', '/insight/funnel', 'Filter', 1),
(502, 1, 500, 'insight:cycle', '招聘周期', 'MENU', '/insight/cycle', 'Timer', 2),
(503, 1, 500, 'insight:roi', '渠道ROI', 'MENU', '/insight/roi', 'Coin', 3),
(504, 1, 500, 'insight:interviewer', '面试官效能', 'MENU', '/insight/interviewer', 'User', 4),
-- 设置
(601, 1, 600, 'settings:tenant', '租户设置', 'MENU', '/settings/tenant', 'Setting', 1),
(602, 1, 600, 'settings:org', '组织架构', 'MENU', '/settings/org', 'Tree', 2),
(603, 1, 600, 'settings:role', '角色管理', 'MENU', '/settings/role', 'Lock', 3),
(604, 1, 600, 'settings:user', '用户管理', 'MENU', '/settings/user', 'UserFilled', 4),
(605, 1, 600, 'settings:sso', 'SSO配置', 'MENU', '/settings/integration/sso', 'Link', 5),
(606, 1, 600, 'settings:license', '许可配额', 'MENU', '/settings/compliance/license', 'Tickets', 6),
(607, 1, 600, 'settings:safety', '对话安全', 'MENU', '/settings/compliance/safety', 'Warning', 7);

-- 6. 角色权限矩阵
-- SUPER_ADMIN: 全部
INSERT INTO `sys_role_permission` (`tenant_id`, `role_id`, `permission_id`)
SELECT 1, 1, id FROM `sys_permission` WHERE tenant_id = 1;

-- HR_MANAGER
INSERT INTO `sys_role_permission` (`tenant_id`, `role_id`, `permission_id`) VALUES
(1,2,100),(1,2,101),(1,2,102),(1,2,103),
(1,2,200),(1,2,201),(1,2,202),(1,2,203),(1,2,204),(1,2,205),(1,2,206),(1,2,207),
(1,2,300),(1,2,301),(1,2,302),(1,2,303),(1,2,304),(1,2,305),(1,2,306),
(1,2,400),(1,2,401),(1,2,402),(1,2,403),(1,2,404),(1,2,405),(1,2,406),(1,2,407),
(1,2,500),(1,2,501),(1,2,502),(1,2,503),(1,2,504),
(1,2,600),(1,2,601),(1,2,602),(1,2,603),(1,2,604),(1,2,605),(1,2,606),(1,2,607);

-- RECRUITER
INSERT INTO `sys_role_permission` (`tenant_id`, `role_id`, `permission_id`) VALUES
(1,3,100),(1,3,101),(1,3,102),
(1,3,200),(1,3,201),(1,3,202),(1,3,203),(1,3,204),(1,3,205),(1,3,206),(1,3,207),
(1,3,300),(1,3,301),(1,3,304),(1,3,306),
(1,3,400),(1,3,401),(1,3,402),(1,3,403),(1,3,404),(1,3,405),(1,3,406),(1,3,407);

-- INTERVIEWER
INSERT INTO `sys_role_permission` (`tenant_id`, `role_id`, `permission_id`) VALUES
(1,4,100),(1,4,101),(1,4,102),
(1,4,200),(1,4,201),(1,4,202),(1,4,203);

-- DEPT_HEAD
INSERT INTO `sys_role_permission` (`tenant_id`, `role_id`, `permission_id`) VALUES
(1,5,100),(1,5,101),(1,5,102),
(1,5,200),(1,5,201),(1,5,202),(1,5,203),(1,5,204),
(1,5,300),(1,5,301),(1,5,302),(1,5,303),(1,5,305),
(1,5,400),(1,5,404);

-- SSC
INSERT INTO `sys_role_permission` (`tenant_id`, `role_id`, `permission_id`) VALUES
(1,6,100),(1,6,101),
(1,6,200),(1,6,205),
(1,6,400);

-- EMPLOYEE (内推)
INSERT INTO `sys_role_permission` (`tenant_id`, `role_id`, `permission_id`) VALUES
(1,7,100),(1,7,400),(1,7,404);
