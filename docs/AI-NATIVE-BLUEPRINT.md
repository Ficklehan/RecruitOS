# RecruitOS AI 原生重构蓝图

> 从「ATS + AI 功能」到「AI 即招聘操作系统」
> 2026-06-11

---

## 核心理念

**AI 不是功能。AI 是操作系统。**

```
现状：人操作ATS → AI帮忙做某些步骤
目标：AI运营招聘 → 人在关键节点确认和决策
```

三层架构：

```
┌──────────────────────────────────────────────┐
│              决策层 (Human Layer)              │
│      Hiring Manager / HR / 面试官             │
│      只做价值判断：要不要这个人？策略可以吗？      │
├──────────────────────────────────────────────┤
│             AI 大脑 (Brain Layer)              │
│  recruitos-brain — 新增核心服务                │
│  理解业务 → 诊断问题 → 制定策略 → 调度执行       │
│  主动巡检 → 异常检测 → 根因分析 → 改进建议       │
├──────────────────────────────────────────────┤
│             执行层 (Execution Layer)            │
│  现有15服务：agent/candidate/interview/...      │
│  被Brain调度，不自己做决策                      │
└──────────────────────────────────────────────┘
```

---

## 一、新服务架构

### 新增：`recruitos-brain`（AI 大脑服务）

这不再是「又一个微服务」，而是系统的中枢神经。

```
recruitos-brain/
├── domain/
│   ├── BusinessContext.java        — 业务上下文模型
│   ├── TeamCapabilityProfile.java  — 团队能力图谱
│   ├── TalentGapAnalysis.java      — 人才缺口分析
│   ├── RecruitmentStrategy.java    — AI生成的招聘策略
│   ├── ProbationCheckin.java       — 试用期跟踪
│   ├── InterviewScorecard.java     — 结构化评分卡
│   ├── DiagnosisResult.java        — AI诊断结果
│   └── AIDecision.java             — AI决策审计
├── engine/
│   ├── DiagnosisEngine.java        — 主动诊断引擎
│   ├── StrategyEngine.java         — 策略生成引擎
│   ├── PredictionEngine.java       — 质量预测引擎
│   ├── InsightEngine.java          — 洞察生成引擎
│   └── OrchestrationEngine.java    — 调度编排引擎
├── controller/
│   ├── BrainController.java        — AI助手API
│   ├── DiagnosisController.java    — 诊断API
│   ├── StrategyController.java     — 策略API
│   └── InsightController.java      — 洞察API
├── scheduler/
│   ├── DailyDiagnosisJob.java      — 每日自动巡检
│   ├── CampaignOptimizerJob.java   — Campaign自动优化
│   └── HealthMonitorJob.java       — 健康监控
└── llm/
    ├── JDUnderstandingService.java  — JD深度理解
    ├── CandidateInsightService.java — 候选人洞察
    ├── DiagnosisReasoningService.java — 诊断推理
    └── StrategyReasoningService.java — 策略推理
```

### 改造现有服务

| 现有服务 | 改造内容 |
|----------|----------|
| `recruitos-agent` | CampaignOrchestrator 不再自己决策关键词/筛选/话术，改为从 Brain 拉策略 |
| `recruitos-candidate` | ScreeningEngine 废弃；统一调 JdTagMatcher；新增 SuccessProbability 计算 |
| `recruitos-interview` | 面试评估从裸JSON → 结构化Scorecard表；新增面试官校准模块 |
| `recruitos-evolution` | HealthService 假分修复；进化对象从3类扩展为6类（标签/关键词/筛选/话术/渠道/面试维度） |
| `recruitos-onboard` | L6真实化：30/60/90天check-in + Hiring Manager评估 → 回流Brain |
| `recruitos-llm` | 扩展：JD解析 + 简历深度提取 + 诊断推理 + 策略推理 |

---

## 二、新数据模型

### 2.1 业务上下文（让AI理解「为什么招」）

