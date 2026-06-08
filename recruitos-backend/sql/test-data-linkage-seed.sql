-- ============================================================
-- RecruitOS 演示数据关联补全
-- 在 test-data.sql + test-data-pipeline-seed.sql 之后执行
-- 用途：简历、寻源活动、管道时间线、Offer 审批、匹配明细等端到端可验证
-- ============================================================
USE `recruit_os`;

-- ============================================================
-- 1. 简历（关联候选人，支撑「简历收件」「人才库」）
-- ============================================================
DELETE FROM `resume` WHERE `tenant_id` = 1;

INSERT INTO `resume` (`id`, `tenant_id`, `candidate_id`, `file_name`, `file_url`, `file_type`, `parsed_json`, `raw_text`, `parse_status`, `version`, `created_at`) VALUES
(1,  1, 1,  '张伟-高级前端.pdf',   '/demo/resumes/zhangwei.pdf',   'pdf',
 '{"basic":{"name":"张伟","phone":"13900000001","email":"zhangwei@test.com","company":"阿里巴巴","position":"高级前端工程师","workYears":6,"education":"硕士"},"skills":["Vue3","TypeScript","前端架构","React"],"summary":"6年前端经验，阿里核心产品线"}',
 '张伟，阿里巴巴高级前端工程师，精通 Vue3/TypeScript', 2, 1, '2026-06-01 10:00:00'),
(2,  1, 2,  '李娜-前端.pdf',       '/demo/resumes/lina.pdf',       'pdf',
 '{"basic":{"name":"李娜","phone":"13900000002","email":"lina@test.com","company":"腾讯","position":"前端工程师","workYears":4,"education":"本科"},"skills":["Vue3","JavaScript","Webpack"],"summary":"腾讯前端，Vue3 项目经验丰富"}',
 '李娜，腾讯前端工程师', 2, 1, '2026-06-02 11:00:00'),
(3,  1, 5,  '陈浩-前端架构.pdf',   '/demo/resumes/chenhao.pdf',    'pdf',
 '{"basic":{"name":"陈浩","phone":"13900000005","email":"chenhao@test.com","company":"百度","position":"高级前端","workYears":8,"education":"本科"},"skills":["Vue3","React","前端架构","工程化"],"summary":"资深前端架构师"}',
 '陈浩，百度高级前端，架构能力突出', 2, 1, '2026-05-25 09:00:00'),
(4,  1, 6,  '刘洋-数据分析.pdf',   '/demo/resumes/liuyang.pdf',    'pdf',
 '{"basic":{"name":"刘洋","phone":"13900000006","email":"liuyang@test.com","company":"京东","position":"数据分析师","workYears":4,"education":"硕士"},"skills":["SQL","Python","BI工具","数据分析"],"summary":"招聘数据分析方向"}',
 '刘洋，京东数据分析师', 2, 1, '2026-05-15 14:00:00'),
(5,  1, 4,  '赵敏-产品经理.pdf',   '/demo/resumes/zhaomin.pdf',    'pdf',
 '{"basic":{"name":"赵敏","phone":"13900000004","email":"zhaomin@test.com","company":"美团","position":"产品经理","workYears":7,"education":"硕士"},"skills":["B端产品","需求分析","HR SaaS"],"summary":"B端产品经验丰富"}',
 '赵敏，美团产品经理', 2, 1, '2026-06-04 16:00:00'),
(6,  1, 12, '马丽-前端.pdf',       '/demo/resumes/mali.pdf',       'pdf',
 '{"basic":{"name":"马丽","phone":"13900000012","email":"mali@test.com","company":"蚂蚁集团","position":"前端工程师","workYears":5,"education":"硕士"},"skills":["React","Vue3","TypeScript"],"summary":"React 经验丰富"}',
 '马丽，蚂蚁集团前端', 2, 1, '2026-06-05 10:00:00'),
(7,  1, 3,  '王强-Java.pdf',       '/demo/resumes/wangqiang.pdf',  'pdf',
 '{"basic":{"name":"王强","phone":"13900000003","email":"wangqiang@test.com","company":"字节跳动","position":"Java工程师","workYears":5,"education":"本科"},"skills":["Java","Spring Boot","微服务"],"summary":"微服务架构经验"}',
 '王强，字节 Java 工程师', 2, 1, '2026-06-03 09:00:00'),
(8,  1, 15, '徐明-后端.pdf',       '/demo/resumes/xuming.pdf',     'pdf',
 '{"basic":{"name":"徐明","phone":"13900000015","email":"xuming@test.com","company":"腾讯","position":"后端开发","workYears":4,"education":"硕士"},"skills":["Java","微服务","MySQL","Redis"],"summary":"微服务经验丰富"}',
 '徐明，腾讯后端开发', 2, 1, '2026-06-06 11:00:00'),
