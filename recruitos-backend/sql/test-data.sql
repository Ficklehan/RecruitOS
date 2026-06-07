-- ============================================================
-- RecruitOS 测试数据
-- 执行前请确保 init.sql 已执行完毕
-- ============================================================

USE `recruit_os`;

-- 清理已有测试数据（按外键依赖顺序）
SET FOREIGN_KEY_CHECKS = 0;
TRUNCATE TABLE `operation_log`;
TRUNCATE TABLE `notification`;
TRUNCATE TABLE `headhunter_recommendation`;
TRUNCATE TABLE `headhunter_vendor`;
TRUNCATE TABLE `referral`;
TRUNCATE TABLE `agent_behavior_log`;
TRUNCATE TABLE `agent_task`;
TRUNCATE TABLE `agent_account`;
TRUNCATE TABLE `ab_test`;
TRUNCATE TABLE `job_covariance_matrix`;
TRUNCATE TABLE `job_weight_snapshot`;
TRUNCATE TABLE `evolution_signal`;
TRUNCATE TABLE `safety_log`;
TRUNCATE TABLE `conversation_message`;
TRUNCATE TABLE `conversation`;
TRUNCATE TABLE `message_template`;
TRUNCATE TABLE `onboard_task`;
TRUNCATE TABLE `onboard`;
TRUNCATE TABLE `offer`;
TRUNCATE TABLE `interview_slot`;
TRUNCATE TABLE `interview_evaluation`;
TRUNCATE TABLE `interview`;
TRUNCATE TABLE `candidate_job`;
TRUNCATE TABLE `candidate`;
TRUNCATE TABLE `resume`;
TRUNCATE TABLE `job_position`;
TRUNCATE TABLE `approval_record`;
TRUNCATE TABLE `approval_instance`;
TRUNCATE TABLE `recruit_demand`;
SET FOREIGN_KEY_CHECKS = 1;

-- ============================================================
-- 1. 扩展组织架构
-- ============================================================
INSERT IGNORE INTO `organization` (`id`, `tenant_id`, `parent_id`, `name`, `type`, `sort_order`) VALUES
(8, 1, 1, '市场部', 'DEPARTMENT', 4),
(9, 1, 1, '销售部', 'DEPARTMENT', 5),
(10, 1, 2, '招聘团队', 'TEAM', 1),
(11, 1, 2, 'SSC团队', 'TEAM', 2);

-- ============================================================
-- 2. 测试用户 (密码统一为 Admin@123456)
-- ============================================================
INSERT IGNORE INTO `sys_user` (`id`, `tenant_id`, `username`, `password_hash`, `real_name`, `email`, `phone`, `org_id`, `status`) VALUES
(2,  1, 'zhangsan',  '$2b$10$NSEsKyZQtBEj1cUd4LMDs.0Hc0NwtqYrx2hzOlecGFe6C.xwKuIAu', '张三', 'zhangsan@recruitos.com',  '13800000001', 10, 1),
(3,  1, 'lisi',      '$2b$10$NSEsKyZQtBEj1cUd4LMDs.0Hc0NwtqYrx2hzOlecGFe6C.xwKuIAu', '李四', 'lisi@recruitos.com',      '13800000002', 10, 1),
(4,  1, 'wangwu',    '$2b$10$NSEsKyZQtBEj1cUd4LMDs.0Hc0NwtqYrx2hzOlecGFe6C.xwKuIAu', '王五', 'wangwu@recruitos.com',    '13800000003', 3,  1),
(5,  1, 'zhaoliu',   '$2b$10$NSEsKyZQtBEj1cUd4LMDs.0Hc0NwtqYrx2hzOlecGFe6C.xwKuIAu', '赵六', 'zhaoliu@recruitos.com',   '13800000004', 4,  1),
(6,  1, 'sunqi',     '$2b$10$NSEsKyZQtBEj1cUd4LMDs.0Hc0NwtqYrx2hzOlecGFe6C.xwKuIAu', '孙七', 'sunqi@recruitos.com',     '13800000005', 8,  1),
(7,  1, 'zhouba',    '$2b$10$NSEsKyZQtBEj1cUd4LMDs.0Hc0NwtqYrx2hzOlecGFe6C.xwKuIAu', '周八', 'zhouba@recruitos.com',    '13800000006', 5,  1),
(8,  1, 'wujiu',     '$2b$10$NSEsKyZQtBEj1cUd4LMDs.0Hc0NwtqYrx2hzOlecGFe6C.xwKuIAu', '吴九', 'wujiu@recruitos.com',     '13800000007', 6,  1);

-- 用户角色关联
INSERT IGNORE INTO `sys_user_role` (`tenant_id`, `user_id`, `role_id`, `scope_type`) VALUES
(1, 2, 3, 'ALL'),   -- 张三 - 招聘HR
(1, 3, 2, 'ALL'),   -- 李四 - HR负责人
(1, 4, 4, 'ALL'),   -- 王五 - 面试官(技术)
(1, 5, 4, 'ALL'),   -- 赵六 - 面试官(产品)
(1, 6, 5, 'ALL'),   -- 孙七 - 部门负责人(市场)
(1, 7, 5, 'ALL'),   -- 周八 - 部门负责人(技术)
(1, 8, 4, 'ALL');   -- 吴九 - 面试官(前端)

