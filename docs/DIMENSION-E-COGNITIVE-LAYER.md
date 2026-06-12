# 维度 E：AI 认知层 — 从工具到伙伴的架构跃迁

**日期**：2026-06-12  
**状态**：v1.0 设计  
**依赖**：维度 A（数据层）、维度 B（模型层）、维度 C（嵌入层）、维度 D（运营层）

---

## 一、认知层的定位

v7 的四个维度解决了"AI 如何工作"（数据、模型、嵌入、运营）。
v8 的认知层解决的是"AI 如何认知"——**记忆、判断、自我意识**。

```
                    ┌──────────────────────────────────────┐
                    │           AI 认知层 (v8 新增)         │
                    │  ┌─────────┐ ┌─────────┐ ┌────────┐ │
                    │  │  记忆体  │ │  判断体  │ │ 自我体  │ │
                    │  │ 跨时间   │ │ 独立观点 │ │ 认识你  │ │
                    │  │ 跨对象   │ │ 可不同意 │ │ 知你盲区│ │
                    │  └─────────┘ └─────────┘ └────────┘ │
                    └──────────────┬───────────────────────┘
                                   │
        ┌──────────────┬───────────┼───────────┬──────────────┐
        │              │           │           │              │
    维度A 数据     维度B 模型    维度C 嵌入    维度D 运营      Agent 评估
    (真实数据)    (ML引擎)     (工作流)     (A/B/审计)     (PrincipalPM)
```

认知层不是又一个微服务。它是 brain 服务内的一个**持久化知识子系统**，所有引擎从这里读取上下文、向这里写入观察结果。

---

## 二、三层结构

### 2.1 记忆体（Memory）— AI 记住了什么

记忆体解决 "上次也这样"、"陈婕的例子告诉我们"、"半年前你拒过一个类似的人"——AI 能跨时间、跨对象关联信息。

```
记忆类型
├── 事件记忆    每次录用/拒绝/离职/Offer谈判 → 完整上下文快照
├── 模式记忆    从多次事件中提炼的规律（"来自阿里的候选人平均Offer接受率高12%"）
├── 对象记忆    每个候选人/面试官/岗位→随时间累积的认知档案
└── 教训记忆    负面结果的事件+归因（"3个表现差的录用，面试时都缺少XXX信号"）
```

### 2.2 判断体（Judgment）— AI 自己的观点

判断体解决 AI 不只是输出分数/排序，而是有自己的观点、能说"我不同意"、能解释为什么这次和上次不同。

```
判断类型
├── 候选人判断    不只是匹配分，是AI的综合观点：值不值得聊？风险在哪？
├── 管道判断      不只是统计数字，是AI对岗位健康度的评估和预测
├── 过程判断      面试质量、面试官偏差、流程瓶颈 —— AI的诊断和归因
├── 决策一致性    对比当前决策和历史模式，标记"这次和你一贯的标准不一样"
└── 不确定性声明   每一条判断附带：我有多确信？什么情况下我会改变判断？
```

### 2.3 自我体（Self-Model）— AI 认识你

自我体解决 AI 知道你的决策偏好、你的盲区、你容易高估/低估的维度。

```
用户画像
├── 决策风格      保守/激进、数据驱动/直觉驱动、偏重硬技能/偏重文化
├── 评分偏差      系统性偏高/偏低的人格维度、易受影响的偏差类型
├── 盲区          历史上容易忽视的信号、反复出现的决策失误模式
└── 进化轨迹      决策质量随时间的变化趋势
```

---

## 三、核心数据模型

### 3.1 事件记忆 — `cognitive_event_memory`

