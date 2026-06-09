-- ============================================================
-- RecruitOS 数据库初始化脚本
-- Database: recruit_os
-- MySQL 8.0+
-- 导入：mysql --default-character-set=utf8mb4 -uroot -p < init.sql
-- ============================================================

CREATE DATABASE IF NOT EXISTS `recruit_os` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE `recruit_os`;

-- ============================================================
-- 1. 租户与组织
-- ============================================================

CREATE TABLE `tenant` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `tenant_code` VARCHAR(32) NOT NULL COMMENT '租户编码',
    `company_name` VARCHAR(128) NOT NULL COMMENT '企业名称',
    `credit_code` VARCHAR(64) DEFAULT NULL COMMENT '统一社会信用代码',
    `plan` VARCHAR(16) NOT NULL DEFAULT 'STARTER' COMMENT '套餐: STARTER/PROFESSIONAL/ENTERPRISE',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '0-试用 1-正常 2-宽限期 3-停用',
    `trial_end_time` DATETIME DEFAULT NULL COMMENT '试用截止时间',
    `config_json` JSON DEFAULT NULL COMMENT '白标配置(域名/Logo/配色/SSO)',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_tenant_code` (`tenant_code`),
    UNIQUE KEY `uk_credit_code` (`credit_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='租户表';