(9,  1, 8,  '周婷-待解析.pdf',       '/demo/resumes/zhouting-pending.pdf', 'pdf',
 NULL, NULL, 0, 1, '2026-06-07 08:00:00'),
(10, 1, 11, '孙超-解析失败.pdf',     '/demo/resumes/sunchao-failed.pdf',   'pdf',
 NULL, '文件损坏无法提取', 3, 1, '2026-06-07 09:00:00');

-- ============================================================
-- 2. 候选人标签（人才库技能搜索）
-- ============================================================
UPDATE `candidate` SET `tags` = '["Vue3","TypeScript","前端架构","React"]' WHERE `id` = 1;
UPDATE `candidate` SET `tags` = '["Vue3","JavaScript","Webpack"]' WHERE `id` = 2;
UPDATE `candidate` SET `tags` = '["Java","Spring Boot","微服务"]' WHERE `id` = 3;
UPDATE `candidate` SET `tags` = '["B端产品","需求分析","HR SaaS"]' WHERE `id` = 4;
UPDATE `candidate` SET `tags` = '["Vue3","React","前端架构","工程化"]' WHERE `id` = 5;
UPDATE `candidate` SET `tags` = '["SQL","Python","数据分析","BI工具"]' WHERE `id` = 6;
UPDATE `candidate` SET `tags` = '["Vue3","JavaScript"]' WHERE `id` = 7;
UPDATE `candidate` SET `tags` = '["Java","MySQL","Redis"]' WHERE `id` = 15;
UPDATE `candidate` SET `tags` = '["React","Vue3","TypeScript"]' WHERE `id` = 12;
UPDATE `candidate` SET `tags` = '["前端架构","Vue3"]' WHERE `id` = 9;

-- ============================================================
-- 3. 岗位匹配明细（候选人列表/管道卡片展示依据）
-- ============================================================
UPDATE `candidate_job` SET `match_detail` = '{"Vue3":92,"TypeScript":88,"前端架构":90,"React":75}' WHERE `candidate_id` = 1 AND `job_id` = 1;
UPDATE `candidate_job` SET `match_detail` = '{"Vue3":85,"JavaScript":80,"Webpack":72}' WHERE `candidate_id` = 2 AND `job_id` = 1;
UPDATE `candidate_job` SET `match_detail` = '{"Vue3":95,"React":90,"前端架构":93,"工程化":88}' WHERE `candidate_id` = 5 AND `job_id` = 1;
UPDATE `candidate_job` SET `match_detail` = '{"SQL":90,"数据分析":88,"Python":85,"BI工具":82}' WHERE `candidate_id` = 6 AND `job_id` = 4;
UPDATE `candidate_job` SET `match_detail` = '{"B端产品":88,"需求分析":85,"HR SaaS":90}' WHERE `candidate_id` = 4 AND `job_id` = 3;
UPDATE `candidate_job` SET `match_detail` = '{"Java":76,"Spring Boot":78,"微服务":72}' WHERE `candidate_id` = 3 AND `job_id` = 2;
UPDATE `candidate_job` SET `match_detail` = '{"Java":85,"微服务":88,"MySQL":80,"Redis":78}' WHERE `candidate_id` = 15 AND `job_id` = 2;
UPDATE `candidate_job` SET `match_detail` = '{"React":84,"Vue3":80,"TypeScript":78}' WHERE `candidate_id` = 12 AND `job_id` = 1;

-- 沟通中阶段（管道 CONTACTED 列可验证）
UPDATE `candidate_job` SET `pipeline_stage` = 'CONTACTED', `screening_status` = 'PENDING'
WHERE `candidate_id` = 3 AND `job_id` = 2;

UPDATE `candidate_job` SET `pipeline_stage` = 'CONTACTED', `screening_status` = 'PENDING'
WHERE `candidate_id` = 8 AND `job_id` = 2;

-- ============================================================
-- 4. 完整管道时间线（候选人 360 页）
-- ============================================================
DELETE FROM `pipeline_stage_log` WHERE `tenant_id` = 1;