```sql
CREATE TABLE cognitive_event_memory (
  id              BIGINT PRIMARY KEY AUTO_INCREMENT,
  tenant_id       BIGINT NOT NULL,
  event_type      ENUM('HIRE','REJECT','DEPARTURE','OFFER_NEGOTIATION',
                       'CANDIDATE_DISENGAGE','PIPELINE_BLOCKAGE',
                       'INTERVIEW_DISAGREEMENT','SCREENING_OVERRIDE') NOT NULL,
  event_subject   VARCHAR(64) NOT NULL COMMENT '主体类型：CANDIDATE/JOB/INTERVIEWER',
  subject_id      BIGINT NOT NULL COMMENT '主体ID',
  context_json    JSON NOT NULL COMMENT '事件发生时的完整上下文快照',
  -- context_json 示例:
  -- {
  --   "job": {"id": 42, "title": "支付架构师", "level": "P8", "urgency": "HIGH"},
  --   "candidate": {"id": 108, "years": 9, "current_company": "蚂蚁", "skills": [...]},
  --   "pipeline_state": {"stage": "终面", "total_candidates": 12, "days_open": 34},
  --   "interview_scores": [{"dimension": "架构能力", "score": 5, "interviewer": "李四"}],
  --   "decision_maker": {"id": 7, "name": "张三"},
  --   "market_context": {"avg_salary_for_level": 650000, "talent_pool_size": "小"}
  -- }
  outcome         VARCHAR(64) NOT NULL COMMENT '结果：ACCEPTED/REJECTED/RESIGNED/...',
  outcome_reason  TEXT COMMENT '归因（AI事后分析或人工标注）',
  decision_quality ENUM('GOOD','NEUTRAL','POOR','UNKNOWN') DEFAULT 'UNKNOWN'
    COMMENT '事后回看：这个决策质量如何？（离职后回溯填充）',
  occurred_at     DATETIME NOT NULL,
  recorded_at     DATETIME DEFAULT NOW(),
  INDEX idx_tenant_subject (tenant_id, event_subject, subject_id),
  INDEX idx_tenant_type_time (tenant_id, event_type, occurred_at)
) COMMENT 'AI认知层：事件记忆 — 每次关键事件的完整上下文';
```

### 3.2 模式记忆 — `cognitive_pattern_memory`

```sql
CREATE TABLE cognitive_pattern_memory (
  id              BIGINT PRIMARY KEY AUTO_INCREMENT,
  tenant_id       BIGINT NOT NULL,
  pattern_type    ENUM('CANDIDATE_SOURCE_PERFORMANCE',  -- 某渠道候选人入职后表现
                       'INTERVIEWER_SCORING_BIAS',     -- 某面试官的系统性偏差
                       'SKILL_RETENTION_CORRELATION',  -- 某技能与留存的关联
                       'OFFER_ACCEPTANCE_FACTOR',      -- 影响Offer接受率的因子
                       'DEPARTURE_EARLY_SIGNAL',       -- 离职预警信号
                       'TEAM_COMPOSITION_RISK',        -- 团队同质化风险
                       'HIRING_MANAGER_PATTERN')       -- 某用人经理的招聘模式
    NOT NULL,
  pattern_name    VARCHAR(128) NOT NULL COMMENT '人类可读的模式名称',
  pattern_rule    JSON NOT NULL COMMENT '模式的量化表达',
  -- pattern_rule 示例:
  -- {
  --   "statement": "来自阿里的候选人Offer接受率比均值高12%，但入职18个月内离职率高8%",
  --   "confidence": 0.82,
  --   "sample_size": 23,
  --   "statistical_test": "chi-square p=0.03",
  --   "derived_from_events": [101, 203, 307, ...],
  --   "last_validated_at": "2026-05-15"
  -- }
  evidence_events JSON COMMENT '支撑此模式的事件ID列表',
  confidence      DECIMAL(4,3) NOT NULL DEFAULT 0.000,
  sample_size     INT NOT NULL DEFAULT 0,
  status          ENUM('ACTIVE','STALE','INVALIDATED') DEFAULT 'ACTIVE',
  discovered_at   DATETIME DEFAULT NOW(),
  last_validated_at DATETIME,
  INDEX idx_tenant_type (tenant_id, pattern_type),
  INDEX idx_tenant_confidence (tenant_id, confidence)
) COMMENT 'AI认知层：模式记忆 — 从事件中提炼的规律';
```

### 3.3 对象记忆 — `cognitive_object_memory`

