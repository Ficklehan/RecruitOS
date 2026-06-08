-- ============================================================
-- RecruitOS v2 表结构对齐（实体字段与 init.sql 不一致修复）
-- 在 init.sql + test-data.sql + migration-v1 之后执行
-- ============================================================
USE `recruit_os`;

-- offer 冗余展示字段 + 审批字段
ALTER TABLE `offer`
  ADD COLUMN `candidate_name` VARCHAR(64) DEFAULT NULL COMMENT '候选人姓名' AFTER `candidate_id`,
  ADD COLUMN `job_title` VARCHAR(128) DEFAULT NULL COMMENT '岗位名称' AFTER `job_id`,
  ADD COLUMN `department` VARCHAR(64) DEFAULT NULL COMMENT '部门' AFTER `job_title`,
  ADD COLUMN `approver_id` BIGINT DEFAULT NULL COMMENT '审批人' AFTER `status`,
  ADD COLUMN `approved_at` DATETIME DEFAULT NULL COMMENT '审批时间' AFTER `approver_id`,
  ADD COLUMN `remark` TEXT DEFAULT NULL COMMENT '备注' AFTER `accepted_at`;

-- onboard 冗余字段（status 列已存在，实体映射 onboardStatus -> status）
ALTER TABLE `onboard`
  ADD COLUMN `candidate_name` VARCHAR(64) DEFAULT NULL AFTER `candidate_id`,
  ADD COLUMN `job_id` BIGINT DEFAULT NULL AFTER `candidate_name`,
  ADD COLUMN `job_title` VARCHAR(128) DEFAULT NULL AFTER `job_id`,
  ADD COLUMN `hr_id` BIGINT DEFAULT NULL AFTER `onboard_date`,
  ADD COLUMN `hr_name` VARCHAR(64) DEFAULT NULL AFTER `hr_id`,
  ADD COLUMN `created_by` BIGINT DEFAULT NULL AFTER `remark`;

-- onboard_task 负责人姓名
ALTER TABLE `onboard_task`
  ADD COLUMN `assignee_name` VARCHAR(64) DEFAULT NULL AFTER `assignee_id`;

-- sys_permission 补齐 BaseEntity 字段
ALTER TABLE `sys_permission`
  ADD COLUMN `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP AFTER `created_at`;

-- 统一 Offer 状态枚举（服务层使用 PENDING 而非 PENDING_APPROVAL）
UPDATE `offer` SET `status` = 'PENDING' WHERE `status` = 'PENDING_APPROVAL';

-- 回填 offer / onboard 冗余字段
UPDATE `offer` o
JOIN `candidate` c ON c.id = o.candidate_id
JOIN `job_position` j ON j.id = o.job_id
LEFT JOIN `recruit_demand` d ON d.id = j.demand_id
LEFT JOIN `organization` org ON org.id = d.org_id
SET o.candidate_name = c.name,
    o.job_title = j.title,
    o.department = COALESCE(org.name, '技术部')
WHERE o.candidate_name IS NULL OR o.job_title IS NULL;

UPDATE `onboard` ob
JOIN `candidate` c ON c.id = ob.candidate_id
LEFT JOIN `offer` o ON o.id = ob.offer_id
SET ob.candidate_name = c.name,
    ob.job_id = COALESCE(ob.job_id, o.job_id),
    ob.job_title = COALESCE(ob.job_title, o.job_title),
    ob.hr_id = COALESCE(ob.hr_id, 2),
    ob.hr_name = COALESCE(ob.hr_name, '张三'),
    ob.created_by = COALESCE(ob.created_by, 2)
WHERE ob.candidate_name IS NULL;

-- onboard 状态与服务层对齐：IN_PROGRESS -> CONFIRMED
UPDATE `onboard` SET `status` = 'CONFIRMED' WHERE `status` = 'IN_PROGRESS';