```sql
CREATE TABLE business_context (
  id BIGINT PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  department_id BIGINT,
  business_objective TEXT NOT NULL COMMENT '业务目标（自然语言）',
  key_results JSON COMMENT '关键结果 [{metric, target, deadline}]',
  urgency ENUM('CRITICAL','HIGH','MEDIUM','LOW') DEFAULT 'MEDIUM',
  budget_approved DECIMAL(12,2),
  headcount_allocated INT,
  headcount_consumed INT DEFAULT 0,
  ai_diagnosis JSON COMMENT 'AI诊断结果 {gaps: [...], recommendations: [...], confidence: 0.85}',
  created_at DATETIME DEFAULT NOW(),
  updated_at DATETIME DEFAULT NOW() ON UPDATE NOW()
);

CREATE TABLE team_capability_profile (
  id BIGINT PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  department_id BIGINT,
  member_id BIGINT COMMENT '关联sys_user',
  skill_dimensions JSON COMMENT '[{skill: "分布式系统", level: 4, evidence: "主导XX项目"}]',
  performance_rating DECIMAL(3,2) COMMENT '最近绩效评分',
  career_stage ENUM('GROWING','PEAK','PLATEAU'),
  retention_risk ENUM('LOW','MEDIUM','HIGH'),
  extracted_from TEXT COMMENT '数据来源：面试评价/绩效/项目',
  updated_at DATETIME DEFAULT NOW()
);
```

### 2.2 试用期验证（让AI知道「招对了没有」）

```sql
CREATE TABLE probation_checkin (
  id BIGINT PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  candidate_id BIGINT NOT NULL,
  job_id BIGINT NOT NULL,
  checkin_day INT NOT NULL COMMENT '30/60/90',
  evaluator_id BIGINT COMMENT 'Hiring Manager',
  dimensions JSON NOT NULL COMMENT '[{dimension:"技术深度", interview_score:4, actual_score:3, gap_analysis:"..."}]',
  overall_rating DECIMAL(3,2),
  would_hire_again BOOLEAN,
  strengths TEXT,
  gaps TEXT,
  recommendation ENUM('CONFIRM','EXTEND_PROBATION','TERMINATE'),
  ai_analysis JSON COMMENT 'AI分析：面试vs实际差距、预测准确度',
  submitted_at DATETIME,
  l6_signal_emitted BOOLEAN DEFAULT FALSE,
  created_at DATETIME DEFAULT NOW()
);
```

### 2.3 结构化面试评分卡

```sql
CREATE TABLE interview_scorecard_template (
  id BIGINT PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  job_family VARCHAR(64) NOT NULL COMMENT '岗位族：BACKEND/FRONTEND/PM/DESIGN',
  level_range VARCHAR(32) COMMENT 'P6-P7/P7-P8',
  dimensions JSON NOT NULL COMMENT '[{name:"技术深度", weight:0.30, behavioral_anchors:{"1":"...","3":"...","5":"..."}, questions:[...]}]',
  version INT DEFAULT 1,
  is_active BOOLEAN DEFAULT TRUE,
  created_at DATETIME DEFAULT NOW()
);

CREATE TABLE interview_scorecard_result (
  id BIGINT PRIMARY KEY,
  interview_id BIGINT NOT NULL,
  template_id BIGINT,
  evaluator_id BIGINT NOT NULL,
  dimensions JSON NOT NULL COMMENT '[{name:"技术深度", score:4, evidence:"...", confidence:"HIGH"}]',
  overall_score DECIMAL(3,2),
  decision ENUM('STRONG_HIRE','HIRE','LEANING_HIRE','LEANING_NO','NO_HIRE'),
  ai_consistency_check JSON COMMENT 'AI检查：评分与对话内容一致性',
  submitted_at DATETIME,
  created_at DATETIME DEFAULT NOW()
);

CREATE TABLE interviewer_calibration (
  id BIGINT PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  interviewer_id BIGINT NOT NULL,
  job_family VARCHAR(64),
  total_evaluations INT DEFAULT 0,
  avg_score DECIMAL(3,2),
  score_stddev DECIMAL(3,2),
  leniency_index DECIMAL(3,2) COMMENT '宽松指数：>1偏松 <1偏严',
  consistency_score DECIMAL(3,2) COMMENT '与最终录用结果的一致性',
  last_calibrated_at DATETIME,
  updated_at DATETIME DEFAULT NOW()
);
```

### 2.4 AI诊断与决策审计