CREATE TABLE `organization` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `tenant_id` BIGINT NOT NULL,
    `parent_id` BIGINT DEFAULT 0 COMMENT '父节点,0=根',
    `name` VARCHAR(128) NOT NULL,
    `type` VARCHAR(16) NOT NULL COMMENT 'GROUP/COMPANY/DEPARTMENT/TEAM',
    `leader_id` BIGINT DEFAULT NULL COMMENT '负责人ID',
    `sort_order` INT DEFAULT 0,
    `status` TINYINT DEFAULT 1,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    INDEX `idx_tenant_parent` (`tenant_id`, `parent_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='组织架构表';

CREATE TABLE `sys_user` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `tenant_id` BIGINT NOT NULL,
    `username` VARCHAR(64) NOT NULL,
    `password_hash` VARCHAR(128) NOT NULL,
    `real_name` VARCHAR(64) NOT NULL,
    `email` VARCHAR(128) DEFAULT NULL,
    `phone` VARCHAR(20) DEFAULT NULL,
    `avatar` VARCHAR(512) DEFAULT NULL,
    `org_id` BIGINT DEFAULT NULL COMMENT '所属组织',
    `mfa_secret` VARCHAR(64) DEFAULT NULL COMMENT 'MFA密钥',
    `sso_type` VARCHAR(16) DEFAULT NULL COMMENT 'SAML/OIDC/FEISHU/WEWORK/DINGTALK',
    `sso_external_id` VARCHAR(128) DEFAULT NULL COMMENT 'SSO外部用户ID',
    `status` TINYINT DEFAULT 1 COMMENT '0-禁用 1-启用',
    `last_login_at` DATETIME DEFAULT NULL,
    `last_login_ip` VARCHAR(64) DEFAULT NULL,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_tenant_username` (`tenant_id`, `username`),
    UNIQUE KEY `uk_tenant_email` (`tenant_id`, `email`),
    INDEX `idx_tenant_org` (`tenant_id`, `org_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

CREATE TABLE `sys_role` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `tenant_id` BIGINT NOT NULL,
    `role_code` VARCHAR(32) NOT NULL COMMENT 'SUPER_ADMIN/HR_MANAGER/RECRUITER/INTERVIEWER/DEPT_HEAD/SSC/EMPLOYEE',
    `role_name` VARCHAR(64) NOT NULL,
    `description` VARCHAR(256) DEFAULT NULL,
    `sort_order` INT DEFAULT 0,
    `status` TINYINT DEFAULT 1,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_tenant_code` (`tenant_id`, `role_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色表';

CREATE TABLE `sys_user_role` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `tenant_id` BIGINT NOT NULL,
    `user_id` BIGINT NOT NULL,
    `role_id` BIGINT NOT NULL,
    `scope_type` VARCHAR(16) DEFAULT 'ALL' COMMENT 'ALL/ORG/SPECIFIC',
    `scope_ids` JSON DEFAULT NULL COMMENT '权限范围关联的组织/岗位ID列表',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_role` (`user_id`, `role_id`),
    INDEX `idx_tenant` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户角色关联表';

CREATE TABLE `sys_permission` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `tenant_id` BIGINT NOT NULL,
    `parent_id` BIGINT DEFAULT 0,
    `perm_code` VARCHAR(64) NOT NULL COMMENT '权限编码',
    `perm_name` VARCHAR(64) NOT NULL COMMENT '权限名称',
    `type` VARCHAR(16) NOT NULL COMMENT 'MENU/BUTTON/API',
    `path` VARCHAR(128) DEFAULT NULL COMMENT '路由路径',
    `icon` VARCHAR(64) DEFAULT NULL,
    `sort_order` INT DEFAULT 0,
    `status` TINYINT DEFAULT 1,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_tenant_code` (`tenant_id`, `perm_code`),
    INDEX `idx_tenant_parent` (`tenant_id`, `parent_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='权限表';

CREATE TABLE `sys_role_permission` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `tenant_id` BIGINT NOT NULL,
    `role_id` BIGINT NOT NULL,
    `permission_id` BIGINT NOT NULL,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_role_perm` (`role_id`, `permission_id`),
    INDEX `idx_tenant` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色权限关联表';

-- ============================================================
-- 2. 招聘需求与岗位
-- ============================================================

CREATE TABLE `recruit_demand` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `tenant_id` BIGINT NOT NULL,
    `demand_no` VARCHAR(32) NOT NULL COMMENT '需求编号',
    `title` VARCHAR(128) NOT NULL COMMENT '需求标题',
    `org_id` BIGINT NOT NULL COMMENT '所属部门',
    `head_count` INT NOT NULL DEFAULT 1 COMMENT '招聘人数',
    `job_level` VARCHAR(16) NOT NULL COMMENT '岗位级别 P6/P7/P8',
    `salary_min` DECIMAL(12,2) DEFAULT NULL COMMENT '期望薪酬下限',
    `salary_max` DECIMAL(12,2) DEFAULT NULL COMMENT '期望薪酬上限',
    `urgency` VARCHAR(8) NOT NULL DEFAULT 'NORMAL' COMMENT 'NORMAL/URGENT/CRITICAL',
    `expected_onboard_date` DATE DEFAULT NULL COMMENT '期望到岗日期',
    `reason` VARCHAR(32) NOT NULL COMMENT 'NEW/REPLACEMENT/EXPANSION/TEMPORARY',
    `job_duty` TEXT DEFAULT NULL COMMENT '岗位职责描述',
    `job_requirement` TEXT DEFAULT NULL COMMENT '任职要求',
    `work_locations` JSON DEFAULT NULL COMMENT '工作地点列表',
    `reporter_id` BIGINT DEFAULT NULL COMMENT '汇报对象(内部用户ID)',
    `initial_interviewer_ids` JSON DEFAULT NULL COMMENT '初面面试官ID列表',
    `final_interviewer_ids` JSON DEFAULT NULL COMMENT '复试面试官ID列表',
    `status` VARCHAR(16) NOT NULL DEFAULT 'DRAFT' COMMENT 'DRAFT/PENDING/APPROVED/REJECTED/JOB_CREATED/RECRUITING/COMPLETED/CLOSED',
    `approved_head_count` INT DEFAULT 0 COMMENT '已通过审批人数',
    `filled_count` INT DEFAULT 0 COMMENT '已到岗人数',
    `reject_reason` VARCHAR(512) DEFAULT NULL COMMENT '驳回原因',
    `created_by` BIGINT NOT NULL,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_tenant_no` (`tenant_id`, `demand_no`),
    INDEX `idx_tenant_status` (`tenant_id`, `status`),
    INDEX `idx_tenant_org` (`tenant_id`, `org_id`),
    INDEX `idx_created_by` (`tenant_id`, `created_by`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='招聘需求表';

CREATE TABLE `approval_instance` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `tenant_id` BIGINT NOT NULL,
    `biz_type` VARCHAR(32) NOT NULL COMMENT 'DEMAND/OFFER',
    `biz_id` BIGINT NOT NULL COMMENT '关联业务ID',
    `process_inst_id` VARCHAR(64) DEFAULT NULL COMMENT 'Flowable流程实例ID',
    `status` VARCHAR(16) NOT NULL DEFAULT 'RUNNING' COMMENT 'RUNNING/APPROVED/REJECTED/CANCELLED',
    `current_node` VARCHAR(64) DEFAULT NULL COMMENT '当前审批节点',
    `current_approver_id` BIGINT DEFAULT NULL COMMENT '当前审批人',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    INDEX `idx_biz` (`tenant_id`, `biz_type`, `biz_id`),
    INDEX `idx_approver` (`tenant_id`, `current_approver_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='审批流实例表';

CREATE TABLE `approval_record` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `tenant_id` BIGINT NOT NULL,
    `instance_id` BIGINT NOT NULL COMMENT '审批实例ID',
    `node_name` VARCHAR(64) NOT NULL COMMENT '审批节点',
    `approver_id` BIGINT NOT NULL COMMENT '审批人',
    `action` VARCHAR(16) NOT NULL COMMENT 'APPROVE/REJECT/TRANSFER/ADD_SIGN',
    `comment` VARCHAR(512) DEFAULT NULL COMMENT '审批意见',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    INDEX `idx_instance` (`instance_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='审批记录表';

CREATE TABLE `job_position` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `tenant_id` BIGINT NOT NULL,
    `demand_id` BIGINT NOT NULL COMMENT '关联招聘需求',
    `job_no` VARCHAR(32) NOT NULL COMMENT '岗位编号',
    `title` VARCHAR(128) NOT NULL,
    `jd_text` TEXT DEFAULT NULL COMMENT 'JD原文',
    `jd_parsed_json` JSON DEFAULT NULL COMMENT 'JD解析结果(实体/标签)',
    `tags` JSON DEFAULT NULL COMMENT '标签数组[{tag,match_weight,search_weight,decision_weight,locked}]',
    `embedding_vector_id` VARCHAR(64) DEFAULT NULL COMMENT 'Milvus中的向量ID',
    `head_count` INT NOT NULL DEFAULT 1,
    `filled_count` INT DEFAULT 0,
    `status` VARCHAR(16) NOT NULL DEFAULT 'DRAFT' COMMENT 'DRAFT/ACTIVE/PAUSED/CLOSED',
    `closed_reason` VARCHAR(256) DEFAULT NULL,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_tenant_no` (`tenant_id`, `job_no`),
    INDEX `idx_tenant_status` (`tenant_id`, `status`),
    INDEX `idx_demand` (`demand_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='岗位表';

-- ============================================================
-- 3. 候选人与简历
-- ============================================================

CREATE TABLE `candidate` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `tenant_id` BIGINT NOT NULL,
    `name` VARCHAR(64) NOT NULL,
    `phone` VARCHAR(20) DEFAULT NULL,
    `email` VARCHAR(128) DEFAULT NULL,
    `gender` VARCHAR(8) DEFAULT NULL COMMENT 'MALE/FEMALE',
    `birth_date` DATE DEFAULT NULL,
    `current_company` VARCHAR(128) DEFAULT NULL,
    `current_title` VARCHAR(128) DEFAULT NULL,
    `work_years` INT DEFAULT NULL,
    `education` VARCHAR(32) DEFAULT NULL COMMENT 'HIGH_SCHOOL/COLLEGE/BACHELOR/MASTER/DOCTOR',
    `school` VARCHAR(128) DEFAULT NULL,
    `major` VARCHAR(128) DEFAULT NULL,
    `expected_salary` DECIMAL(12,2) DEFAULT NULL,
    `work_location` VARCHAR(128) DEFAULT NULL,
    `source` VARCHAR(32) NOT NULL COMMENT 'PLATFORM/REFERRAL/HEADHUNTER/DIRECT/PORTAL',
    `source_detail` VARCHAR(64) DEFAULT NULL COMMENT '具体来源',
    `status` VARCHAR(16) NOT NULL DEFAULT 'NEW' COMMENT 'NEW/SCREENING/INTERVIEWING/OFFER/ONBOARD/POOL/BLACKLIST',
    `tags` JSON DEFAULT NULL COMMENT '候选人标签',
    `embedding_vector_id` VARCHAR(64) DEFAULT NULL COMMENT '经验向量ID',
    `remark` TEXT DEFAULT NULL,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    INDEX `idx_tenant_status` (`tenant_id`, `status`),
    INDEX `idx_tenant_company` (`tenant_id`, `current_company`),
    INDEX `idx_tenant_name` (`tenant_id`, `name`),
    UNIQUE KEY `uk_tenant_phone` (`tenant_id`, `phone`),
    UNIQUE KEY `uk_tenant_email` (`tenant_id`, `email`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='候选人表';

CREATE TABLE `resume` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `tenant_id` BIGINT NOT NULL,
    `candidate_id` BIGINT NOT NULL,
    `file_name` VARCHAR(256) DEFAULT NULL,
    `file_url` VARCHAR(512) DEFAULT NULL COMMENT '原始文件MinIO路径',
    `file_type` VARCHAR(16) DEFAULT NULL COMMENT 'PDF/DOCX/IMAGE',
    `parsed_json` JSON DEFAULT NULL COMMENT '结构化解析结果',
    `raw_text` LONGTEXT DEFAULT NULL COMMENT '纯文本提取',
    `parse_status` TINYINT DEFAULT 0 COMMENT '0-待解析 1-解析中 2-已完成 3-失败',
    `version` INT DEFAULT 1,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    INDEX `idx_candidate` (`candidate_id`),
    INDEX `idx_tenant` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='简历表';

CREATE TABLE `candidate_job` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `tenant_id` BIGINT NOT NULL,
    `candidate_id` BIGINT NOT NULL,
    `job_id` BIGINT NOT NULL,
    `match_score` DECIMAL(5,2) DEFAULT NULL COMMENT 'AI匹配评分',
    `match_detail` JSON DEFAULT NULL COMMENT '标签维度得分明细',
    `screening_status` VARCHAR(16) DEFAULT 'PENDING' COMMENT 'PENDING/PASSED/REJECTED/POOL',
    `screener_id` BIGINT DEFAULT NULL COMMENT '筛选人',
    `screener_comment` TEXT DEFAULT NULL COMMENT '筛选备注',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_cand_job` (`candidate_id`, `job_id`),
    INDEX `idx_job_score` (`tenant_id`, `job_id`, `match_score` DESC),
    INDEX `idx_tenant` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='候选人岗位关联表';

-- ============================================================
-- 4. 面试管理
-- ============================================================

CREATE TABLE `interview` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `tenant_id` BIGINT NOT NULL,
    `candidate_id` BIGINT NOT NULL,
    `job_id` BIGINT NOT NULL,
    `demand_id` BIGINT NOT NULL COMMENT '关联需求(用于获取面试官配置)',
    `round` VARCHAR(16) NOT NULL COMMENT 'INITIAL/FINAL/SUPPLEMENT',
    `round_seq` INT NOT NULL DEFAULT 1 COMMENT '同轮次第几次(支持复试补充)',
    `interviewer_id` BIGINT NOT NULL COMMENT '面试官',
    `scheduled_start_time` DATETIME DEFAULT NULL COMMENT '面试开始时间',
    `scheduled_end_time` DATETIME DEFAULT NULL COMMENT '面试结束时间',
    `actual_start_time` DATETIME DEFAULT NULL,
    `actual_end_time` DATETIME DEFAULT NULL,
    `format` VARCHAR(16) DEFAULT 'ONLINE' COMMENT 'ONLINE/OFFLINE',
    `meeting_link` VARCHAR(512) DEFAULT NULL COMMENT '会议链接',
    `meeting_platform` VARCHAR(16) DEFAULT NULL COMMENT 'TENCENT/LARK/ZOOM/OFFLINE',
    `location` VARCHAR(256) DEFAULT NULL COMMENT '线下地点',
    `duration_minutes` INT DEFAULT 60 COMMENT '预计时长(分钟)',
    `status` VARCHAR(16) NOT NULL DEFAULT 'PENDING_ARRANGE' COMMENT 'PENDING_ARRANGE/ARRANGED/IN_PROGRESS/COMPLETED/CANCELLED/NO_SHOW',
    `recording_url` VARCHAR(512) DEFAULT NULL COMMENT '录制文件URL',
    `ai_summary` TEXT DEFAULT NULL COMMENT 'AI生成的面试摘要',
    `candidate_agreed_record` TINYINT DEFAULT 0 COMMENT '候选人是否同意录制',
    `cancel_reason` VARCHAR(256) DEFAULT NULL,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    INDEX `idx_tenant_round` (`tenant_id`, `round`),
    INDEX `idx_candidate` (`candidate_id`),
    INDEX `idx_job` (`job_id`),
    INDEX `idx_interviewer` (`interviewer_id`),
    INDEX `idx_status` (`tenant_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='面试表';

CREATE TABLE `interview_evaluation` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `tenant_id` BIGINT NOT NULL,
    `interview_id` BIGINT NOT NULL,
    `round` VARCHAR(16) NOT NULL COMMENT 'INITIAL/FINAL/SUPPLEMENT',
    `decision` VARCHAR(16) NOT NULL COMMENT 'PASS/PENDING/REJECT',
    `overall_score` INT DEFAULT NULL COMMENT '总分 0-100',
    `dimensions` JSON NOT NULL COMMENT '评分维度数组',
    `evolution_feedback` JSON DEFAULT NULL COMMENT '进化反馈',
    `comment` TEXT DEFAULT NULL COMMENT '面试官补充说明',
    `submitted_at` DATETIME DEFAULT NULL,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_interview` (`interview_id`),
    INDEX `idx_tenant_round` (`tenant_id`, `round`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='面试评价表';

CREATE TABLE `interview_slot` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `tenant_id` BIGINT NOT NULL,
    `interview_id` BIGINT NOT NULL,
    `slot_start` DATETIME NOT NULL,
    `slot_end` DATETIME NOT NULL,
    `is_selected` TINYINT DEFAULT 0,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    INDEX `idx_interview` (`interview_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='面试候选时间表';

-- ============================================================
-- 5. Offer管理
-- ============================================================

CREATE TABLE `offer` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `tenant_id` BIGINT NOT NULL,
    `candidate_id` BIGINT NOT NULL,
    `job_id` BIGINT NOT NULL,
    `interview_id` BIGINT DEFAULT NULL COMMENT '最终面试ID',
    `salary` DECIMAL(12,2) NOT NULL,
    `bonus` DECIMAL(12,2) DEFAULT NULL,
    `equity` VARCHAR(256) DEFAULT NULL,
    `level` VARCHAR(16) DEFAULT NULL,
    `onboard_date` DATE DEFAULT NULL,
    `offer_template_id` BIGINT DEFAULT NULL,
    `status` VARCHAR(16) DEFAULT 'DRAFT' COMMENT 'DRAFT/PENDING_APPROVAL/APPROVED/SENT/ACCEPTED/REJECTED/WITHDRAWN',
    `e_sign_url` VARCHAR(512) DEFAULT NULL,
    `e_sign_status` VARCHAR(16) DEFAULT NULL,
    `bg_check_status` VARCHAR(16) DEFAULT NULL COMMENT 'PENDING/PASSED/FAILED',
    `reject_reason` VARCHAR(256) DEFAULT NULL,
    `sent_at` DATETIME DEFAULT NULL,
    `accepted_at` DATETIME DEFAULT NULL,
    `created_by` BIGINT NOT NULL,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    INDEX `idx_tenant_status` (`tenant_id`, `status`),
    INDEX `idx_candidate` (`candidate_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Offer表';

-- ============================================================
-- 6. 入职管理
-- ============================================================

CREATE TABLE `onboard` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `tenant_id` BIGINT NOT NULL,
    `candidate_id` BIGINT NOT NULL,
    `offer_id` BIGINT NOT NULL,
    `onboard_date` DATE NOT NULL,
    `status` VARCHAR(16) DEFAULT 'PENDING' COMMENT 'PENDING/IN_PROGRESS/COMPLETED/CANCELLED',
    `hrone_synced` TINYINT DEFAULT 0,
    `hrone_employee_id` VARCHAR(64) DEFAULT NULL,
    `remark` TEXT DEFAULT NULL,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    INDEX `idx_tenant_status` (`tenant_id`, `status`),
    INDEX `idx_candidate` (`candidate_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='入职表';

CREATE TABLE `onboard_task` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `tenant_id` BIGINT NOT NULL,
    `onboard_id` BIGINT NOT NULL,
    `task_type` VARCHAR(32) NOT NULL COMMENT 'UPLOAD_DOC/CONTRACT/IT_EQUIP/BUDDY_PLAN/HR_REVIEW/ACCOUNT_CREATE',
    `task_name` VARCHAR(128) NOT NULL,
    `assignee_id` BIGINT DEFAULT NULL COMMENT '负责人',
    `assignee_type` VARCHAR(16) DEFAULT NULL COMMENT 'CANDIDATE/HR/IT/MANAGER/BUDDY',
    `due_date` DATE DEFAULT NULL,
    `status` VARCHAR(16) DEFAULT 'PENDING' COMMENT 'PENDING/IN_PROGRESS/DONE/SKIPPED',
    `completed_at` DATETIME DEFAULT NULL,
    `remark` VARCHAR(512) DEFAULT NULL,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    INDEX `idx_onboard` (`onboard_id`),
    INDEX `idx_assignee` (`assignee_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='入职任务表';

-- ============================================================
-- 7. AI沟通与话术
-- ============================================================

CREATE TABLE `message_template` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `tenant_id` BIGINT NOT NULL,
    `scene` VARCHAR(32) NOT NULL COMMENT 'INITIAL/CHASE/RESUME_REQ/INTERVIEW_INVITE/OFFER/REACTIVATE',
    `candidate_type` VARCHAR(32) DEFAULT 'ALL' COMMENT 'TECH/MANAGER/FRESH/COMPETITOR/ALL',
    `title` VARCHAR(128) NOT NULL,
    `content` TEXT NOT NULL COMMENT '话术内容,支持变量{{name}}{{company}}',
    `priority` INT DEFAULT 0 COMMENT '优先级(绩效排名)',
    `send_count` INT DEFAULT 0,
    `reply_count` INT DEFAULT 0,
    `resume_count` INT DEFAULT 0,
    `hire_count` INT DEFAULT 0,
    `is_ab_test` TINYINT DEFAULT 0,
    `ab_group` VARCHAR(8) DEFAULT NULL COMMENT 'A/B分组',
    `status` TINYINT DEFAULT 1,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    INDEX `idx_tenant_scene` (`tenant_id`, `scene`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='话术模板表';

CREATE TABLE `conversation` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `tenant_id` BIGINT NOT NULL,
    `candidate_id` BIGINT NOT NULL,
    `job_id` BIGINT DEFAULT NULL,
    `agent_account_id` BIGINT DEFAULT NULL,
    `platform` VARCHAR(16) NOT NULL COMMENT 'BOSS/LIEPIN/MAIMAI/LINKEDIN',
    `platform_conversation_id` VARCHAR(128) DEFAULT NULL,
    `status` VARCHAR(16) DEFAULT 'ACTIVE' COMMENT 'ACTIVE/PAUSED/COMPLETED/BLOCKED',
    `last_message_at` DATETIME DEFAULT NULL,
    `message_count` INT DEFAULT 0,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    INDEX `idx_tenant_candidate` (`tenant_id`, `candidate_id`),
    INDEX `idx_platform` (`tenant_id`, `platform`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='对话记录表';

CREATE TABLE `conversation_message` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `tenant_id` BIGINT NOT NULL,
    `conversation_id` BIGINT NOT NULL,
    `direction` VARCHAR(8) NOT NULL COMMENT 'IN/OUT',
    `sender_type` VARCHAR(16) NOT NULL COMMENT 'AGENT/CANDIDATE/HUMAN',
    `content` TEXT NOT NULL,
    `template_id` BIGINT DEFAULT NULL,
    `safety_check_status` VARCHAR(16) DEFAULT 'PASSED' COMMENT 'PASSED/BLOCKED/WARNED',
    `safety_check_detail` JSON DEFAULT NULL,
    `sent_at` DATETIME DEFAULT NULL,
    `read_at` DATETIME DEFAULT NULL,
    `replied_at` DATETIME DEFAULT NULL,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    INDEX `idx_conversation` (`conversation_id`),
    INDEX `idx_tenant` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='对话消息表';

CREATE TABLE `safety_log` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `tenant_id` BIGINT NOT NULL,
    `conversation_id` BIGINT DEFAULT NULL COMMENT '会话ID',
    `message_id` BIGINT DEFAULT NULL,
    `check_type` VARCHAR(32) NOT NULL COMMENT 'KEYWORD/AI/SENSITIVE/SALARY/PROMPT_INJECTION',
    `check_result` VARCHAR(16) DEFAULT 'PASS' COMMENT 'PASS/BLOCK/WARN',
    `matched_content` TEXT DEFAULT NULL COMMENT '命中内容',
    `risk_level` VARCHAR(8) NOT NULL COMMENT 'LOW/MEDIUM/HIGH/CRITICAL',
    `action` VARCHAR(16) NOT NULL COMMENT 'ALLOW/BLOCKED/REVIEW/PASS/BLOCK',
    `checked_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '审查时间',
    `detail` JSON DEFAULT NULL,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    INDEX `idx_tenant_type` (`tenant_id`, `check_type`),
    INDEX `idx_conversation` (`conversation_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='安全审查日志表';

-- ============================================================
-- 8. 进化引擎
-- ============================================================

CREATE TABLE `evolution_signal` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `tenant_id` BIGINT NOT NULL,
    `job_id` BIGINT NOT NULL,
    `signal_level` TINYINT NOT NULL COMMENT '1-6级信号',
    `confidence` DECIMAL(3,2) NOT NULL COMMENT '置信度',
    `candidate_id` BIGINT DEFAULT NULL,
    `tag_adjustments` JSON DEFAULT NULL COMMENT '标签权重调整明细',
    `learning_rate` DECIMAL(5,4) DEFAULT NULL COMMENT '本次学习率',
    `status` VARCHAR(16) DEFAULT 'PENDING' COMMENT 'PENDING/APPLIED/ROLLED_BACK',
    `ab_group` VARCHAR(8) DEFAULT NULL COMMENT 'A/B分组: TREATMENT/CONTROL',
    `applied_at` DATETIME DEFAULT NULL,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    INDEX `idx_job_level` (`tenant_id`, `job_id`, `signal_level`),
    INDEX `idx_status` (`tenant_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='进化信号记录表';

CREATE TABLE `job_weight_snapshot` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `tenant_id` BIGINT NOT NULL,
    `job_id` BIGINT NOT NULL,
    `snapshot_type` VARCHAR(16) NOT NULL COMMENT 'INITIAL/EVOLUTION/HEALTH_CHECK/ROLLBACK',
    `tags_snapshot` JSON NOT NULL COMMENT '完整标签权重快照',
    `health_score` DECIMAL(5,2) DEFAULT NULL COMMENT '模型健康度评分',
    `signal_id` BIGINT DEFAULT NULL COMMENT '关联信号ID',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    INDEX `idx_job` (`job_id`),
    INDEX `idx_tenant` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='岗位权重快照表';

CREATE TABLE `job_covariance_matrix` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `tenant_id` BIGINT NOT NULL,
    `job_id` BIGINT NOT NULL,
    `matrix_data` JSON NOT NULL COMMENT '协方差矩阵数据',
    `version` INT DEFAULT 1,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    INDEX `idx_job` (`job_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='岗位协方差矩阵表';

CREATE TABLE `ab_test` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `tenant_id` BIGINT NOT NULL,
    `job_id` BIGINT NOT NULL,
    `test_name` VARCHAR(128) NOT NULL,
    `description` TEXT DEFAULT NULL,
    `treatment_snapshot_id` BIGINT NOT NULL COMMENT '实验组快照ID',
    `control_snapshot_id` BIGINT NOT NULL COMMENT '对照组快照ID',
    `traffic_split` DECIMAL(3,2) DEFAULT 0.80 COMMENT '实验组流量比例',
    `start_date` DATETIME NOT NULL,
    `end_date` DATETIME DEFAULT NULL,
    `min_signals` INT DEFAULT 20,
    `status` VARCHAR(16) DEFAULT 'RUNNING' COMMENT 'RUNNING/COMPLETED/CANCELLED',
    `result` JSON DEFAULT NULL COMMENT '测试结果',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    INDEX `idx_job` (`tenant_id`, `job_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='A/B测试表';

-- ============================================================
-- 9. Agent管理
-- ============================================================

CREATE TABLE `agent_account` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `tenant_id` BIGINT NOT NULL,
    `platform` VARCHAR(16) NOT NULL COMMENT 'BOSS/LIEPIN/MAIMAI/LINKEDIN',
    `account_name` VARCHAR(128) NOT NULL,
    `encrypted_credential` TEXT NOT NULL COMMENT 'KMS加密后的凭证',
    `health_score` DECIMAL(5,2) DEFAULT 100.00,
    `daily_msg_count` INT DEFAULT 0,
    `daily_search_count` INT DEFAULT 0,
    `daily_msg_limit` INT DEFAULT 50,
    `daily_search_limit` INT DEFAULT 10,
    `status` VARCHAR(16) DEFAULT 'ACTIVE' COMMENT 'ACTIVE/DEGRADED/PAUSED/BANNED',
    `last_active_at` DATETIME DEFAULT NULL,
    `last_health_check_at` DATETIME DEFAULT NULL,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    INDEX `idx_tenant_platform` (`tenant_id`, `platform`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Agent平台账号表';

CREATE TABLE `agent_task` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `tenant_id` BIGINT NOT NULL,
    `agent_account_id` BIGINT NOT NULL,
    `job_id` BIGINT DEFAULT NULL,
    `task_type` VARCHAR(16) NOT NULL COMMENT 'SEARCH/MESSAGE/RESUME_REQ',
    `status` VARCHAR(16) DEFAULT 'PENDING' COMMENT 'PENDING/RUNNING/COMPLETED/FAILED/CANCELLED',
    `input_data` JSON DEFAULT NULL,
    `result_data` JSON DEFAULT NULL,
    `error_message` VARCHAR(512) DEFAULT NULL,
    `retry_count` INT DEFAULT 0,
    `started_at` DATETIME DEFAULT NULL,
    `completed_at` DATETIME DEFAULT NULL,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    INDEX `idx_account` (`agent_account_id`),
    INDEX `idx_tenant_status` (`tenant_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Agent任务表';

CREATE TABLE `agent_behavior_log` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `tenant_id` BIGINT NOT NULL,
    `agent_account_id` BIGINT NOT NULL,
    `action` VARCHAR(32) NOT NULL COMMENT 'LOGIN/SEARCH/VIEW_PROFILE/SEND_MSG/READ_MSG/WAIT/ERROR',
    `target_url` VARCHAR(512) DEFAULT NULL,
    `duration_ms` INT DEFAULT NULL,
    `success` TINYINT DEFAULT 1,
    `error_detail` VARCHAR(512) DEFAULT NULL,
    `screenshot_url` VARCHAR(512) DEFAULT NULL,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    INDEX `idx_account` (`agent_account_id`),
    INDEX `idx_tenant_time` (`tenant_id`, `created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Agent行为日志表';

-- ============================================================
-- 10. 内推管理
-- ============================================================

CREATE TABLE `referral` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `tenant_id` BIGINT NOT NULL,
    `referrer_id` BIGINT NOT NULL COMMENT '推荐人员工ID',
    `referrer_name` VARCHAR(64) DEFAULT NULL COMMENT '推荐人姓名',
    `candidate_id` BIGINT NOT NULL,
    `candidate_name` VARCHAR(64) DEFAULT NULL COMMENT '候选人姓名',
    `job_id` BIGINT NOT NULL,
    `job_title` VARCHAR(128) DEFAULT NULL COMMENT '岗位名称',
    `status` VARCHAR(16) DEFAULT 'SUBMITTED' COMMENT 'SUBMITTED/SCREENING/INTERVIEWING/HIRED/REJECTED',
    `reward_amount` DECIMAL(12,2) DEFAULT NULL COMMENT '奖励金额',
    `reward_status` VARCHAR(16) DEFAULT NULL COMMENT 'PENDING/APPROVED/PAID',
    `reward_paid_at` DATETIME DEFAULT NULL,
    `remark` VARCHAR(256) DEFAULT NULL,
    `created_by` BIGINT DEFAULT NULL COMMENT '创建人',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    INDEX `idx_tenant_referrer` (`tenant_id`, `referrer_id`),
    INDEX `idx_candidate` (`candidate_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='内推表';

CREATE TABLE `referral_reward` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `tenant_id` BIGINT NOT NULL,
    `referral_id` BIGINT NOT NULL,
    `referrer_id` BIGINT NOT NULL,
    `referrer_name` VARCHAR(64) DEFAULT NULL,
    `reward_type` VARCHAR(16) DEFAULT 'CASH' COMMENT 'CASH/GIFT/OTHER',
    `reward_amount` DECIMAL(12,2) DEFAULT NULL,
    `status` VARCHAR(16) DEFAULT 'PENDING' COMMENT 'PENDING/APPROVED/PAID/CANCELLED',
    `approved_by` BIGINT DEFAULT NULL,
    `approved_at` DATETIME DEFAULT NULL,
    `paid_at` DATETIME DEFAULT NULL,
    `remark` VARCHAR(256) DEFAULT NULL,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    INDEX `idx_tenant_referral` (`tenant_id`, `referral_id`),
    INDEX `idx_tenant_status` (`tenant_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='内推奖励表';

-- ============================================================
-- 11. 猎头管理
-- ============================================================

CREATE TABLE `headhunter_vendor` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `tenant_id` BIGINT NOT NULL,
    `name` VARCHAR(128) NOT NULL,
    `contact_name` VARCHAR(64) DEFAULT NULL,
    `contact_phone` VARCHAR(20) DEFAULT NULL,
    `contact_email` VARCHAR(128) DEFAULT NULL,
    `fee_rate` DECIMAL(5,4) DEFAULT NULL COMMENT '费率(如0.2000=20%)',
    `service_scope` JSON DEFAULT NULL COMMENT '服务范围(行业/级别)',
    `contract_start` DATE DEFAULT NULL,
    `contract_end` DATE DEFAULT NULL,
    `total_recommended` INT DEFAULT 0,
    `total_hired` INT DEFAULT 0,
    `status` TINYINT DEFAULT 1,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    INDEX `idx_tenant` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='猎头供应商表';

CREATE TABLE `headhunter_recommendation` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `tenant_id` BIGINT NOT NULL,
    `vendor_id` BIGINT NOT NULL,
    `candidate_id` BIGINT NOT NULL,
    `job_id` BIGINT NOT NULL,
    `status` VARCHAR(16) DEFAULT 'SUBMITTED' COMMENT 'SUBMITTED/SCREENING/INTERVIEWING/HIRED/REJECTED',
    `fee_amount` DECIMAL(12,2) DEFAULT NULL,
    `fee_status` VARCHAR(16) DEFAULT NULL COMMENT 'PENDING/APPROVED/PAID',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    INDEX `idx_vendor` (`vendor_id`),
    INDEX `idx_tenant` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='猎头推荐记录表';

-- ============================================================
-- 12. 许可与计费
-- ============================================================

CREATE TABLE `tenant_license` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `tenant_id` BIGINT NOT NULL,
    `plan` VARCHAR(16) NOT NULL,
    `max_jobs` INT NOT NULL,
    `max_agents` INT NOT NULL,
    `used_jobs` INT DEFAULT 0,
    `used_agents` INT DEFAULT 0,
    `resume_quota` INT DEFAULT 500 COMMENT '简历解析配额',
    `resume_used` INT DEFAULT 0,
    `message_quota` INT DEFAULT 2000 COMMENT 'AI消息配额',
    `message_used` INT DEFAULT 0,
    `start_date` DATE NOT NULL,
    `end_date` DATE NOT NULL,
    `grace_days` INT DEFAULT 7,
    `status` TINYINT DEFAULT 1,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_tenant` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='租户许可表';

CREATE TABLE `usage_counter` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `tenant_id` BIGINT NOT NULL,
    `counter_type` VARCHAR(32) NOT NULL COMMENT 'RESUME_PARSE/AI_MESSAGE/AGENT_SEARCH/AGENT_MSG',
    `counter_date` DATE NOT NULL,
    `count_value` INT DEFAULT 0,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_tenant_type_date` (`tenant_id`, `counter_type`, `counter_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用量计数器表';

-- ============================================================
-- 13. 消息通知
-- ============================================================

CREATE TABLE `notification` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `tenant_id` BIGINT NOT NULL,
    `user_id` BIGINT NOT NULL COMMENT '接收人',
    `type` VARCHAR(32) NOT NULL COMMENT 'DEMAND_APPROVAL/INTERVIEW_ARRANGE/OFFER_APPROVAL/TASK_ASSIGN/SLA_ALERT/SYSTEM',
    `title` VARCHAR(128) NOT NULL,
    `content` TEXT DEFAULT NULL,
    `biz_type` VARCHAR(32) DEFAULT NULL,
    `biz_id` BIGINT DEFAULT NULL,
    `is_read` TINYINT DEFAULT 0,
    `read_at` DATETIME DEFAULT NULL,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    INDEX `idx_user_read` (`tenant_id`, `user_id`, `is_read`),
    INDEX `idx_created` (`tenant_id`, `created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='通知消息表';

-- ============================================================
-- 14. 操作日志
-- ============================================================

CREATE TABLE `operation_log` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `tenant_id` BIGINT NOT NULL,
    `user_id` BIGINT DEFAULT NULL,
    `module` VARCHAR(32) NOT NULL COMMENT '模块',
    `action` VARCHAR(32) NOT NULL COMMENT '操作',
    `target_type` VARCHAR(32) DEFAULT NULL COMMENT '目标类型',
    `target_id` BIGINT DEFAULT NULL COMMENT '目标ID',
    `detail` JSON DEFAULT NULL COMMENT '操作详情',
    `ip` VARCHAR(64) DEFAULT NULL,
    `user_agent` VARCHAR(512) DEFAULT NULL,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    INDEX `idx_tenant_user` (`tenant_id`, `user_id`),
    INDEX `idx_tenant_time` (`tenant_id`, `created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='操作日志表';

-- ============================================================
-- 初始数据
-- ============================================================

-- 默认租户
INSERT INTO `tenant` (`id`, `tenant_code`, `company_name`, `plan`, `status`, `trial_end_time`)
VALUES (1, 'DEFAULT', 'RecruitOS演示企业', 'ENTERPRISE', 1, '2099-12-31 23:59:59');

-- 默认组织架构
INSERT INTO `organization` (`id`, `tenant_id`, `parent_id`, `name`, `type`, `sort_order`) VALUES
(1, 1, 0, 'RecruitOS演示企业', 'GROUP', 1),
(2, 1, 1, '集团HR中心', 'DEPARTMENT', 1),
(3, 1, 1, '技术中心', 'DEPARTMENT', 2),
(4, 1, 1, '产品中心', 'DEPARTMENT', 3),
(5, 1, 3, '后端团队', 'TEAM', 1),
(6, 1, 3, '前端团队', 'TEAM', 2),
(7, 1, 3, '数据团队', 'TEAM', 3);

-- 默认角色
INSERT INTO `sys_role` (`id`, `tenant_id`, `role_code`, `role_name`, `description`, `sort_order`) VALUES
(1, 1, 'SUPER_ADMIN', '超级管理员', '系统最高权限', 1),
(2, 1, 'HR_MANAGER', 'HR负责人', 'HR部门负责人', 2),
(3, 1, 'RECRUITER', '招聘HR', '招聘专员', 3),
(4, 1, 'INTERVIEWER', '面试官', '面试官', 4),
(5, 1, 'DEPT_HEAD', '部门负责人', '用人部门负责人', 5),
(6, 1, 'SSC', 'SSC专员', '共享服务中心', 6),
(7, 1, 'EMPLOYEE', '员工', '普通员工(内推)', 7);

-- 默认管理员 (密码: Admin@123456 BCrypt加密)
INSERT INTO `sys_user` (`id`, `tenant_id`, `username`, `password_hash`, `real_name`, `email`, `phone`, `org_id`, `status`)
VALUES (1, 1, 'admin', '$2b$10$NSEsKyZQtBEj1cUd4LMDs.0Hc0NwtqYrx2hzOlecGFe6C.xwKuIAu', '系统管理员', 'admin@recruitos.com', '13800000000', 1, 1);

-- 管理员角色关联
INSERT INTO `sys_user_role` (`tenant_id`, `user_id`, `role_id`, `scope_type`) VALUES
(1, 1, 1, 'ALL');

-- 默认权限数据（菜单权限）
INSERT INTO `sys_permission` (`tenant_id`, `parent_id`, `perm_code`, `perm_name`, `type`, `path`, `icon`, `sort_order`) VALUES
-- 一级菜单
(1, 0, 'dashboard', '工作台', 'MENU', '/dashboard', 'Monitor', 1),
(1, 0, 'demand', '招聘需求', 'MENU', '/demand', 'Document', 2),
(1, 0, 'job', '岗位管理', 'MENU', '/job', 'Briefcase', 3),
(1, 0, 'candidate', '候选人', 'MENU', '/candidate', 'User', 4),
(1, 0, 'interview', '面试管理', 'MENU', '/interview', 'VideoCamera', 5),
(1, 0, 'offer', 'Offer管理', 'MENU', '/offer', 'DocumentChecked', 6),
(1, 0, 'onboard', '入职管理', 'MENU', '/onboard', 'Finished', 7),
(1, 0, 'communication', 'AI沟通', 'MENU', '/communication', 'ChatDotRound', 8),
(1, 0, 'evolution', '进化引擎', 'MENU', '/evolution', 'TrendCharts', 9),
(1, 0, 'agent', 'Agent管理', 'MENU', '/agent', 'Connection', 10),
(1, 0, 'referral', '内推管理', 'MENU', '/referral', 'Share', 11),
(1, 0, 'headhunter', '猎头管理', 'MENU', '/headhunter', 'OfficeBuilding', 12),
(1, 0, 'analytics', '数据分析', 'MENU', '/analytics', 'DataAnalysis', 13),
(1, 0, 'settings', '系统设置', 'MENU', '/settings', 'Setting', 14),
-- 二级菜单 - 工作台
(1, 1, 'dashboard:overview', '数据概览', 'MENU', '/dashboard', 'Odometer', 1),
-- 二级菜单 - 招聘需求
(1, 2, 'demand:list', '需求列表', 'MENU', '/demand/list', 'List', 1),
(1, 2, 'demand:board', '需求看板', 'MENU', '/demand/board', 'Grid', 2),
(1, 2, 'demand:approval', '我的审批', 'MENU', '/demand/approval', 'Checked', 3),
-- 二级菜单 - 岗位管理
(1, 3, 'job:list', '岗位列表', 'MENU', '/job/list', 'List', 1),
(1, 3, 'job:jd', 'JD工作台', 'MENU', '/job/jd-editor', 'Edit', 2),
-- 二级菜单 - 候选人
(1, 4, 'candidate:list', '候选人列表', 'MENU', '/candidate/list', 'List', 1),
(1, 4, 'candidate:pool', '人才库', 'MENU', '/candidate/pool', 'Files', 2),
(1, 4, 'candidate:decision', '决策面板', 'MENU', '/candidate/decision', 'DataBoard', 3),
-- 二级菜单 - 面试管理
(1, 5, 'interview:board', '面试看板', 'MENU', '/interview/board', 'Grid', 1),
(1, 5, 'interview:calendar', '面试日历', 'MENU', '/interview/calendar', 'Calendar', 2),
(1, 5, 'interview:evaluation', '评价管理', 'MENU', '/interview/evaluation', 'EditPen', 3),
-- 二级菜单 - Offer管理
(1, 6, 'offer:list', 'Offer列表', 'MENU', '/offer/list', 'List', 1),
(1, 6, 'offer:approval', 'Offer审批', 'MENU', '/offer/approval', 'Checked', 2),
-- 二级菜单 - 入职管理
(1, 7, 'onboard:list', '入职列表', 'MENU', '/onboard/list', 'List', 1),
(1, 7, 'onboard:task', '入职任务', 'MENU', '/onboard/task', 'Finished', 2),
-- 二级菜单 - AI沟通
(1, 8, 'communication:template', '话术管理', 'MENU', '/communication/template', 'ChatLineRound', 1),
(1, 8, 'communication:conversation', '对话记录', 'MENU', '/communication/conversation', 'ChatSquare', 2),
(1, 8, 'communication:safety', '安全审查', 'MENU', '/communication/safety', 'Warning', 3),
-- 二级菜单 - 进化引擎
(1, 9, 'evolution:weight', '权重面板', 'MENU', '/evolution/weight', 'Histogram', 1),
(1, 9, 'evolution:health', '健康监控', 'MENU', '/evolution/health', 'FirstAidKit', 2),
(1, 9, 'evolution:abtest', 'A/B测试', 'MENU', '/evolution/ab-test', 'Switch', 3),
-- 二级菜单 - Agent管理
(1, 10, 'agent:list', 'Agent列表', 'MENU', '/agent/list', 'List', 1),
(1, 10, 'agent:account', '平台账号', 'MENU', '/agent/account', 'UserFilled', 2),
(1, 10, 'agent:log', '行为日志', 'MENU', '/agent/log', 'Notebook', 3),
-- 二级菜单 - 内推管理
(1, 11, 'referral:list', '内推列表', 'MENU', '/referral/list', 'List', 1),
(1, 11, 'referral:reward', '奖励管理', 'MENU', '/referral/reward', 'Money', 2),
-- 二级菜单 - 猎头管理
(1, 12, 'headhunter:vendor', '供应商管理', 'MENU', '/headhunter/vendor', 'OfficeBuilding', 1),
(1, 12, 'headhunter:performance', '效果对比', 'MENU', '/headhunter/performance', 'DataLine', 2),
-- 二级菜单 - 数据分析
(1, 13, 'analytics:funnel', '招聘漏斗', 'MENU', '/analytics/funnel', 'Filter', 1),
(1, 13, 'analytics:roi', '渠道ROI', 'MENU', '/analytics/roi', 'Coin', 2),
(1, 13, 'analytics:interviewer', '面试官效能', 'MENU', '/analytics/interviewer', 'User', 3),
(1, 13, 'analytics:cycle', '招聘周期', 'MENU', '/analytics/cycle', 'Timer', 4),
-- 二级菜单 - 系统设置
(1, 14, 'settings:tenant', '租户设置', 'MENU', '/settings/tenant', 'Setting', 1),
(1, 14, 'settings:org', '组织架构', 'MENU', '/settings/org', 'Tree', 2),
(1, 14, 'settings:role', '角色管理', 'MENU', '/settings/role', 'Lock', 3),
(1, 14, 'settings:user', '用户管理', 'MENU', '/settings/user', 'UserFilled', 4),
(1, 14, 'settings:sso', 'SSO配置', 'MENU', '/settings/sso', 'Link', 5),
(1, 14, 'settings:license', '许可信息', 'MENU', '/settings/license', 'Tickets', 6);

-- 超级管理员拥有所有权限
INSERT INTO `sys_role_permission` (`tenant_id`, `role_id`, `permission_id`)
SELECT 1, 1, id FROM `sys_permission` WHERE tenant_id = 1;

-- 默认许可
INSERT INTO `tenant_license` (`tenant_id`, `plan`, `max_jobs`, `max_agents`, `resume_quota`, `message_quota`, `start_date`, `end_date`)
VALUES (1, 'ENTERPRISE', 999, 99, 99999, 999999, '2024-01-01', '2099-12-31');