-- 张伟 → 高级前端（EVALUATED，待 Offer 审批）
INSERT INTO `pipeline_stage_log` (`tenant_id`, `candidate_job_id`, `from_stage`, `to_stage`, `operator_id`, `comment`, `created_at`)
SELECT 1, cj.id, NULL, 'SOURCED', 2, 'BOSS 获客入库', DATE_SUB(NOW(), INTERVAL 20 DAY)
FROM `candidate_job` cj WHERE cj.candidate_id = 1 AND cj.job_id = 1;
INSERT INTO `pipeline_stage_log` (`tenant_id`, `candidate_job_id`, `from_stage`, `to_stage`, `operator_id`, `comment`, `created_at`)
SELECT 1, cj.id, 'SOURCED', 'SCREENING', 2, '简历初筛通过，匹配分 92.5', DATE_SUB(NOW(), INTERVAL 18 DAY)
FROM `candidate_job` cj WHERE cj.candidate_id = 1 AND cj.job_id = 1;
INSERT INTO `pipeline_stage_log` (`tenant_id`, `candidate_job_id`, `from_stage`, `to_stage`, `operator_id`, `comment`, `created_at`)
SELECT 1, cj.id, 'SCREENING', 'INTERVIEWING', 2, '安排初面（面试官：王五）', DATE_SUB(NOW(), INTERVAL 14 DAY)
FROM `candidate_job` cj WHERE cj.candidate_id = 1 AND cj.job_id = 1;
INSERT INTO `pipeline_stage_log` (`tenant_id`, `candidate_job_id`, `from_stage`, `to_stage`, `operator_id`, `comment`, `created_at`)
SELECT 1, cj.id, 'INTERVIEWING', 'EVALUATED', 7, '初面通过，待终面', DATE_SUB(NOW(), INTERVAL 7 DAY)
FROM `candidate_job` cj WHERE cj.candidate_id = 1 AND cj.job_id = 1;

-- 陈浩 → 高级前端（OFFER 已发送）
INSERT INTO `pipeline_stage_log` (`tenant_id`, `candidate_job_id`, `from_stage`, `to_stage`, `operator_id`, `comment`, `created_at`)
SELECT 1, cj.id, 'SOURCED', 'SCREENING', 2, '内推入库', DATE_SUB(NOW(), INTERVAL 30 DAY)
FROM `candidate_job` cj WHERE cj.candidate_id = 5 AND cj.job_id = 1;
INSERT INTO `pipeline_stage_log` (`tenant_id`, `candidate_job_id`, `from_stage`, `to_stage`, `operator_id`, `comment`, `created_at`)
SELECT 1, cj.id, 'SCREENING', 'INTERVIEWING', 2, '安排初面+复试', DATE_SUB(NOW(), INTERVAL 20 DAY)
FROM `candidate_job` cj WHERE cj.candidate_id = 5 AND cj.job_id = 1;
INSERT INTO `pipeline_stage_log` (`tenant_id`, `candidate_job_id`, `from_stage`, `to_stage`, `operator_id`, `comment`, `created_at`)
SELECT 1, cj.id, 'INTERVIEWING', 'EVALUATED', 7, '复试通过，录用决策 HIRE', DATE_SUB(NOW(), INTERVAL 5 DAY)
FROM `candidate_job` cj WHERE cj.candidate_id = 5 AND cj.job_id = 1;
INSERT INTO `pipeline_stage_log` (`tenant_id`, `candidate_job_id`, `from_stage`, `to_stage`, `operator_id`, `comment`, `created_at`)
SELECT 1, cj.id, 'EVALUATED', 'OFFER', 2, '发放 Offer 52K', DATE_SUB(NOW(), INTERVAL 3 DAY)
FROM `candidate_job` cj WHERE cj.candidate_id = 5 AND cj.job_id = 1;

-- 刘洋 → 数据分析师（HIRED）
INSERT INTO `pipeline_stage_log` (`tenant_id`, `candidate_job_id`, `from_stage`, `to_stage`, `operator_id`, `comment`, `created_at`)
SELECT 1, cj.id, 'INTERVIEWING', 'EVALUATED', 7, '复试通过', DATE_SUB(NOW(), INTERVAL 25 DAY)
FROM `candidate_job` cj WHERE cj.candidate_id = 6 AND cj.job_id = 4;
INSERT INTO `pipeline_stage_log` (`tenant_id`, `candidate_job_id`, `from_stage`, `to_stage`, `operator_id`, `comment`, `created_at`)
SELECT 1, cj.id, 'EVALUATED', 'OFFER', 2, 'Offer 已接受', DATE_SUB(NOW(), INTERVAL 20 DAY)
FROM `candidate_job` cj WHERE cj.candidate_id = 6 AND cj.job_id = 4;
INSERT INTO `pipeline_stage_log` (`tenant_id`, `candidate_job_id`, `from_stage`, `to_stage`, `operator_id`, `comment`, `created_at`)
SELECT 1, cj.id, 'OFFER', 'HIRED', 2, '已入职', DATE_SUB(NOW(), INTERVAL 10 DAY)
FROM `candidate_job` cj WHERE cj.candidate_id = 6 AND cj.job_id = 4;