```sql
CREATE TABLE ai_diagnosis (
  id BIGINT PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  diagnosis_type ENUM('JOB_HEALTH','FUNNEL_BOTTLENECK','INTERVIEWER_BIAS','CHANNEL_ROI','CANDIDATE_EXPERIENCE','MARKET_SHIFT'),
  target_id BIGINT COMMENT '关联的job_id/campaign_id等',
  severity ENUM('CRITICAL','WARNING','INFO'),
  title VARCHAR(256) NOT NULL,
  evidence JSON COMMENT '诊断依据的数据',
  root_cause TEXT COMMENT 'AI推理的根因',
  recommendation TEXT COMMENT 'AI建议的行动',
  expected_impact TEXT COMMENT '采取行动后的预期效果',
  status ENUM('PENDING','ACKNOWLEDGED','DISMISSED','ACTIONED'),
  acknowledged_by BIGINT,
  actioned_at DATETIME,
  created_at DATETIME DEFAULT NOW()
);

CREATE TABLE ai_decision_log (
  id BIGINT PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  decision_type ENUM('KEYWORD_OPTIMIZE','SCREENING_ADJUST','GREETING_OPTIMIZE','CHANNEL_SWITCH','CANDIDATE_RANK','OFFER_RECOMMEND'),
  target_id BIGINT,
  decision_detail JSON COMMENT '决策内容与依据',
  confidence DECIMAL(3,2),
  auto_executed BOOLEAN DEFAULT FALSE COMMENT '是否自动执行',
  human_confirmed BOOLEAN DEFAULT FALSE,
  confirmed_by BIGINT,
  outcome JSON COMMENT '决策后的结果（回溯填充）',
  created_at DATETIME DEFAULT NOW()
);
```

---

## 三、新菜单结构

### 旧 vs 新

| 旧菜单（职能） | 新菜单（场景） |
|---------------|---------------|
| 工作台 → 收件箱/今日/驾驶舱 | **AI 助手**（首页）= 智能收件箱+今日重点+洞察 |
| 招聘执行 → 进展/候选人/面试... | **招人**（按岗位）= 一个岗位的全部信息+AI建议 |
| 招聘规划 → 需求/审批/职位... | 融入「招人」流程中，AI辅助发起 |
| 人才库 → 人才库/简历/渠道... | **人才网络** = 人才库+渠道+Agent统一视角 |
| 数据洞察 → 漏斗/周期/ROI... | **诊断中心** = AI主动巡检结果+根因分析 |
| 设置 → 租户/组织/角色... | **设置** 保留，简化 |

### 新菜单

```
┌─────────────────────────────────────────────┐
│  🧠 AI 助手                                  │
│  ├── 今日重点（AI排好优先级的事项）              │
│  └── 收件箱（所有待确认/待决策）                │
├─────────────────────────────────────────────┤
│  🎯 招人（按在招职位）                         │
│  ├── 职位列表（每个职位一行，带健康状态）         │
│  ├── 职位工作台 = 一站式：                        │
│  │   ├── 策略（AI推荐的找人策略，可调整确认）      │
│  │   ├── 候选人管道（Kanban + AI排序）           │
│  │   ├── 沟通记录（Agent对话+人工对话）           │
│  │   ├── 面试（安排+结构化评价）                  │
│  │   └── AI洞察（这个岗位的专属诊断）             │
│  └── 发起新需求（AI对话式）                      │
├─────────────────────────────────────────────┤
│  🌐 人才网络                                  │
│  ├── 人才库（AI帮你挖掘存量）                    │
│  ├── 内推（分享链接+奖励）                       │
│  ├── 猎头（合作管理）                           │
│  └── 渠道账号（平台账号+对外风格）               │
├─────────────────────────────────────────────┤
│  📊 诊断中心                                  │
│  ├── 全局健康（AI打分+异常列表）                 │
│  ├── 招聘漏斗（含寻源-沟通-筛选全链路）           │
│  ├── 面试官分析（偏差+校准）                     │
│  └── 渠道ROI（含AI优化建议）                    │
├─────────────────────────────────────────────┤
│  ⚙️ 设置                                     │
│  └── 租户/组织/角色/用户/SSO/许可（保持）        │
└─────────────────────────────────────────────┘
```