-- ============================================================
-- 3. 招聘需求
-- ============================================================
INSERT IGNORE INTO `recruit_demand` (`id`, `tenant_id`, `demand_no`, `title`, `org_id`, `head_count`, `job_level`, `salary_min`, `salary_max`, `urgency`, `expected_onboard_date`, `reason`, `job_duty`, `job_requirement`, `work_locations`, `reporter_id`, `initial_interviewer_ids`, `final_interviewer_ids`, `status`, `approved_head_count`, `filled_count`, `created_by`) VALUES
(1, 1, 'DEM-2026-001', '高级前端工程师', 3, 2, 'P7', 30000, 50000, 'URGENT', '2026-07-15', 'EXPANSION',
 '1. 负责公司核心产品的前端架构设计与开发\n2. 推动前端工程化建设，提升团队开发效率\n3. 参与技术选型与方案评审\n4. 指导初中级工程师成长',
 '1. 本科及以上学历，计算机相关专业\n2. 5年以上前端开发经验\n3. 精通 Vue 3 / React，熟悉 TypeScript\n4. 有大型项目架构经验优先\n5. 良好的沟通能力和团队合作精神',
 '["北京", "上海"]', 7, '[4, 8]', '[7]', 'RECRUITING', 2, 0, 2),

(2, 1, 'DEM-2026-002', 'Java后端工程师', 3, 3, 'P6', 25000, 40000, 'NORMAL', '2026-08-01', 'EXPANSION',
 '1. 负责后端微服务的设计与开发\n2. 参与系统架构优化\n3. 编写技术文档和单元测试',
 '1. 本科及以上学历\n2. 3年以上Java开发经验\n3. 熟悉Spring Boot/MyBatis\n4. 有微服务经验优先',
 '["北京"]', 7, '[4]', '[7]', 'APPROVED', 3, 0, 2),

(3, 1, 'DEM-2026-003', '产品经理', 4, 1, 'P7', 35000, 55000, 'URGENT', '2026-06-30', 'REPLACEMENT',
 '1. 负责产品规划和需求分析\n2. 输出PRD和原型设计\n3. 跟进产品开发和上线',
 '1. 本科及以上学历\n2. 5年以上B端产品经验\n3. 有HR SaaS产品经验优先',
 '["北京"]', 5, '[5]', '[3]', 'PENDING', 0, 0, 3),

(4, 1, 'DEM-2026-004', '市场运营专员', 8, 2, 'P5', 15000, 25000, 'NORMAL', '2026-09-01', 'NEW',
 '1. 负责品牌推广和内容运营\n2. 策划线上线下活动\n3. 分析运营数据并优化策略',
 '1. 本科及以上学历\n2. 2年以上运营经验\n3. 有ToB运营经验优先',
 '["上海"]', 6, '[]', '[]', 'DRAFT', 0, 0, 6),

(5, 1, 'DEM-2026-005', '数据分析师', 3, 1, 'P6', 28000, 42000, 'CRITICAL', '2026-06-15', 'EXPANSION',
 '1. 负责招聘数据分析和报表\n2. 搭建数据指标体系\n3. 输出数据洞察报告',
 '1. 本科及以上学历\n2. 3年以上数据分析经验\n3. 熟悉SQL和BI工具',
 '["北京"]', 7, '[4]', '[7]', 'COMPLETED', 1, 1, 2),

(6, 1, 'DEM-2026-006', 'UI设计师', 4, 1, 'P6', 22000, 35000, 'NORMAL', '2026-08-15', 'EXPANSION',
 '1. 负责产品UI设计\n2. 建立设计规范和组件库\n3. 配合前端还原设计稿',
 '1. 本科及以上学历\n2. 3年以上UI设计经验\n3. 熟悉Figma/Sketch',
 '["杭州"]', 5, '[5]', '[]', 'REJECTED', 0, 0, 3);

-- ============================================================
-- 4. 审批实例与记录
-- ============================================================
INSERT IGNORE INTO `approval_instance` (`id`, `tenant_id`, `biz_type`, `biz_id`, `status`, `current_node`, `current_approver_id`) VALUES
(1, 1, 'DEMAND', 1, 'APPROVED', NULL, NULL),
(2, 1, 'DEMAND', 2, 'APPROVED', NULL, NULL),
(3, 1, 'DEMAND', 3, 'RUNNING', 'HR_MANAGER', 3),
(4, 1, 'DEMAND', 6, 'REJECTED', NULL, NULL);

INSERT IGNORE INTO `approval_record` (`tenant_id`, `instance_id`, `node_name`, `approver_id`, `action`, `comment`) VALUES
(1, 1, 'HRBP审批', 3, 'APPROVE', '同意，业务扩张确实需要补充前端人手'),
(1, 1, 'HR负责人审批', 3, 'APPROVE', '预算已确认，同意招聘'),
(1, 2, 'HRBP审批', 3, 'APPROVE', '同意'),
(1, 3, '提交申请', 3, 'APPROVE', '提交审批'),
(1, 4, 'HRBP审批', 3, 'REJECT', '当前季度预算已满，建议下季度再提');

-- ============================================================
-- 5. 岗位
-- ============================================================
INSERT IGNORE INTO `job_position` (`id`, `tenant_id`, `demand_id`, `job_no`, `title`, `jd_text`, `tags`, `head_count`, `filled_count`, `status`) VALUES
(1, 1, 1, 'JOB-2026-001', '高级前端工程师',
 '岗位职责：\n1. 负责公司核心产品的前端架构设计与开发\n2. 推动前端工程化建设\n\n任职要求：\n1. 5年以上前端经验\n2. 精通Vue3/React',
 '[{"tag":"Vue3","match_weight":0.35,"search_weight":0.30,"decision_weight":0.25,"locked":false},{"tag":"TypeScript","match_weight":0.25,"search_weight":0.20,"decision_weight":0.20,"locked":false},{"tag":"前端架构","match_weight":0.20,"search_weight":0.15,"decision_weight":0.25,"locked":false},{"tag":"React","match_weight":0.10,"search_weight":0.15,"decision_weight":0.15,"locked":false},{"tag":"工程化","match_weight":0.10,"search_weight":0.20,"decision_weight":0.15,"locked":false}]',
 2, 0, 'ACTIVE'),