```sql
CREATE TABLE cognitive_object_memory (
  id              BIGINT PRIMARY KEY AUTO_INCREMENT,
  tenant_id       BIGINT NOT NULL,
  object_type     ENUM('CANDIDATE','JOB','INTERVIEWER','HIRING_MANAGER','TEAM') NOT NULL,
  object_id       BIGINT NOT NULL,
  summary_tldr    VARCHAR(256) COMMENT 'AI对这个对象的一句话总结',
  evolving_profile JSON COMMENT '随时间累积的认知画像',
  -- candidate示例:
  -- {
  --   "signals": [
  --     {"type": "strong_tech", "source": "简历+技术面", "confidence": 0.9},
  --     {"type": "job_hopper_risk", "source": "3份工作<18月", "confidence": 0.7,
  --      "similar_to": "candidate_47_who_left_in_7_months"}
  --   ],
  --   "interactions": [
  --     {"date": "2026-06-01", "type": "AGENT_MESSAGE", "response_time": "4h", "sentiment": "positive"},
  --     {"date": "2026-06-05", "type": "PHONE_SCREEN", "engagement": "high"}
  --   ],
  --   "ai_opinion": "技术强但有留存风险，建议面试中直接聊跳槽原因"
  -- }
  key_signals     JSON COMMENT '关键信号摘要（供快速检索）',
  risk_flags      JSON COMMENT '风险标记',
  last_updated    DATETIME DEFAULT NOW(),
  UNIQUE KEY uk_tenant_object (tenant_id, object_type, object_id),
  INDEX idx_tenant_type (tenant_id, object_type)
) COMMENT 'AI认知层：对象记忆 — 对每个实体的累积认知';
```

### 3.4 教训记忆 — `cognitive_lesson_memory`

```sql
CREATE TABLE cognitive_lesson_memory (
  id              BIGINT PRIMARY KEY AUTO_INCREMENT,
  tenant_id       BIGINT NOT NULL,
  lesson_type     ENUM('BAD_HIRE_PATTERN',       -- 表现差的录用有什么共性
                       'MISSED_GOOD_CANDIDATE',  -- 被拒的人后来在别处表现好
                       'INTERVIEW_BLIND_SPOT',   -- 面试中没评估到但事后暴露的关键维度
                       'PROCESS_FAILURE')        -- 流程失误导致的招聘失败
    NOT NULL,
  title           VARCHAR(256) NOT NULL,
  description     TEXT NOT NULL COMMENT '完整叙述',
  evidence        JSON NOT NULL COMMENT '支撑这个教训的事件和证据',
  corrective_action TEXT COMMENT 'AI建议的预防措施',
  severity        ENUM('CRITICAL','IMPORTANT','NOTABLE') DEFAULT 'IMPORTANT',
  status          ENUM('ACTIVE','ADDRESSED','STALE') DEFAULT 'ACTIVE',
  learned_at      DATETIME DEFAULT NOW(),
  addressed_at    DATETIME,
  INDEX idx_tenant_type (tenant_id, lesson_type)
) COMMENT 'AI认知层：教训记忆 — 从负面结果中学到的';
```

### 3.5 AI 判断 — `cognitive_judgment`

```sql
CREATE TABLE cognitive_judgment (
  id              BIGINT PRIMARY KEY AUTO_INCREMENT,
  tenant_id       BIGINT NOT NULL,
  judgment_type   ENUM('CANDIDATE_OPINION',      -- AI对候选人的综合看法
                       'PIPELINE_HEALTH',        -- AI对岗位管道健康的评估
                       'HIRING_RISK',            -- AI对录用决策的风险判断
                       'INTERVIEW_QUALITY',      -- AI对面试质量的判断
                       'DECISION_CONSISTENCY',   -- AI对当前决策与历史一致性的判断
                       'TEAM_COMPOSITION',       -- AI对团队构成的看法
                       'PROCESS_BOTTLENECK')     -- AI对流程瓶颈的判断
    NOT NULL,
  subject_type    VARCHAR(64) NOT NULL,
  subject_id      BIGINT NOT NULL,
  judgment_text   TEXT NOT NULL COMMENT 'AI的自然语言判断',
  judgment_json   JSON NOT NULL COMMENT '结构化的判断（含置信度、证据、替代解释）',
  -- judgment_json 示例:
  -- {
  --   "primary": "推荐面试但不建议立即录用",
  --   "confidence": 0.75,
  --   "key_evidence": ["技术面评分5/5", "3份工作<18月", "和张三（去年离职）模式相似"],
  --   "alternative_view": "如果团队急需分布式经验且能接受1年流失风险，可以直接发Offer",
  --   "what_would_change_my_mind": "如果面试中他能合理解释跳槽原因且有成长性证据",
  --   "similar_past_cases": [{"candidate": "张三", "outcome": "7月离职"}]
  -- }
  confidence      DECIMAL(4,3) NOT NULL,
  evidence_memory JSON COMMENT '引用的记忆事件ID列表',
  contradiction   JSON COMMENT 'AI自我质疑：这个判断可能错在哪？',
  status          ENUM('DRAFT','PUBLISHED','SUPERSEDED','WITHDRAWN') DEFAULT 'PUBLISHED',
  created_at      DATETIME DEFAULT NOW(),
  superseded_by   BIGINT COMMENT '被哪个新判断替代',
  INDEX idx_tenant_subject (tenant_id, judgment_type, subject_type, subject_id),
  INDEX idx_tenant_created (tenant_id, created_at)
) COMMENT 'AI认知层：判断 — AI的独立观点';
```