---

## 四、Phase A：数据闭环（第1-4周）

**目标**：让AI有真实数据可学，建立「招聘结果 → 策略反馈」的完整回路。

### 第一周：试用期验证（L6真实化）

| 任务 | 涉及模块 | 产出 |
|------|----------|------|
| 建 `probation_checkin` 表 | SQL | DDL |
| 建 `ProbationCheckin` 实体+Mapper+Service | recruitos-onboard | Java |
| 30/60/90天自动触发check-in | recruitos-onboard | 调度器 |
| Hiring Manager 评估表单（5维度与面试对齐） | recruitos-frontend | Vue页面 |
| AI分析：面试评分 vs 实际绩效差距 | recruitos-brain | 分析逻辑 |
| L6信号真实发射到进化引擎 | recruitos-onboard→evolution | 事件流 |

### 第二周：业务上下文建模

| 任务 | 涉及模块 | 产出 |
|------|----------|------|
| 建 `business_context`、`team_capability_profile` 表 | SQL | DDL |
| 需求创建流程改造：从「填表」变为「对话」 | recruitos-demand + brain | API |
| AI分析业务目标 → 诊断能力缺口 → 建议HC | recruitos-brain | LLM流水线 |
| 需求审批页展示AI诊断结果 | recruitos-frontend | Vue |
| 团队能力图谱初始填充（从历史面试评价+绩效提取） | recruitos-brain | 批处理 |

### 第三周：结构化面试评分卡

| 任务 | 涉及模块 | 产出 |
|------|----------|------|
| 建 `interview_scorecard_template`、`_result` 表 | SQL | DDL |
| 评分卡模板管理CRUD（后端/前端） | recruitos-interview | API+页面 |
| 面试评价从裸JSON迁移到结构化维度 | recruitos-interview | 迁移+改造 |
| AI生成每个候选人的「建议追问问题」 | recruitos-brain | LLM |
| 面试安排时自动分配评估维度给面试官 | recruitos-interview | 分配逻辑 |

### 第四周：面试官校准

| 任务 | 涉及模块 | 产出 |
|------|----------|------|
| 建 `interviewer_calibration` 表 | SQL | DDL |
| 校准数据自动计算（均值/方差/宽松指数） | recruitos-brain | 统计引擎 |
| 校准面板：每个面试官的评分分布 | recruitos-frontend | Vue页面 |
| 校准会流程：独立评分→陈述→讨论→修正 | recruitos-interview | 状态机 |
| 面试官认证体系：Level 1/2 | recruitos-tenant | 权限扩展 |
| HealthService假分修复（真实方差计算） | recruitos-evolution | 修复 |

---

## 五、Phase B：AI 接管执行（第5-10周）

**目标**：AI从「辅助」升级为「主驾驶员」，人在关键节点确认。

### 第5-6周：AI驱动的Campaign

| 任务 | 涉及模块 | 产出 |
|------|----------|------|
| `DiagnosisEngine` 每日自动巡检所有在招岗位 | recruitos-brain | 调度器 |
| Campaign关键词不再HR手动填 → AI从JD+历史录用者自动生成 | recruitos-brain→agent | 策略生成 |
| 筛选策略自适应：回复率<阈值→自动调关键词/话术 | recruitos-brain→agent | 自适应循环 |
| 平台选择数据驱动：哪个平台对这个岗位类型效果好 | recruitos-brain | 策略引擎 |
| 沟通DecisionTree真连：candidate上下文全量传入 | recruitos-agent | 修复 |
| 话术个性化：不只是4分支，而是基于候选人特征的动态生成 | recruitos-brain→agent | LLM |

### 第7-8周：AI增强面试

| 任务 | 涉及模块 | 产出 |
|------|----------|------|
| 面试前：AI分析候选人薄弱维度 → 面试官拿到「重点考察清单」 | recruitos-brain | 推荐引擎 |
| 面试后：AI对比面试官评分 → 标记「可能偏严/偏松」 | recruitos-brain | 校准引擎 |
| AI初评：根据评分+证据 → 判断与对话内容一致性 | recruitos-brain | LLM |
| 综合决策面板升级：展示「录用成功概率」而非仅匹配分 | recruitos-frontend | Vue |
| 面试数据回流JdTagMatcher：哪个标签预测效度高？ | recruitos-brain→evolution | 反馈 |