(2, 1, 2, 'JOB-2026-002', 'Java后端工程师',
 '岗位职责：\n1. 负责后端微服务开发\n2. 参与架构优化\n\n任职要求：\n1. 3年以上Java经验\n2. 熟悉Spring Boot',
 '[{"tag":"Java","match_weight":0.30,"search_weight":0.30,"decision_weight":0.25,"locked":false},{"tag":"Spring Boot","match_weight":0.25,"search_weight":0.25,"decision_weight":0.25,"locked":false},{"tag":"微服务","match_weight":0.20,"search_weight":0.20,"decision_weight":0.25,"locked":false},{"tag":"MySQL","match_weight":0.15,"search_weight":0.15,"decision_weight":0.15,"locked":false},{"tag":"Redis","match_weight":0.10,"search_weight":0.10,"decision_weight":0.10,"locked":false}]',
 3, 0, 'ACTIVE'),

(3, 1, 3, 'JOB-2026-003', '产品经理',
 '岗位职责：\n1. 负责产品规划\n2. 输出PRD\n\n任职要求：\n1. 5年以上B端产品经验\n2. 有HR SaaS经验优先',
 '[{"tag":"B端产品","match_weight":0.35,"search_weight":0.30,"decision_weight":0.30,"locked":false},{"tag":"需求分析","match_weight":0.25,"search_weight":0.25,"decision_weight":0.25,"locked":false},{"tag":"HR SaaS","match_weight":0.20,"search_weight":0.20,"decision_weight":0.25,"locked":true},{"tag":"项目管理","match_weight":0.20,"search_weight":0.25,"decision_weight":0.20,"locked":false}]',
 1, 0, 'DRAFT'),

(4, 1, 5, 'JOB-2026-004', '数据分析师',
 '岗位职责：\n1. 负责招聘数据分析\n2. 搭建指标体系\n\n任职要求：\n1. 3年以上数据分析经验\n2. 熟悉SQL和BI工具',
 '[{"tag":"SQL","match_weight":0.30,"search_weight":0.30,"decision_weight":0.25,"locked":false},{"tag":"数据分析","match_weight":0.30,"search_weight":0.25,"decision_weight":0.30,"locked":false},{"tag":"BI工具","match_weight":0.20,"search_weight":0.20,"decision_weight":0.20,"locked":false},{"tag":"Python","match_weight":0.20,"search_weight":0.25,"decision_weight":0.25,"locked":false}]',
 1, 1, 'CLOSED'),

(5, 1, 1, 'JOB-2026-005', '前端开发工程师(中级)',
 '岗位职责：\n1. 负责业务页面开发\n2. 参与组件库建设\n\n任职要求：\n1. 3年以上前端经验\n2. 熟悉Vue3',
 '[{"tag":"Vue3","match_weight":0.35,"search_weight":0.35,"decision_weight":0.30,"locked":false},{"tag":"JavaScript","match_weight":0.30,"search_weight":0.30,"decision_weight":0.25,"locked":false},{"tag":"CSS","match_weight":0.20,"search_weight":0.20,"decision_weight":0.25,"locked":false},{"tag":"Webpack/Vite","match_weight":0.15,"search_weight":0.15,"decision_weight":0.20,"locked":false}]',
 1, 0, 'ACTIVE');

-- ============================================================
-- 6. 候选人
-- ============================================================
INSERT IGNORE INTO `candidate` (`id`, `tenant_id`, `name`, `phone`, `email`, `gender`, `current_company`, `current_title`, `work_years`, `education`, `school`, `expected_salary`, `source`, `source_detail`, `status`, `remark`) VALUES
(1,  1, '张伟',   '13900000001', 'zhangwei@test.com',   'MALE',   '阿里巴巴',   '高级前端工程师', 6, 'MASTER',  '北京大学',   45000, 'PLATFORM',   'BOSS直聘',  'INTERVIEWING', '技术扎实，有大厂经验'),
(2,  1, '李娜',   '13900000002', 'lina@test.com',       'FEMALE', '腾讯',       '前端工程师',     4, 'BACHELOR','浙江大学',   35000, 'PLATFORM',   '猎聘',      'SCREENING',    'Vue3经验丰富'),
(3,  1, '王强',   '13900000003', 'wangqiang@test.com',  'MALE',   '字节跳动',   'Java工程师',     5, 'BACHELOR','清华大学',   40000, 'PLATFORM',   '拉勾',      'NEW',          '微服务架构经验'),
(4,  1, '赵敏',   '13900000004', 'zhaomin@test.com',    'FEMALE', '美团',       '产品经理',       7, 'MASTER',  '复旦大学',   50000, 'HEADHUNTER', '猎头推荐',  'INTERVIEWING', 'B端产品经验丰富'),
(5,  1, '陈浩',   '13900000005', 'chenhao@test.com',    'MALE',   '百度',       '高级前端',       8, 'BACHELOR','武汉大学',   55000, 'REFERRAL',   '张三内推',  'OFFER',        '架构能力突出'),
(6,  1, '刘洋',   '13900000006', 'liuyang@test.com',    'MALE',   '京东',       '数据分析师',     4, 'MASTER',  '中科院',     38000, 'PLATFORM',   'BOSS直聘',  'ONBOARD',      '数据分析能力强'),
(7,  1, '杨帆',   '13900000007', 'yangfan@test.com',    'MALE',   '小米',       '前端开发',       3, 'BACHELOR','北邮',       28000, 'DIRECT',     '官网投递',  'SCREENING',    '潜力不错'),
(8,  1, '周婷',   '13900000008', 'zhouting@test.com',   'FEMALE', '网易',       'Java开发',       5, 'BACHELOR','南开大学',   38000, 'PLATFORM',   '猎聘',      'NEW',          ''),
(9,  1, '吴磊',   '13900000009', 'wulei@test.com',      'MALE',   '华为',       '前端架构师',     10,'MASTER',  '中科大',     60000, 'HEADHUNTER', '科锐国际',  'POOL',         '资深前端'),
(10, 1, '郑雪',   '13900000010', 'zhengxue@test.com',   'FEMALE', '滴滴',       '产品经理',       4, 'BACHELOR','人大',       35000, 'REFERRAL',   '李四内推',  'SCREENING',    ''),
(11, 1, '孙超',   '13900000011', 'sunchao@test.com',    'MALE',   '快手',       'Java高级',       6, 'BACHELOR','北理工',     42000, 'PLATFORM',   'BOSS直聘',  'NEW',          ''),
(12, 1, '马丽',   '13900000012', 'mali@test.com',       'FEMALE', '蚂蚁集团',   '前端工程师',     5, 'MASTER',  '上交',       40000, 'PLATFORM',   '猎聘',      'INTERVIEWING', 'React经验丰富'),
(13, 1, '黄涛',   '13900000013', 'huangtao@test.com',   'MALE',   '拼多多',     '数据工程师',     3, 'BACHELOR','华科',       30000, 'DIRECT',     '官网投递',  'NEW',          ''),
(14, 1, '林琳',   '13900000014', 'linlin@test.com',     'FEMALE', '字节跳动',   'UI设计师',       5, 'BACHELOR','央美',       32000, 'PLATFORM',   '拉勾',      'POOL',         '设计能力优秀'),
(15, 1, '徐明',   '13900000015', 'xuming@test.com',     'MALE',   '腾讯',       '后端开发',       4, 'MASTER',  '北航',       38000, 'REFERRAL',   '王五内推',  'SCREENING',    '微服务经验丰富');