### 3.6 用户画像 — `cognitive_user_model`

```sql
CREATE TABLE cognitive_user_model (
  id              BIGINT PRIMARY KEY AUTO_INCREMENT,
  tenant_id       BIGINT NOT NULL,
  user_id         BIGINT NOT NULL,
  role            ENUM('HIRING_MANAGER','INTERVIEWER','RECRUITER','HRBP','BAR_RAISER') NOT NULL,
  
  -- 决策风格
  decision_speed  DECIMAL(3,2) COMMENT '决策速度 z-score（>0偏快 <0偏慢）',
  risk_tolerance  DECIMAL(3,2) COMMENT '风险容忍度 z-score（>0激进 <0保守）',
  standard_rigidity DECIMAL(3,2) COMMENT '标准刚性 z-score（>0不妥协 <0容易降低标准）',
  
  -- 评分偏差
  scoring_bias_json JSON COMMENT '每个评估维度的系统性偏差',
  -- {"product_sense": +0.4, "execution": -0.2, "culture": +0.1}
  leniency_index  DECIMAL(4,3) COMMENT '整体宽松指数 >1偏松 <1偏严',
  bias_awareness  JSON COMMENT 'AI已知的偏差模式及历史表现',
  
  -- 盲区
  blind_spots_json JSON COMMENT '历史上反复忽视的信号',
  -- [{"signal": "跨部门协作经验", "missed_count": 3, "consequence": "2人因此表现不佳"}]
  
  -- 进化
  decision_quality_trend JSON COMMENT '决策质量时间序列',
  pattern_stability DECIMAL(3,2) COMMENT '决策模式稳定性（>0.7=稳定 <0.5=波动大）',
  
  -- 元数据
  total_decisions INT DEFAULT 0,
  last_evaluated_at DATETIME,
  
  UNIQUE KEY uk_tenant_user (tenant_id, user_id),
  INDEX idx_tenant_role (tenant_id, role)
) COMMENT 'AI认知层：用户画像 — AI对每个用户的认知';
```

### 3.7 AI 主动观察 — `cognitive_observation`

```sql
CREATE TABLE cognitive_observation (
  id              BIGINT PRIMARY KEY AUTO_INCREMENT,
  tenant_id       BIGINT NOT NULL,
  observation_type ENUM('ALERT','INSIGHT','SUGGESTION','QUESTION') NOT NULL,
  severity        ENUM('CRITICAL','WARNING','INFO','CURIOSITY') NOT NULL,
  title           VARCHAR(256) NOT NULL COMMENT '一句话摘要',
  body            TEXT NOT NULL COMMENT '完整叙述（包含证据链和建议行动）',
  related_objects JSON COMMENT '关联的实体 [{type: "JOB", id: 42}, ...]',
  suggested_action JSON COMMENT '建议操作 {text: "暂停猎聘转内推", action_type: "CHANNEL_SWITCH", params: {...}}',
  action_taken    ENUM('PENDING','EXECUTED','DISMISSED','DEFERRED') DEFAULT 'PENDING',
  action_taken_by BIGINT,
  action_taken_at DATETIME,
  feedback        ENUM('HELPFUL','NOT_HELPFUL','PARTIALLY','NONE') DEFAULT 'NONE',
  created_at      DATETIME DEFAULT NOW(),
  expires_at      DATETIME COMMENT '超过此时间自动降级',
  INDEX idx_tenant_severity (tenant_id, severity, created_at),
  INDEX idx_tenant_status (tenant_id, action_taken)
) COMMENT 'AI认知层：主动观察 — AI主动推送给用户的洞察';
```

---

## 四、认知层与现有系统的关系

### 4.1 数据流入

```
进化引擎 L1-L6 信号
    ↓
cognitive_event_memory     ← 每个信号触发事件记录
    ↓
cognitive_pattern_memory   ← 定期从事件中挖掘模式（scheduled job）
cognitive_lesson_memory    ← 负面事件触发教训生成
cognitive_object_memory    ← 实时更新对应对象的认知画像
```

### 4.2 判断生成

