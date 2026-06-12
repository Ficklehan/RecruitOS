-- ================================================================
-- RecruitOS v9: 合规审计 & GDPR 数据删除
-- ================================================================

-- 1. 审计日志表：记录所有 AI 决策和敏感操作
CREATE TABLE compliance_audit_log (
  id              BIGINT PRIMARY KEY AUTO_INCREMENT,
  tenant_id       BIGINT NOT NULL,
  actor_id        BIGINT NOT NULL COMMENT '操作者（用户或 AI agent）',
  actor_type      ENUM('USER','AI_AGENT','SYSTEM','SCHEDULER') NOT NULL,
  action          VARCHAR(64) NOT NULL COMMENT '操作类型：AI_ADVICE_GENERATED, AI_ADVICE_ACCEPTED, AI_ADVICE_IGNORED, CANDIDATE_DECISION, DATA_EXPORT, USER_LOGIN, GDPR_DELETE_REQUEST, GDPR_DELETE_EXECUTED, etc.',
  target_type     VARCHAR(64) NOT NULL COMMENT '操作对象：CANDIDATE, JOB, OFFER, INTERVIEW, USER, TENANT',
  target_id       BIGINT,
  target_label    VARCHAR(256) COMMENT '对象可读标签',
  detail_json     JSON COMMENT '完整操作上下文',
  ip_address      VARCHAR(64),
  user_agent      VARCHAR(512),
  created_at      DATETIME DEFAULT NOW(),
  INDEX idx_tenant_created (tenant_id, created_at),
  INDEX idx_tenant_action (tenant_id, action, created_at),
  INDEX idx_actor (actor_id, created_at),
  INDEX idx_target (target_type, target_id)
) COMMENT '合规审计日志';

-- 2. GDPR 数据删除请求表
CREATE TABLE compliance_gdpr_request (
  id              BIGINT PRIMARY KEY AUTO_INCREMENT,
  tenant_id       BIGINT NOT NULL,
  request_type    ENUM('DELETE','EXPORT','ANONYMIZE') NOT NULL,
  target_type     ENUM('CANDIDATE','EMPLOYEE','INTERVIEWER') NOT NULL,
  target_id       BIGINT NOT NULL,
  target_identifier VARCHAR(256) COMMENT '邮箱/手机等可识别信息',
  requestor_id    BIGINT COMMENT '谁发起的请求',
  status          ENUM('PENDING','IN_PROGRESS','COMPLETED','FAILED','CANCELLED') DEFAULT 'PENDING',
  deletion_scope  JSON COMMENT 'JSON: {candidates: true, interviews: true, communications: true, events: false}',
  executed_at     DATETIME,
  verified_by     BIGINT COMMENT '二次确认人',
  audit_trail     JSON COMMENT '每一步操作的记录',
  error_message   TEXT,
  created_at      DATETIME DEFAULT NOW(),
  INDEX idx_tenant_status (tenant_id, status),
  INDEX idx_target (target_type, target_id)
) COMMENT 'GDPR 数据主体请求';

-- 3. 数据保留策略配置表（租户级）
CREATE TABLE compliance_retention_policy (
  id              BIGINT PRIMARY KEY AUTO_INCREMENT,
  tenant_id       BIGINT NOT NULL,
  data_category   ENUM('CANDIDATE_RESUME','INTERVIEW_RECORDING','COMMUNICATION_LOG',
                       'OFFER_DOCUMENT','AI_JUDGMENT','AUDIT_LOG','COGNITIVE_MEMORY') NOT NULL,
  retention_days  INT NOT NULL DEFAULT 730 COMMENT '保留天数，0=永久',
  auto_delete     BOOLEAN DEFAULT FALSE,
  legal_hold      BOOLEAN DEFAULT FALSE COMMENT '司法冻结标记',
  updated_at      DATETIME DEFAULT NOW(),
  updated_by      BIGINT,
  UNIQUE KEY uk_tenant_category (tenant_id, data_category)
) COMMENT '数据保留策略';

-- 预置保留策略
INSERT INTO compliance_retention_policy (tenant_id, data_category, retention_days, auto_delete) VALUES
(1, 'CANDIDATE_RESUME', 730, TRUE),
(1, 'INTERVIEW_RECORDING', 365, TRUE),
(1, 'COMMUNICATION_LOG', 730, FALSE),
(1, 'OFFER_DOCUMENT', 1825, FALSE),
(1, 'AI_JUDGMENT', 1095, FALSE),
(1, 'AUDIT_LOG', 1825, FALSE),
(1, 'COGNITIVE_MEMORY', 0, FALSE);

-- 4. 飞书/企微集成配置表
CREATE TABLE integration_config (
  id              BIGINT PRIMARY KEY AUTO_INCREMENT,
  tenant_id       BIGINT NOT NULL,
  platform        ENUM('FEISHU','WECOM','DINGTALK') NOT NULL,
  config_key      VARCHAR(64) NOT NULL COMMENT 'webhook_url, bot_token, app_id, app_secret, encrypt_key, verification_token',
  config_value    TEXT NOT NULL,
  encrypted       BOOLEAN DEFAULT FALSE,
  enabled         BOOLEAN DEFAULT TRUE,
  updated_at      DATETIME DEFAULT NOW(),
  UNIQUE KEY uk_tenant_platform_key (tenant_id, platform, config_key)
) COMMENT '飞书/企微/钉钉集成配置';