-- ============================================================
-- 7. 候选人-岗位关联 (含AI匹配分数)
-- ============================================================
INSERT IGNORE INTO `candidate_job` (`tenant_id`, `candidate_id`, `job_id`, `match_score`, `screening_status`, `screener_id`, `screener_comment`) VALUES
(1, 1, 1, 92.50, 'PASSED', 2, '技术面试通过，推荐复试'),
(1, 2, 1, 78.30, 'PASSED', 2, 'Vue3经验丰富'),
(1, 5, 1, 95.00, 'PASSED', 2, '资深前端，直接终面'),
(1, 7, 1, 65.40, 'PENDING', NULL, NULL),
(1, 9, 1, 88.60, 'POOL', 2, '人才储备'),
(1, 12, 1, 82.10, 'PASSED', 2, 'React转Vue可行'),
(1, 3, 2, 75.80, 'PENDING', NULL, NULL),
(1, 8, 2, 80.20, 'PENDING', NULL, NULL),
(1, 11, 2, 70.50, 'PENDING', NULL, NULL),
(1, 15, 2, 85.30, 'PASSED', 2, '微服务经验丰富'),
(1, 4, 3, 88.00, 'PASSED', 2, '产品经验匹配'),
(1, 10, 3, 72.50, 'PENDING', NULL, NULL),
(1, 6, 4, 90.00, 'PASSED', 2, '已入职'),
(1, 13, 4, 68.00, 'PENDING', NULL, NULL),
(1, 14, 1, 45.00, 'REJECTED', 2, '设计师不适合前端岗');

-- ============================================================
-- 8. 面试
-- ============================================================
INSERT IGNORE INTO `interview` (`id`, `tenant_id`, `candidate_id`, `job_id`, `demand_id`, `round`, `round_seq`, `interviewer_id`, `scheduled_start_time`, `scheduled_end_time`, `actual_start_time`, `actual_end_time`, `format`, `location`, `status`) VALUES
-- 张伟 面试高级前端 - 初面已完成
(1,  1, 1, 1, 1, 'INITIAL', 1, 4, '2026-06-02 10:00:00', '2026-06-02 11:00:00', '2026-06-02 10:05:00', '2026-06-02 10:55:00', 'ONLINE', '腾讯会议', 'COMPLETED'),
-- 张伟 面试高级前端 - 复试已安排
(2,  1, 1, 1, 1, 'FINAL',   1, 7, '2026-06-10 14:00:00', '2026-06-10 15:00:00', NULL, NULL, 'OFFLINE', '北京总部A座3楼会议室', 'ARRANGED'),
-- 李娜 面试高级前端 - 初面进行中
(3,  1, 2, 1, 1, 'INITIAL', 1, 8, '2026-06-07 10:00:00', '2026-06-07 11:00:00', '2026-06-07 10:03:00', NULL, 'ONLINE', '飞书会议', 'IN_PROGRESS'),
-- 陈浩 面试高级前端 - 初面已通过，复试已通过
(4,  1, 5, 1, 1, 'INITIAL', 1, 4, '2026-05-28 10:00:00', '2026-05-28 11:00:00', '2026-05-28 10:00:00', '2026-05-28 10:50:00', 'ONLINE', '腾讯会议', 'COMPLETED'),
(5,  1, 5, 1, 1, 'FINAL',   1, 7, '2026-06-03 14:00:00', '2026-06-03 15:00:00', '2026-06-03 14:00:00', '2026-06-03 14:50:00', 'OFFLINE', '北京总部A座3楼会议室', 'COMPLETED'),
-- 赵敏 面试产品经理 - 初面已通过
(6,  1, 4, 3, 3, 'INITIAL', 1, 5, '2026-06-05 10:00:00', '2026-06-05 11:30:00', '2026-06-05 10:05:00', '2026-06-05 11:20:00', 'OFFLINE', '北京总部B座2楼会议室', 'COMPLETED'),
-- 刘洋 面试数据分析师 - 全部通过
(7,  1, 6, 4, 5, 'INITIAL', 1, 4, '2026-05-20 10:00:00', '2026-05-20 11:00:00', '2026-05-20 10:00:00', '2026-05-20 10:50:00', 'ONLINE', '腾讯会议', 'COMPLETED'),
(8,  1, 6, 4, 5, 'FINAL',   1, 7, '2026-05-25 14:00:00', '2026-05-25 15:00:00', '2026-05-25 14:00:00', '2026-05-25 14:45:00', 'OFFLINE', '北京总部A座3楼会议室', 'COMPLETED'),
-- 杨帆 面试高级前端 - 待安排
(9,  1, 7, 1, 1, 'INITIAL', 1, 8, NULL, NULL, NULL, NULL, 'ONLINE', NULL, 'PENDING_ARRANGE'),
-- 马丽 面试高级前端 - 初面待确认
(10, 1, 12, 1, 1, 'INITIAL', 1, 4, '2026-06-12 10:00:00', '2026-06-12 11:00:00', NULL, NULL, 'ONLINE', '腾讯会议', 'ARRANGED');