-- 李娜 → 初面进行中
INSERT INTO `pipeline_stage_log` (`tenant_id`, `candidate_job_id`, `from_stage`, `to_stage`, `operator_id`, `comment`, `created_at`)
SELECT 1, cj.id, 'SOURCED', 'SCREENING', 2, '猎聘获客', DATE_SUB(NOW(), INTERVAL 10 DAY)
FROM `candidate_job` cj WHERE cj.candidate_id = 2 AND cj.job_id = 1;
INSERT INTO `pipeline_stage_log` (`tenant_id`, `candidate_job_id`, `from_stage`, `to_stage`, `operator_id`, `comment`, `created_at`)
SELECT 1, cj.id, 'SCREENING', 'INTERVIEWING', 2, '初面进行中', DATE_SUB(NOW(), INTERVAL 2 DAY)
FROM `candidate_job` cj WHERE cj.candidate_id = 2 AND cj.job_id = 1;

-- ============================================================
-- 5. Offer 审批（收件箱 / admin 待办可验证）
-- ============================================================
INSERT IGNORE INTO `approval_instance` (`id`, `tenant_id`, `biz_type`, `biz_id`, `status`, `current_node`, `current_approver_id`) VALUES
(5, 1, 'OFFER', 3, 'RUNNING', 'HR_MANAGER', 1);

INSERT IGNORE INTO `approval_record` (`tenant_id`, `instance_id`, `node_name`, `approver_id`, `action`, `comment`) VALUES
(1, 5, '提交申请', 2, 'APPROVE', '张伟终面通过，提交 Offer 审批');

UPDATE `offer` SET
  `candidate_name` = '张伟',
  `job_title` = '高级前端工程师',
  `department` = '技术部',
  `status` = 'PENDING',
  `approver_id` = NULL
WHERE `id` = 3;

-- ============================================================
-- 6. 寻源活动（岗位寻源页 / Agent 工作流可验证）
-- ============================================================
DELETE FROM `campaign_candidate_trace` WHERE `tenant_id` = 1;
DELETE FROM `campaign_platform_run` WHERE `tenant_id` = 1;
DELETE FROM `candidate_acquire_lock` WHERE `tenant_id` = 1;
DELETE FROM `job_sourcing_campaign` WHERE `tenant_id` = 1;

INSERT INTO `job_sourcing_campaign` (`id`, `tenant_id`, `job_id`, `name`, `mode`, `status`, `config_json`, `stats_json`, `resume_confirm_required`, `publish_confirm_required`, `created_by`, `started_at`, `created_at`) VALUES
(1, 1, 1, '高级前端工程师-渠道寻源', 'SEMI_AUTO', 'RUNNING',
 '{"keywords":["Vue3","TypeScript","前端架构"],"dailyLimit":50,"templateId":1,"platformConfigs":[{"platform":"BOSS","primaryAccountId":1},{"platform":"LIEPIN","primaryAccountId":2}]}',
 '{"published":2,"searched":156,"locked":12,"greeted":45,"replied":28,"resumes":18,"imported":8,"pendingScreening":3,"duplicatesSkipped":5,"alerts":0}',
 1, 1, 2, DATE_SUB(NOW(), INTERVAL 7 DAY), DATE_SUB(NOW(), INTERVAL 7 DAY));

INSERT INTO `campaign_platform_run` (`id`, `tenant_id`, `campaign_id`, `platform`, `primary_account_id`, `current_step`, `current_account_id`, `platform_job_url`, `status`, `started_at`) VALUES
(1, 1, 1, 'BOSS',   1, 'MONITOR', 1, 'https://www.zhipin.com/job/frontend-001', 'RUNNING', DATE_SUB(NOW(), INTERVAL 7 DAY)),
(2, 1, 1, 'LIEPIN', 2, 'GREET',   2, 'https://lpt.liepin.com/job/frontend-002', 'RUNNING', DATE_SUB(NOW(), INTERVAL 6 DAY));