### 第9-10周：AI决策支持

| 任务 | 涉及模块 | 产出 |
|------|----------|------|
| `PredictionEngine`：根据候选人画像+历史数据→预测入职后绩效 | recruitos-brain | ML模型 |
| Offer策略建议：市场薪酬对标+候选人当前薪资推测+接受概率 | recruitos-brain | 策略引擎 |
| 被拒根因分析：AI分析拒绝原因→回流画像调整 | recruitos-brain | LLM |
| 候选人对比视图：并排显示+AI标注差异 | recruitos-frontend | Vue |
| 备选推荐：「这个岗还有个候选人打分82，建议关注」 | recruitos-brain | 推荐引擎 |

---

## 六、Phase C：AI 全局视角与新界面（第11-14周）

**目标**：新主界面上线，AI主动诊断替代被动报表。

### 第11-12周：AI诊断引擎

| 任务 | 涉及模块 | 产出 |
|------|----------|------|
| `DailyDiagnosisJob`：每天扫描所有岗位→生成诊断报告 | recruitos-brain | 调度器 |
| 诊断类型全覆盖：岗位健康/漏斗瓶颈/面试官偏差/渠道ROI/候选人体验 | recruitos-brain | 诊断引擎 |
| 异常自动分级：P0（影响录用）/P1（效率下降）/P2（优化建议） | recruitos-brain | 分级逻辑 |
| 诊断详情页：症状→证据→根因→建议→预期效果 | recruitos-frontend | Vue |
| 诊断结果推送：收件箱+企微/飞书通知 | recruitos-tenant | 通知 |

### 第13-14周：新主界面

| 任务 | 涉及模块 | 产出 |
|------|----------|------|
| **AI助手首页**：今日重点+全局健康+KPI+洞察卡片 | recruitos-frontend | Vue |
| **职位工作台重构**：一站式（策略+管道+沟通+面试+洞察） | recruitos-frontend | Vue |
| **诊断中心**：替代旧分析页面，AI主动推送 | recruitos-frontend | Vue |
| **菜单重构**：从职能菜单→场景导航 | recruitos-frontend | 路由+菜单 |
| 旧页面保留为二级入口（兼容过渡期） | recruitos-frontend | 路由 |
| AI助手对话式入口：「用自然语言问招聘问题」 | recruitos-frontend+brain | Vue+API |

---

## 七、关键决策记录

| # | 决策 | 理由 |
|---|------|------|
| D1 | 新增 `recruitos-brain` 服务而不是改造现有服务 | 现有15服务各司其职，Brain是横向编排层，不应耦合到任何现有域 |
| D2 | ScreeningEngine废弃，统一JdTagMatcher | 双轨运行是历史债，AI原生必须统一标准 |
| D3 | 保留现有菜单作为兼容，新菜单作为默认 | 用户迁移需要过渡期 |
| D4 | L6真实化优先于JD LLM解析 | 反馈闭环是AI学习的根基，没有L6=盲飞 |
| D5 | 数据闭环（Phase A）优先级最高 | AI再聪明也需要真实数据，L6造假=整个进化引擎自欺欺人 |
| D6 | AI决策可自动执行低风险操作，高风险须HR确认 | 关键词微调自动；筛选标准调整HR确认 |
| D7 | 所有AI决策写入`ai_decision_log` | 可审计、可回溯、可复盘 |

---

## 八、风险与缓解

| 风险 | 概率 | 影响 | 缓解 |
|------|------|------|------|
| L6数据获取困难（Hiring Manager不愿填） | 中 | 高 | 极简评估（3个问题1分钟）+ 自动发提醒 + 关联到转正流程 |
| AI诊断不准确，用户失去信任 | 中 | 高 | 每条诊断带置信度+证据+「为什么这么判断」+人工标记对/错 |
| 菜单重构用户不习惯 | 高 | 中 | 保留旧菜单入口，AB测试新旧两版 |
| LLM成本失控 | 中 | 中 | 诊断/策略用批处理+缓存，对话式用流式，设置成本上限 |