-- 面试时间偏好
INSERT IGNORE INTO `interview_slot` (`tenant_id`, `interview_id`, `slot_start`, `slot_end`, `is_selected`) VALUES
(1, 2, '2026-06-10 14:00:00', '2026-06-10 15:00:00', 1),
(1, 2, '2026-06-11 10:00:00', '2026-06-11 11:00:00', 0),
(1, 10, '2026-06-12 10:00:00', '2026-06-12 11:00:00', 1),
(1, 10, '2026-06-13 14:00:00', '2026-06-13 15:00:00', 0);

-- ============================================================
-- 9. 面试评价
-- ============================================================
INSERT IGNORE INTO `interview_evaluation` (`tenant_id`, `interview_id`, `round`, `decision`, `overall_score`, `dimensions`, `comment`, `submitted_at`) VALUES
-- 张伟初面评价
(1, 1, 'INITIAL', 'PASS', 88,
 '[{"name":"技术能力","score":90,"weight":0.40},{"name":"沟通表达","score":85,"weight":0.20},{"name":"项目经验","score":92,"weight":0.25},{"name":"学习能力","score":80,"weight":0.15}]',
 '技术功底扎实，Vue3源码理解深入，有大型项目架构经验。沟通清晰，推荐复试。',
 '2026-06-02 11:00:00'),
-- 陈浩初面评价
(1, 4, 'INITIAL', 'PASS', 92,
 '[{"name":"技术能力","score":95,"weight":0.40},{"name":"沟通表达","score":88,"weight":0.20},{"name":"项目经验","score":93,"weight":0.25},{"name":"学习能力","score":90,"weight":0.15}]',
 '资深前端，技术全面，有团队管理经验。强烈推荐。',
 '2026-05-28 11:00:00'),
-- 陈浩复试评价
(1, 5, 'FINAL', 'PASS', 90,
 '[{"name":"技术深度","score":92,"weight":0.35},{"name":"架构设计","score":88,"weight":0.30},{"name":"团队协作","score":90,"weight":0.20},{"name":"文化匹配","score":88,"weight":0.15}]',
 '技术能力优秀，架构思维清晰，团队协作意识强。建议录用。',
 '2026-06-03 15:00:00'),
-- 赵敏初面评价
(1, 6, 'INITIAL', 'PASS', 85,
 '[{"name":"产品思维","score":88,"weight":0.35},{"name":"需求分析","score":82,"weight":0.25},{"name":"行业经验","score":86,"weight":0.25},{"name":"沟通能力","score":85,"weight":0.15}]',
 'B端产品经验丰富，对HR SaaS有深入理解。建议安排与技术负责人复试。',
 '2026-06-05 11:30:00'),
-- 刘洋初面评价
(1, 7, 'INITIAL', 'PASS', 86,
 '[{"name":"数据分析","score":88,"weight":0.40},{"name":"SQL能力","score":85,"weight":0.25},{"name":"业务理解","score":82,"weight":0.20},{"name":"表达能力","score":88,"weight":0.15}]',
 '数据分析基础扎实，SQL熟练，对招聘业务有理解。',
 '2026-05-20 11:00:00'),
-- 刘洋复试评价
(1, 8, 'FINAL', 'PASS', 84,
 '[{"name":"分析深度","score":85,"weight":0.35},{"name":"工具熟练度","score":82,"weight":0.25},{"name":"洞察力","score":86,"weight":0.25},{"name":"协作能力","score":83,"weight":0.15}]',
 '综合能力不错，建议录用。',
 '2026-05-25 15:00:00');

-- ============================================================
-- 10. Offer
-- ============================================================
INSERT IGNORE INTO `offer` (`id`, `tenant_id`, `candidate_id`, `job_id`, `interview_id`, `salary`, `bonus`, `level`, `onboard_date`, `status`, `bg_check_status`, `created_by`, `created_at`) VALUES
(1, 1, 5, 1, 5, 52000, 5000, 'P7', '2026-07-01', 'SENT', 'PASSED', 2, '2026-06-04 10:00:00'),
(2, 1, 6, 4, 8, 36000, 3000, 'P6', '2026-06-16', 'ACCEPTED', 'PASSED', 2, '2026-05-26 10:00:00'),
(3, 1, 1, 1, 1, 45000, NULL, 'P7', '2026-07-15', 'PENDING_APPROVAL', 'PENDING', 2, '2026-06-06 10:00:00'),
(4, 1, 4, 3, 6, 48000, 5000, 'P7', '2026-07-01', 'DRAFT', NULL, 2, '2026-06-06 14:00:00');

-- ============================================================
-- 11. 入职
-- ============================================================
INSERT IGNORE INTO `onboard` (`id`, `tenant_id`, `candidate_id`, `offer_id`, `onboard_date`, `status`, `remark`) VALUES
(1, 1, 6, 2, '2026-06-16', 'IN_PROGRESS', '已发送入职通知'),
(2, 1, 5, 1, '2026-07-01', 'PENDING', '待候选人确认');

