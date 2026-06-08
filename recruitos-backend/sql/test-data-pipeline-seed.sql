-- ============================================================
-- 管道 + 录用决策 + 验收链路补全（在 test-data + migration-v1/v2 后执行）
-- ============================================================
USE `recruit_os`;

-- 1. 管道阶段（按候选人状态 + 业务场景细化）
UPDATE `candidate_job` cj
JOIN `candidate` c ON c.id = cj.candidate_id
SET cj.pipeline_stage = CASE
  WHEN c.id = 1  AND cj.job_id = 1 THEN 'EVALUATED'      -- 张伟：待决策/Offer审批中
  WHEN c.id = 2  AND cj.job_id = 1 THEN 'INTERVIEWING'   -- 李娜：初面进行中
  WHEN c.id = 5  AND cj.job_id = 1 THEN 'OFFER'          -- 陈浩：Offer已发送
  WHEN c.id = 6  AND cj.job_id = 4 THEN 'HIRED'           -- 刘洋：已入职
  WHEN c.id = 7  AND cj.job_id = 1 THEN 'SCREENING'      -- 杨帆：待安排面试
  WHEN c.id = 12 AND cj.job_id = 1 THEN 'INTERVIEWING'   -- 马丽：初面已安排
  WHEN c.id = 4  AND cj.job_id = 3 THEN 'EVALUATED'      -- 赵敏：待决策
  WHEN c.id = 15 AND cj.job_id = 2 THEN 'SCREENING'      -- 徐明：后端筛选通过
  WHEN cj.screening_status = 'REJECTED' THEN 'ARCHIVED'
  WHEN c.status = 'NEW' THEN 'SOURCED'
  WHEN c.status = 'SCREENING' THEN 'SCREENING'
  WHEN c.status = 'INTERVIEWING' THEN 'INTERVIEWING'
  WHEN c.status = 'OFFER' THEN 'OFFER'
  WHEN c.status = 'ONBOARD' THEN 'HIRED'
  ELSE cj.pipeline_stage
END,
cj.interview_sub_stage = CASE
  WHEN c.id = 1 AND cj.job_id = 1 THEN 'FINAL_PENDING'
  WHEN c.id = 2 AND cj.job_id = 1 THEN 'INITIAL'
  WHEN c.id = 12 AND cj.job_id = 1 THEN 'INITIAL'
  ELSE cj.interview_sub_stage
END;

-- 2. 录用决策（Offer 前置条件 R9）
INSERT INTO `hiring_decision` (`tenant_id`, `candidate_job_id`, `candidate_id`, `job_id`, `decision`, `summary`, `decided_by`)
SELECT cj.tenant_id, cj.id, cj.candidate_id, cj.job_id, 'HIRE', '终面通过，建议录用', 7
FROM `candidate_job` cj
WHERE (cj.candidate_id = 5 AND cj.job_id = 1)
   OR (cj.candidate_id = 6 AND cj.job_id = 4)
   OR (cj.candidate_id = 1 AND cj.job_id = 1)
ON DUPLICATE KEY UPDATE `decision` = VALUES(`decision`);

INSERT INTO `hiring_decision` (`tenant_id`, `candidate_job_id`, `candidate_id`, `job_id`, `decision`, `summary`, `decided_by`)
SELECT cj.tenant_id, cj.id, cj.candidate_id, cj.job_id, 'HIRE', '产品面试通过', 5
FROM `candidate_job` cj
WHERE cj.candidate_id = 4 AND cj.job_id = 3
ON DUPLICATE KEY UPDATE `decision` = VALUES(`decision`);

-- 3. 管道变更日志（候选人360时间线）
INSERT INTO `pipeline_stage_log` (`tenant_id`, `candidate_job_id`, `from_stage`, `to_stage`, `operator_id`, `comment`, `created_at`)
SELECT 1, cj.id, 'SOURCED', 'SCREENING', 2, '简历初筛通过', DATE_SUB(NOW(), INTERVAL 14 DAY)
FROM `candidate_job` cj WHERE cj.candidate_id IN (1,5,6) AND cj.job_id IN (1,4);

INSERT INTO `pipeline_stage_log` (`tenant_id`, `candidate_job_id`, `from_stage`, `to_stage`, `operator_id`, `comment`, `created_at`)
SELECT 1, cj.id, 'SCREENING', 'INTERVIEWING', 2, '安排初面', DATE_SUB(NOW(), INTERVAL 10 DAY)
FROM `candidate_job` cj WHERE cj.candidate_id IN (1,5) AND cj.job_id = 1;

INSERT INTO `pipeline_stage_log` (`tenant_id`, `candidate_job_id`, `from_stage`, `to_stage`, `operator_id`, `comment`, `created_at`)
SELECT 1, cj.id, 'INTERVIEWING', 'EVALUATED', 7, '复试通过，进入决策', DATE_SUB(NOW(), INTERVAL 3 DAY)
FROM `candidate_job` cj WHERE cj.candidate_id = 5 AND cj.job_id = 1;

INSERT INTO `pipeline_stage_log` (`tenant_id`, `candidate_job_id`, `from_stage`, `to_stage`, `operator_id`, `comment`, `created_at`)
SELECT 1, cj.id, 'EVALUATED', 'OFFER', 2, '发放Offer', DATE_SUB(NOW(), INTERVAL 2 DAY)
FROM `candidate_job` cj WHERE cj.candidate_id = 5 AND cj.job_id = 1;

-- 4. admin 账号待办：需求审批 + Offer 待审
UPDATE `approval_instance` SET `current_approver_id` = 1 WHERE `id` = 3;
UPDATE `recruit_demand` SET `status` = 'PENDING' WHERE `id` = 3;

INSERT IGNORE INTO `notification` (`tenant_id`, `user_id`, `type`, `title`, `content`, `biz_type`, `biz_id`, `is_read`) VALUES
(1, 1, 'DEMAND_APPROVAL', '需求待您审批', '产品经理招聘需求等待您审批', 'DEMAND', 3, 0),
(1, 1, 'OFFER_APPROVAL', 'Offer待您审批', '候选人张伟的Offer等待审批', 'OFFER', 3, 0),
(1, 1, 'INTERVIEW_ARRANGE', '今日复试', '候选人张伟今日14:00复试', 'INTERVIEW', 2, 0);

-- 5. 今日面试：将张伟复试调整到今日（便于「今日日程」验收）
UPDATE `interview` SET
  `scheduled_start_time` = CONCAT(CURDATE(), ' 14:00:00'),
  `scheduled_end_time` = CONCAT(CURDATE(), ' 15:00:00'),
  `status` = 'ARRANGED'
WHERE `id` = 2;

UPDATE `interview` SET
  `scheduled_start_time` = CONCAT(CURDATE(), ' 10:00:00'),
  `scheduled_end_time` = CONCAT(CURDATE(), ' 11:00:00'),
  `status` = 'IN_PROGRESS'
WHERE `id` = 3;

-- 6. 岗位面试方案
UPDATE `job_position` SET `interview_plan` = JSON_OBJECT(
  'rounds', JSON_ARRAY(
    JSON_OBJECT('round', 'INITIAL', 'name', '技术初面', 'interviewerRole', 'INTERVIEWER'),
    JSON_OBJECT('round', 'FINAL', 'name', '负责人终面', 'interviewerRole', 'DEPT_MANAGER')
  )
) WHERE `id` IN (1, 2, 3);