INSERT INTO `campaign_candidate_trace` (`tenant_id`, `campaign_id`, `platform_run_id`, `job_id`, `platform`, `account_id`, `platform_user_id`, `candidate_name`, `phone`, `dedup_key`, `trace_status`, `match_score`, `candidate_id`, `resume_id`, `locked_by_account_id`, `created_at`) VALUES
(1, 1, 1, 1, 'BOSS',   1, 'boss-u-10001', '张伟',   '13900000001', 'boss:10001', 'IMPORTED',              92.50, 1,  1, 1, DATE_SUB(NOW(), INTERVAL 5 DAY)),
(1, 1, 1, 1, 'BOSS',   1, 'boss-u-10002', '李娜',   '13900000002', 'boss:10002', 'IMPORTED',              78.30, 2,  2, 1, DATE_SUB(NOW(), INTERVAL 4 DAY)),
(1, 1, 1, 1, 'BOSS',   1, 'boss-u-10007', '杨帆',   '13900000007', 'boss:10007', 'PENDING_GREET_CONFIRM', 65.40, 7,  NULL, 1, DATE_SUB(NOW(), INTERVAL 1 DAY)),
(1, 1, 1, 1, 'BOSS',   1, 'boss-u-10012', '马丽',   '13900000012', 'boss:10012', 'IMPORTED',              82.10, 12, 6, 1, DATE_SUB(NOW(), INTERVAL 3 DAY)),
(1, 1, 2, 1, 'LIEPIN', 2, 'lp-u-20003',   '王强',   '13900000003', 'liepin:20003', 'PENDING_IMPORT',    75.80, 3,  7, 2, DATE_SUB(NOW(), INTERVAL 2 DAY)),
(1, 1, 2, 1, 'LIEPIN', 2, 'lp-u-20015',   '徐明',   '13900000015', 'liepin:20015', 'SEARCHED',            85.30, 15, 8, 2, DATE_SUB(NOW(), INTERVAL 1 DAY)),
(1, 1, 1, 1, 'BOSS',   1, 'boss-u-10099', '新进候选人', '13900000099', 'boss:10099', 'GREETED',          70.00, NULL, NULL, 1, DATE_SUB(NOW(), INTERVAL 6 HOUR));

UPDATE `agent_task` SET `campaign_id` = 1, `job_title` = '高级前端工程师', `platform` = 'BOSS', `completed_count` = 8, `target_count` = 20
WHERE `id` IN (1, 2);

-- ============================================================
-- 7. 今日面试 + 需求/Offer 待办（工作台可验证）
-- ============================================================
UPDATE `interview` SET
  `scheduled_start_time` = CONCAT(CURDATE(), ' 14:00:00'),
  `scheduled_end_time` = CONCAT(CURDATE(), ' 15:00:00'),
  `status` = 'ARRANGED',
  `meeting_platform` = 'TENCENT'
WHERE `id` = 2;

UPDATE `interview` SET
  `scheduled_start_time` = CONCAT(CURDATE(), ' 10:00:00'),
  `scheduled_end_time` = CONCAT(CURDATE(), ' 11:00:00'),
  `status` = 'IN_PROGRESS',
  `meeting_platform` = 'LARK'
WHERE `id` = 3;

UPDATE `approval_instance` SET `current_approver_id` = 1, `status` = 'RUNNING' WHERE `id` = 3;
UPDATE `recruit_demand` SET `status` = 'PENDING' WHERE `id` = 3;

INSERT IGNORE INTO `notification` (`tenant_id`, `user_id`, `type`, `title`, `content`, `biz_type`, `biz_id`, `is_read`) VALUES
(1, 1, 'DEMAND_APPROVAL',  '需求待您审批',     '产品经理招聘需求（DEM-2026-003）等待审批', 'DEMAND', 3, 0),
(1, 1, 'OFFER_APPROVAL',   'Offer待您审批',    '候选人张伟 Offer（45K）等待审批',          'OFFER',  3, 0),
(1, 1, 'INTERVIEW_ARRANGE','今日复试提醒',     '张伟 14:00 复试 · 高级前端工程师',         'INTERVIEW', 2, 0),
(1, 7, 'INTERVIEW_ARRANGE','今日复试',         '您今日 14:00 需面试候选人张伟',            'INTERVIEW', 2, 0),
(1, 8, 'INTERVIEW_ARRANGE','今日初面',         '您今日 10:00 初面候选人李娜',              'INTERVIEW', 3, 0);

-- ============================================================
-- 8. 内推/猎头状态与候选人阶段对齐
-- ============================================================
UPDATE `referral` SET `status` = 'OFFER' WHERE `id` = 1 AND `candidate_id` = 5;
UPDATE `headhunter_recommendation` SET `status` = 'INTERVIEWING' WHERE `candidate_id` = 4 AND `job_id` = 3;

-- ============================================================
-- 完成。详见 RecruitOS/DEMO-VERIFICATION.md
-- ============================================================