INSERT IGNORE INTO `onboard_task` (`tenant_id`, `onboard_id`, `task_type`, `task_name`, `assignee_type`, `due_date`, `status`) VALUES
-- 刘洋的入职任务
(1, 1, 'UPLOAD_DOC',   '上传身份证复印件',       'CANDIDATE', '2026-06-10', 'DONE'),
(1, 1, 'UPLOAD_DOC',   '上传学历证书',           'CANDIDATE', '2026-06-10', 'DONE'),
(1, 1, 'CONTRACT',     '签订劳动合同',           'HR',        '2026-06-14', 'IN_PROGRESS'),
(1, 1, 'IT_EQUIP',     '准备办公电脑和账号',       'IT',        '2026-06-15', 'PENDING'),
(1, 1, 'BUDDY_PLAN',   '指定入职导师',           'MANAGER',   '2026-06-14', 'PENDING'),
(1, 1, 'HR_REVIEW',    'HR入职面谈',            'HR',        '2026-06-16', 'PENDING'),
(1, 1, 'ACCOUNT_CREATE','创建企业邮箱和系统账号',   'IT',        '2026-06-15', 'PENDING'),
-- 陈浩的入职任务
(1, 2, 'UPLOAD_DOC',   '上传身份证复印件',       'CANDIDATE', '2026-06-25', 'PENDING'),
(1, 2, 'CONTRACT',     '签订劳动合同',           'HR',        '2026-06-28', 'PENDING'),
(1, 2, 'IT_EQUIP',     '准备办公电脑和账号',       'IT',        '2026-06-30', 'PENDING');

-- ============================================================
-- 12. 话术模板
-- ============================================================
INSERT IGNORE INTO `message_template` (`id`, `tenant_id`, `scene`, `candidate_type`, `title`, `content`, `send_count`, `reply_count`, `resume_count`, `hire_count`, `is_ab_test`, `ab_group`) VALUES
(1, 1, 'INITIAL', 'TECH', '技术岗初次打招呼-版本A',
 'Hi {{name}}，我是{{company}}的招聘负责人。看到您在{{platform}}上的简历，我们正在招聘{{job}}岗位，您的经验很匹配。方便聊聊吗？',
 156, 42, 28, 5, 1, 'A'),
(2, 1, 'INITIAL', 'TECH', '技术岗初次打招呼-版本B',
 '{{name}}您好！{{company}}正在寻找{{job}}方向的优秀人才，您的背景非常吸引我们。不知道您是否对新的职业机会感兴趣？期待您的回复。',
 148, 38, 22, 3, 1, 'B'),
(3, 1, 'CHASE', 'ALL', '跟进话术-3天未回复',
 '您好{{name}}，之前给您发的消息不知道是否看到？我们这个岗位确实很适合您的背景，薪资范围{{salary}}，希望能有机会详聊。',
 89, 18, 12, 2, 0, NULL),
(4, 1, 'RESUME_REQ', 'ALL', '索要简历话术',
 '{{name}}您好，经过初步沟通，想请您发一份最新简历给我们，方便我们推进后续流程。邮箱：hr@{{company}}.com，谢谢！',
 65, 55, 55, 0, 0, NULL),
(5, 1, 'INTERVIEW_INVITE', 'ALL', '面试邀请',
 '{{name}}您好，恭喜您通过了我们的初筛！诚邀您参加{{job}}岗位的面试，时间：{{time}}，形式：{{format}}。请确认是否方便？',
 32, 30, 0, 0, 0, NULL);

-- ============================================================
-- 13. 对话记录
-- ============================================================
INSERT IGNORE INTO `conversation` (`id`, `tenant_id`, `candidate_id`, `job_id`, `agent_account_id`, `platform`, `status`, `last_message_at`, `message_count`) VALUES
(1, 1, 1, 1, 1, 'BOSS',    'ACTIVE',    '2026-06-05 15:30:00', 8),
(2, 1, 2, 1, 1, 'BOSS',    'ACTIVE',    '2026-06-06 10:00:00', 4),
(3, 1, 3, 2, 2, 'LIEPIN',  'COMPLETED', '2026-06-04 16:00:00', 6),
(4, 1, 4, 3, 3, 'MAIMAI',  'ACTIVE',    '2026-06-05 11:00:00', 5),
(5, 1, 5, 1, 1, 'BOSS',    'COMPLETED', '2026-05-28 09:00:00', 10);

INSERT IGNORE INTO `conversation_message` (`tenant_id`, `conversation_id`, `direction`, `sender_type`, `content`, `sent_at`) VALUES
-- 对话1：与张伟的BOSS沟通
(1, 1, 'OUT', 'AGENT',     'Hi张伟，看到您的简历，我们正在招聘高级前端工程师，方便聊聊吗？', '2026-06-03 10:00:00'),
(1, 1, 'IN',  'CANDIDATE', '您好，方便的，请介绍一下岗位。', '2026-06-03 10:30:00'),
(1, 1, 'OUT', 'AGENT',     '这个岗位负责核心产品前端架构，薪资30-50K，团队技术氛围好。', '2026-06-03 10:35:00'),
(1, 1, 'IN',  'CANDIDATE', '听起来不错，我目前在阿里做类似的工作，想了解一下技术栈。', '2026-06-03 14:00:00'),
(1, 1, 'OUT', 'AGENT',     '主要是Vue3 + TypeScript，也有部分React项目。我们很重视工程化。', '2026-06-03 14:10:00'),
(1, 1, 'IN',  'CANDIDATE', '很好，我对Vue3比较熟悉。方便发一份简历吗？', '2026-06-04 09:00:00'),
(1, 1, 'OUT', 'AGENT',     '好的，请发到 hr@recruitos.com，我们会尽快安排面试。', '2026-06-04 09:10:00'),
(1, 1, 'IN',  'CANDIDATE', '已发送，请查收。', '2026-06-05 15:30:00'),
-- 对话2：与李娜的沟通
(1, 2, 'OUT', 'AGENT',     '李娜您好，我们正在招聘前端工程师，您的经验很匹配。', '2026-06-05 10:00:00'),
(1, 2, 'IN',  'CANDIDATE', '您好，请问是哪个公司？', '2026-06-05 14:00:00'),
(1, 2, 'OUT', 'AGENT',     '是RecruitOS，一家HR SaaS公司，目前在快速发展阶段。', '2026-06-05 14:10:00'),
(1, 2, 'IN',  'CANDIDATE', '了解，我考虑一下。', '2026-06-06 10:00:00');

