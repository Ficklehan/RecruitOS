-- ============================================================
-- Phase 2 演示种子：运营包 · 进化信号/建议 · 轨迹筛选 · 暂存库
-- 在 migration-v10 + linkage seed 之后执行
-- ============================================================
USE `recruit_os`;

-- 1. 租户试用友好：降低进化信号阈值（也可在租户设置中调整）
UPDATE `tenant` SET `config_json` = JSON_SET(
  COALESCE(`config_json`, '{}'),
  '$.evolutionMinSignals', 5
) WHERE `id` = 1;

-- 2. 高级前端工程师 · 已确认招人方式 v1
DELETE FROM `job_sourcing_ops_pack` WHERE `tenant_id` = 1 AND `job_id` = 1;

INSERT INTO `job_sourcing_ops_pack` (
  `id`, `tenant_id`, `job_id`, `version`, `status`, `pack_json`,
  `confirmed_by`, `confirmed_at`, `created_by`, `created_at`
) VALUES (
  1, 1, 1, 1, 'ACTIVE',
  JSON_OBJECT(
    'greetStrategy', 'SCREEN_THEN_GREET',
    'runMode', 'SEMI_AUTO',
    'platformQuotas', JSON_OBJECT('BOSS', 30, 'LIEPIN', 30),
    'screeningProfile', JSON_OBJECT('passThreshold', 60, 'twoStage', true),
    'searchKeywords', JSON_ARRAY('Vue3', 'TypeScript', '前端架构')
  ),
  2, DATE_SUB(NOW(), INTERVAL 8 DAY), 2, DATE_SUB(NOW(), INTERVAL 8 DAY)
);

UPDATE `job_sourcing_campaign` SET
  `ops_pack_id` = 1,
  `ops_pack_version` = 1
WHERE `id` = 1 AND `tenant_id` = 1;

-- 3. 轨迹两阶段筛选示例
UPDATE `campaign_candidate_trace` SET
  `screen_stage` = 'FULL_RESUME',
  `greet_strategy_applied` = 'SCREEN_THEN_GREET',
  `ops_pack_version` = 1,
  `skip_reason_json` = NULL
WHERE `tenant_id` = 1 AND `trace_status` = 'IMPORTED' AND `candidate_id` IN (1, 2, 12);

UPDATE `campaign_candidate_trace` SET
  `screen_stage` = 'CARD',
  `skip_reason_json` = JSON_OBJECT('reason', '卡片信息不匹配', 'code', 'LOW_MATCH')
WHERE `tenant_id` = 1 AND `candidate_name` = '新进候选人';

-- 4. L3 筛选信号（待处理，用于进化建议生成）
DELETE FROM `evolution_signal` WHERE `tenant_id` = 1 AND `job_id` = 1 AND `status` = 'PENDING' AND `signal_level` = 3;

INSERT INTO `evolution_signal` (
  `tenant_id`, `job_id`, `signal_level`, `confidence`, `candidate_id`,
  `source_module`, `source_event`, `campaign_id`, `tag_adjustments`,
  `learning_rate`, `status`, `created_at`
)
SELECT
  1, 1, 3, 0.82, NULL,
  'agent', IF(n % 3 = 0, 'SCREEN_SKIP', 'SCREEN_PASS'), 1,
  JSON_OBJECT('Vue3', IF(n % 2 = 0, 0.02, -0.01)),
  0.0100, 'PENDING', DATE_SUB(NOW(), INTERVAL n HOUR)
FROM (
  SELECT 1 AS n UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5
  UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9 UNION SELECT 10
  UNION SELECT 11 UNION SELECT 12 UNION SELECT 13 UNION SELECT 14 UNION SELECT 15
  UNION SELECT 16
) nums;

-- 5. 待确认进化建议（可直接在 UI 演示采纳/驳回）
DELETE FROM `evolution_proposal` WHERE `tenant_id` = 1 AND `job_id` = 1;

INSERT INTO `evolution_proposal` (
  `tenant_id`, `job_id`, `proposal_type`, `status`, `title`,
  `diff_json`, `evidence_json`, `proposed_ops_pack_json`, `base_ops_pack_version`,
  `created_at`
) VALUES (
  1, 1, 'OPS_PACK', 'PENDING',
  '筛选策略调整：及格线 60 → 63（基于 16 条信号）',
  JSON_OBJECT('passThreshold', JSON_OBJECT('from', 60, 'to', 63)),
  JSON_OBJECT('signalCount', 16, 'skipCount', 6, 'passCount', 10),
  JSON_OBJECT(
    'greetStrategy', 'SCREEN_THEN_GREET',
    'runMode', 'SEMI_AUTO',
    'platformQuotas', JSON_OBJECT('BOSS', 30, 'LIEPIN', 30),
    'screeningProfile', JSON_OBJECT('passThreshold', 63, 'twoStage', true),
    'searchKeywords', JSON_ARRAY('Vue3', 'TypeScript', '前端架构')
  ),
  1,
  DATE_SUB(NOW(), INTERVAL 2 HOUR)
);

-- 6. 渠道暂存库样例
DELETE FROM `channel_staging_candidate` WHERE `tenant_id` = 1 AND `job_id` = 1;

INSERT INTO `channel_staging_candidate` (
  `tenant_id`, `job_id`, `campaign_id`, `platform`, `platform_user_id`,
  `candidate_name`, `match_score`, `status`, `resume_text`, `created_at`
) VALUES
(1, 1, 1, 'BOSS', 'boss-stg-001', '周候选', 68.50, 'STAGED', 'Vue2 经验，正在学习 Vue3', DATE_SUB(NOW(), INTERVAL 1 DAY)),
(1, 1, 1, 'LIEPIN', 'lp-stg-002', '吴候选', 72.00, 'STAGED', '3年前端，熟悉 React', DATE_SUB(NOW(), INTERVAL 12 HOUR));

-- 7. 复试待反馈演示：将一场已完成复试设为无评价
DELETE FROM `interview_evaluation` WHERE `interview_id` = 2 AND `tenant_id` = 1;
UPDATE `interview` SET `status` = 'COMPLETED',
  `actual_end_time` = CONCAT(CURDATE(), ' 15:00:00')
WHERE `id` = 2 AND `tenant_id` = 1;

-- 完成。详见 RecruitOS/DEMO-VERIFICATION.md Phase 2