```
Brain 引擎（意向预测/校准/诊断/周期...）
    ↓ 读取 cognitive_* 表作为上下文
    ↓ 生成判断
cognitive_judgment          ← 引擎输出持久化
    ↓ 对比历史判断和用户画像
cognitive_observation       ← 值得告诉用户的事情推送到这里
    ↓ 触发 UI 更新
前端菜单状态 / 推送通知
```

### 4.3 用户模型更新

```
每次录用/拒绝决策 → 对比 AI 判断 → 更新 cognitive_user_model
    - 决策速度 ← 从需求确认到录用的天数
    - 风险容忍度 ← 在"高匹配但高风险"vs"中匹配但低风险"之间的选择
    - 评分偏差 ← AI评分 vs 用户评分的系统差异
    - 盲区 ← AI判断高但用户否决且事后证明AI正确的事件
```

---

## 五、认知层驱动的产品行为

### 5.1 菜单：AI 状态驱动

每个菜单项不再是死标签，而是动态展示 AI 的当前认知：

| 菜单项 | 展示内容 | 数据来源 |
|--------|----------|----------|
| 在招岗位列表 | 每个岗位旁标注健康状态 + AI 一句话判断 | `cognitive_judgment` (PIPELINE_HEALTH) |
| 在招岗位命名 | 「支付架构师 P8 🔥 管道枯竭」或「✅ 4人终面」 | `cognitive_judgment` 摘要 |
| 人才发现 | 「AI 提醒：内推沉默3周」 | `cognitive_observation` (未处理) |
| 面试 | 「2场待评估」「王五评分漂移」 | `cognitive_observation` |
| 洞察 | 未处理的 observation 分三级展示 | `cognitive_observation` 按 severity |
| 记忆 | 模式/教训/用户画像的可视化 | 各 cognitive_* 表 |

### 5.2 AI 推送优先级

```
CRITICAL  → 企微/飞书通知 + 产品内红色横幅 + 邮件
WARNING   → 产品内收件箱 + 每日摘要
INFO      → 产品内洞察列表
CURIOSITY → 仅在用户主动看"记忆与模式"时展示
```

### 5.3 AI 自我质疑

每条判断都包含 `contradiction` 字段——AI 必须自己指出判断可能错在哪：

```json
{
  "contradiction": {
    "alternative_explanation": "候选人回复慢不一定是意向低，可能只是面试排期满",
    "weakest_evidence": "提问深度评分仅基于文本分析，未考虑候选人的口音/打字习惯",
    "what_would_overturn": "如果下一轮面试候选人主动提问3个以上深度问题"
  }
}
```

---

## 六、实施路线

### Phase E1：记忆基础设施（第 1 周）

| 任务 | 产出 |
|------|------|
| 创建 7 张 cognitive_* 表 | DDL |
| Brain 服务增加 CognitiveMemoryService | Java |
| 进化引擎事件 → cognitive_event_memory 的写入管道 | 事件消费者 |
| cognitive_object_memory 初始化（从现有数据冷启动） | 批处理 |

### Phase E2：判断生成（第 2 周）

| 任务 | 产出 |
|------|------|
| CognitiveJudgmentEngine：从 memory + brain 引擎输出生成判断 | Java |
| contradiction 自检逻辑 | LLM prompt 工程 |
| cognitive_observation 推送管道 | 调度器 |

### Phase E3：用户建模与菜单联动（第 3 周）

| 任务 | 产出 |
|------|------|
| cognitive_user_model 初始化和持续更新 | 批处理 + 事件驱动 |
| 菜单 AI 状态指示器 | 前端组件 |
| Observation 分级推送 UI | 前端通知中心 |

---

## 七、关键设计决策

| # | 决策 | 理由 |
|---|------|------|
| E1 | 认知层在 brain 服务内，不是独立微服务 | 判断生成依赖 brain 引擎输出，拆分会增加延迟和复杂度 |
| E2 | 记忆是事件溯源（Event Sourcing）模式 | 每个事件不可变、可回溯、可重放，模式可以随时从事件重新计算 |
| E3 | AI 判断必须自带 contradiction | 强制 AI 自我校准，防止过度自信；也让用户理解 AI 的局限 |
| E4 | 用户画像只用于暴露盲区，不用于限制 AI 建议 | AI 可以基于用户画像调整表达方式，但不能因此隐藏可能被用户忽视的重要信号 |
| E5 | cognitive_observation 有失效期 | 过期洞察自动降级，防止"已处理过的问题"持续骚扰用户 |