-- ============================================================
-- 14. 安全审查日志
-- ============================================================
INSERT IGNORE INTO `safety_log` (`tenant_id`, `message_id`, `check_type`, `risk_level`, `action`, `detail`) VALUES
(1, NULL, 'SALARY',           'MEDIUM', 'PASS',   '{"reason":"薪资在合理范围内","detected_amount":"45000"}'),
(1, NULL, 'SENSITIVE_TOPIC',  'LOW',    'PASS',   '{"reason":"未检测到敏感话题"}'),
(1, NULL, 'PROMPT_INJECTION', 'LOW',    'PASS',   '{"reason":"未检测到注入攻击"}');

-- ============================================================
-- 15. 进化引擎数据
-- ============================================================
INSERT IGNORE INTO `evolution_signal` (`tenant_id`, `job_id`, `signal_level`, `confidence`, `candidate_id`, `tag_adjustments`, `learning_rate`, `status`, `ab_group`) VALUES
(1, 1, 3, 0.85, 1,  '{"Vue3":0.05,"TypeScript":0.03}', 0.0100, 'APPLIED', 'TREATMENT'),
(1, 1, 2, 0.72, 5,  '{"前端架构":0.08,"Vue3":0.02}',   0.0080, 'APPLIED', 'TREATMENT'),
(1, 1, 4, 0.90, 12, '{"React":0.10,"Vue3":-0.05}',     0.0120, 'PENDING', 'CONTROL'),
(1, 2, 3, 0.78, 3,  '{"Java":0.05,"微服务":0.04}',     0.0090, 'APPLIED', 'TREATMENT'),
(1, 2, 2, 0.65, 15, '{"Spring Boot":0.06,"MySQL":0.03}', 0.0070, 'PENDING', 'TREATMENT');

INSERT IGNORE INTO `job_weight_snapshot` (`tenant_id`, `job_id`, `snapshot_type`, `tags_snapshot`, `health_score`, `signal_id`) VALUES
(1, 1, 'INITIAL',   '[{"tag":"Vue3","match_weight":0.35,"search_weight":0.30,"decision_weight":0.25},{"tag":"TypeScript","match_weight":0.25,"search_weight":0.20,"decision_weight":0.20},{"tag":"前端架构","match_weight":0.20,"search_weight":0.15,"decision_weight":0.25}]', 85.00, NULL),
(1, 1, 'EVOLUTION', '[{"tag":"Vue3","match_weight":0.40,"search_weight":0.33,"decision_weight":0.28},{"tag":"TypeScript","match_weight":0.28,"search_weight":0.23,"decision_weight":0.23},{"tag":"前端架构","match_weight":0.28,"search_weight":0.15,"decision_weight":0.25}]', 88.50, 1),
(1, 2, 'INITIAL',   '[{"tag":"Java","match_weight":0.30,"search_weight":0.30,"decision_weight":0.25},{"tag":"Spring Boot","match_weight":0.25,"search_weight":0.25,"decision_weight":0.25},{"tag":"微服务","match_weight":0.20,"search_weight":0.20,"decision_weight":0.25}]', 82.00, NULL);

INSERT IGNORE INTO `ab_test` (`id`, `tenant_id`, `job_id`, `test_name`, `description`, `treatment_snapshot_id`, `control_snapshot_id`, `traffic_split`, `start_date`, `status`) VALUES
(1, 1, 1, 'Vue3权重提升实验', '测试提升Vue3权重对匹配质量的影响', 2, 1, 0.80, '2026-06-01 00:00:00', 'RUNNING'),
(2, 1, 2, '微服务权重实验', '测试微服务标签权重优化效果', 3, 1, 0.70, '2026-06-05 00:00:00', 'RUNNING');

-- ============================================================
-- 16. Agent平台账号与任务
-- ============================================================
INSERT IGNORE INTO `agent_account` (`id`, `tenant_id`, `platform`, `account_name`, `encrypted_credential`, `health_score`, `daily_msg_count`, `daily_search_count`, `daily_msg_limit`, `daily_search_limit`, `status`, `last_active_at`) VALUES
(1, 1, 'BOSS',   'RecruitOS招聘官-张三', 'encrypted_boss_credential_placeholder', 95.00, 23, 8, 50, 10, 'ACTIVE', '2026-06-07 09:00:00'),
(2, 1, 'LIEPIN', 'RecruitOS猎聘企业版',  'encrypted_liepin_credential_placeholder', 88.50, 15, 5, 30, 8,  'ACTIVE', '2026-06-06 18:00:00'),
(3, 1, 'MAIMAI', 'RecruitOS脉脉官方号',  'encrypted_maimai_credential_placeholder', 72.00, 8,  3, 20, 5,  'DEGRADED', '2026-06-05 12:00:00');

INSERT IGNORE INTO `agent_task` (`id`, `tenant_id`, `agent_account_id`, `job_id`, `task_type`, `status`, `started_at`, `completed_at`) VALUES
(1, 1, 1, 1, 'SEARCH',    'RUNNING',    '2026-06-07 09:00:00', NULL),
(2, 1, 1, 1, 'MESSAGE',   'RUNNING',    '2026-06-07 09:05:00', NULL),
(3, 1, 2, 2, 'SEARCH',    'COMPLETED',  '2026-06-06 10:00:00', '2026-06-06 10:30:00'),
(4, 1, 2, 2, 'RESUME_REQ','COMPLETED',  '2026-06-06 11:00:00', '2026-06-06 11:15:00'),
(5, 1, 3, 3, 'MESSAGE',   'PENDING',    NULL, NULL);