-- 5. 飞书消息模板
CREATE TABLE integration_message_template (
  id              BIGINT PRIMARY KEY AUTO_INCREMENT,
  tenant_id       BIGINT NOT NULL,
  platform        ENUM('FEISHU','WECOM') NOT NULL,
  template_code   VARCHAR(64) NOT NULL COMMENT 'INTERVIEW_INVITE, OFFER_APPROVAL, PIPELINE_ALERT, DECISION_REMINDER, ONBOARD_WELCOME',
  template_name   VARCHAR(128) NOT NULL,
  card_json       JSON NOT NULL COMMENT '飞书卡片消息 JSON 模板，支持变量 {{variable}}',
  enabled         BOOLEAN DEFAULT TRUE,
  created_at      DATETIME DEFAULT NOW(),
  updated_at      DATETIME DEFAULT NOW(),
  UNIQUE KEY uk_tenant_platform_code (tenant_id, platform, template_code)
) COMMENT '集成消息模板';

-- 预置飞书消息模板
INSERT INTO integration_message_template (tenant_id, platform, template_code, template_name, card_json) VALUES
(1, 'FEISHU', 'INTERVIEW_INVITE', '面试邀请',
 '{"header":{"title":{"tag":"plain_text","content":"📋 面试邀请 - {{jobTitle}}"},"template":"blue"},"elements":[{"tag":"div","text":{"tag":"lark_md","content":"**候选人：**{{candidateName}}\n**面试时间：**{{interviewTime}}\n**面试方式：**{{interviewType}}\n**面试官：**{{interviewer}}"}},{"tag":"hr"},{"tag":"div","text":{"tag":"lark_md","content":"**面试准备：**\n{{prepNotes}}"}},{"tag":"hr"},{"tag":"action","actions":[{"tag":"button","text":{"tag":"plain_text","content":"确认参加"},"type":"primary","value":"{\"action\":\"confirm\",\"interviewId\":\"{{interviewId}}\"}"},{"tag":"button","text":{"tag":"plain_text","content":"调整时间"},"type":"default","value":"{\"action\":\"reschedule\",\"interviewId\":\"{{interviewId}}\"}"},{"tag":"button","text":{"tag":"plain_text","content":"查看详情"},"type":"default","url":"{{detailUrl}}"}]}]}'),

(1, 'FEISHU', 'OFFER_APPROVAL', 'Offer 审批',
 '{"header":{"title":{"tag":"plain_text","content":"📝 Offer 审批 - {{candidateName}}"},"template":"orange"},"elements":[{"tag":"div","text":{"tag":"lark_md","content":"**岗位：**{{jobTitle}}\n**候选人：**{{candidateName}}\n**薪资包：**{{salaryPackage}}\n**级别：**{{level}}"}},{"tag":"hr"},{"tag":"div","text":{"tag":"lark_md","content":"**AI 分析：**\n> 市场竞争力：{{marketCompetitiveness}}\n> 流失风险：{{attritionRisk}}\n> 建议：{{aiAdvice}}"}},{"tag":"hr"},{"tag":"action","actions":[{"tag":"button","text":{"tag":"plain_text","content":"✅ 批准"},"type":"primary","value":"{\"action\":\"approve\",\"offerId\":\"{{offerId}}\"}"},{"tag":"button","text":{"tag":"plain_text","content":"❌ 驳回"},"type":"danger","value":"{\"action\":\"reject\",\"offerId\":\"{{offerId}}\"}"},{"tag":"button","text":{"tag":"plain_text","content":"查看详情"},"type":"default","url":"{{detailUrl}}"}]}]}'),

(1, 'FEISHU', 'PIPELINE_ALERT', '管道告警',
 '{"header":{"title":{"tag":"plain_text","content":"🔥 管道告警 - {{jobTitle}}"},"template":"red"},"elements":[{"tag":"div","text":{"tag":"lark_md","content":"**状态：**管道枯竭\n**当前候选人：**{{totalCandidates}} 名\n**最后活跃：**{{daysSinceLast}} 天前\n**岗位已开放：**{{daysOpen}} 天"}},{"tag":"hr"},{"tag":"div","text":{"tag":"lark_md","content":"**AI 建议：**\n{{aiAdvice}}"}},{"tag":"action","actions":[{"tag":"button","text":{"tag":"plain_text","content":"查看岗位"},"type":"primary","url":"{{detailUrl}}"},{"tag":"button","text":{"tag":"plain_text","content":"增加寻源"},"type":"default","value":"{\"action\":\"boost_sourcing\",\"jobId\":\"{{jobId}}\"}"}]}]}'),

(1, 'FEISHU', 'DECISION_REMINDER', '决策提醒',
 '{"header":{"title":{"tag":"plain_text","content":"⏰ 决策提醒 - {{candidateName}}"},"template":"purple"},"elements":[{"tag":"div","text":{"tag":"lark_md","content":"**候选人：**{{candidateName}}\n**岗位：**{{jobTitle}}\n**当前阶段：**{{currentStage}}\n**停留天数：**{{daysInStage}} 天"}},{"tag":"hr"},{"tag":"div","text":{"tag":"lark_md","content":"**AI 观察：**\n> {{aiObservation}}"}},{"tag":"action","actions":[{"tag":"button","text":{"tag":"plain_text","content":"去决策"},"type":"primary","url":"{{detailUrl}}"},{"tag":"button","text":{"tag":"plain_text","content":"暂缓"},"type":"default","value":"{\"action\":\"snooze\",\"candidateId\":\"{{candidateId}}\"}"}]}]}');