INSERT IGNORE INTO `agent_behavior_log` (`tenant_id`, `agent_account_id`, `action`, `target_url`, `duration_ms`, `success`) VALUES
(1, 1, 'LOGIN',       'https://www.zhipin.com',               2300, 1),
(1, 1, 'SEARCH',      'https://www.zhipin.com/search?job=前端', 1500, 1),
(1, 1, 'VIEW_PROFILE','https://www.zhipin.com/user/12345',     800,  1),
(1, 1, 'SEND_MSG',    'https://www.zhipin.com/chat/12345',     1200, 1),
(1, 1, 'WAIT',        NULL,                                     30000,1),
(1, 2, 'LOGIN',       'https://www.liepin.com',                2100, 1),
(1, 2, 'SEARCH',      'https://www.liepin.com/search?job=Java',1800, 1),
(1, 2, 'VIEW_PROFILE','https://www.liepin.com/user/67890',     900,  1),
(1, 2, 'SEND_MSG',    'https://www.liepin.com/chat/67890',     1100, 1),
(1, 2, 'ERROR',       'https://www.liepin.com/chat/67890',     500,  0);

-- ============================================================
-- 17. 内推
-- ============================================================
INSERT IGNORE INTO `referral` (`id`, `tenant_id`, `referrer_id`, `candidate_id`, `job_id`, `status`, `reward_amount`, `reward_status`, `remark`) VALUES
(1, 1, 2, 5,  1, 'HIRED',        8000, 'APPROVED', '内推成功，推荐奖金已审批'),
(2, 1, 3, 10, 3, 'INTERVIEWING', 5000, 'PENDING',  '候选人正在面试中'),
(3, 1, 4, 15, 2, 'SCREENING',    5000, NULL,       '等待筛选'),
(4, 1, 8, 3,  2, 'SUBMITTED',    5000, NULL,       '刚提交');

-- ============================================================
-- 18. 猎头
-- ============================================================
INSERT IGNORE INTO `headhunter_vendor` (`id`, `tenant_id`, `name`, `contact_name`, `contact_phone`, `contact_email`, `fee_rate`, `contract_start`, `contract_end`, `total_recommended`, `total_hired`) VALUES
(1, 1, '科锐国际',   '王经理', '13700000001', 'wang@careerintl.com',    0.2000, '2026-01-01', '2026-12-31', 12, 3),
(2, 1, '猎聘网',     '李经理', '13700000002', 'li@liepin.com',          0.1800, '2026-01-01', '2026-12-31', 8,  2),
(3, 1, '万宝盛华',   '张经理', '13700000003', 'zhang@manpower.com.cn',  0.2200, '2026-03-01', '2027-02-28', 5,  1);

INSERT IGNORE INTO `headhunter_recommendation` (`tenant_id`, `vendor_id`, `candidate_id`, `job_id`, `status`, `fee_amount`, `fee_status`) VALUES
(1, 1, 9,  1, 'HIRED',       12000, 'PAID'),
(1, 1, 4,  3, 'INTERVIEWING', NULL,  NULL),
(1, 2, 14, 1, 'REJECTED',    NULL,  NULL),
(1, 2, 3,  2, 'SCREENING',   NULL,  NULL),
(1, 3, 9,  2, 'SUBMITTED',   NULL,  NULL);

-- ============================================================
-- 19. 通知消息
-- ============================================================
INSERT IGNORE INTO `notification` (`tenant_id`, `user_id`, `type`, `title`, `content`, `biz_type`, `biz_id`) VALUES
(1, 2, 'DEMAND_APPROVAL',  '新需求待审批',     '李四提交了招聘需求"产品经理"，请尽快审批。', 'DEMAND', 3),
(1, 7, 'INTERVIEW_ARRANGE', '面试安排通知',     '您被安排为候选人张伟的复试面试官，时间：6月10日 14:00。', 'INTERVIEW', 2),
(1, 2, 'OFFER_APPROVAL',   'Offer待审批',      '候选人张伟的Offer已提交审批，请处理。', 'OFFER', 3),
(1, 4, 'TASK_ASSIGN',      '入职任务分配',      '您有新的入职任务待处理：候选人刘洋的入职准备工作。', 'ONBOARD', 1),
(1, 3, 'SLA_ALERT',        '需求SLA预警',      '需求"高级前端工程师"已超过30天未完成招聘，请关注。', 'DEMAND', 1);

-- ============================================================
-- 20. 操作日志
-- ============================================================
INSERT IGNORE INTO `operation_log` (`tenant_id`, `user_id`, `module`, `action`, `target_type`, `target_id`, `detail`) VALUES
(1, 2, 'demand',    'create',  'demand',    1, '{"title":"高级前端工程师"}'),
(1, 3, 'demand',    'approve', 'demand',    1, '{"action":"APPROVE","comment":"同意"}'),
(1, 2, 'job',       'create',  'job',       1, '{"title":"高级前端工程师"}'),
(1, 2, 'job',       'activate','job',       1, '{}'),
(1, 2, 'interview', 'arrange', 'interview', 1, '{"candidate":"张伟","round":"INITIAL"}'),
(1, 2, 'offer',     'create',  'offer',     1, '{"candidate":"陈浩","salary":52000}');

-- ============================================================
-- 21. 用量计数器
-- ============================================================
INSERT IGNORE INTO `usage_counter` (`tenant_id`, `counter_type`, `counter_date`, `count_value`) VALUES
(1, 'RESUME_PARSE',  '2026-06-07', 45),
(1, 'AI_MESSAGE',    '2026-06-07', 128),
(1, 'AGENT_SEARCH',  '2026-06-07', 16),
(1, 'AGENT_MSG',     '2026-06-07', 23);

-- ============================================================
-- 完成！测试数据已全部插入
-- 测试账号: admin / Admin@123456
-- 其他用户: zhangsan/lisi/wangwu/zhaoliu/sunqi/zhouba/wujiu 密码统一为 Admin@123456
-- ============================================================
