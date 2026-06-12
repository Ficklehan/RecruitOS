# RecruitOS — 集团级 AI 招聘操作系统 v8

**版本**：v8.0  
**日期**：2026-06-12  
**部署模式**：纯 SaaS 云端多租户  
**文档类型**：产品需求规格（含 AI 认知层架构）  
**上一版**：[PRD-v7.md](./PRD-v7.md)（v7.0，2026-06-11）

---

## v8 变更摘要

v7 完成了 AI-Native 改造的四个维度设计（数据层 / 模型层 / 嵌入层 / 运营层），让 AI 从"独立功能"变成"嵌入工作流的决策增强"。但 v7 的 AI 仍然是**浅层协同**：AI 分析数据、给出建议、等人来问。

v8 的目标是 **深层人机协同**——AI 有自己的记忆、判断、和对你的认知。

核心转变：
- **v7**：AI 嵌入工作流，洞察→行动
- **v8**：AI 有持久记忆和独立判断，跨时间、跨对象、跨决策关联信息

v8 不推翻 v7，在其之上叠加**维度 E：AI 认知层**和**全新的协同菜单结构**。

---

## 一、产品愿景（修订）

**AI doesn't just assist. AI remembers, judges, and knows you. Every hire, every rejection, every departure teaches the system. The AI spots patterns you miss, challenges your biases, and grows sharper with every decision you make. You and the AI think together, work together, decide together, and learn from each other.**

---

## 二、设计原则（新增 4 条）

| # | 原则 | 说明 | 版本 |
|---|------|------|------|
| 1 | 算法即活体 | 标签权重、招人方式随 L1–L6 信号进化 | v5 |
| 2 | 人在回路 | HR/面试官做价值判断；系统做搜索、初筛、话术草稿 | v5 |
| 3 | 一职位一主线 | 在招职位工作台 = 找人 + 管人枢纽 | v5 |
| 4 | 平台无关 | 企业自有招聘平台账号 + RPA | v5 |
| 5 | 租户即王国 | 数据、模型、配置租户隔离 | v5 |
| 6 | Insight → Action | 每条 AI 洞察通向可执行操作 | v7 |
| 7 | Embedded, Not Separate | AI 在用户自然工作流中出现 | v7 |
| 8 | Measurable AI | 每个 AI 能力有效果度量 | v7 |
| **9** | **Memory Over Session** | **AI 跨时间、跨对象记忆，不是会话缓存** | **v8 新增** |
| **10** | **Judgment With Contradiction** | **AI 有自己的观点，且必须自带自我质疑** | **v8 新增** |
| **11** | **Know The User** | **AI 认识每个用户的决策风格、偏差、盲区** | **v8 新增** |
| **12** | **Menu = AI State** | **菜单不是功能导航，是 AI 和你共同的状态视图** | **v8 新增** |

---

## 三、AI 能力四层模型（修订）

```
L4: 认知协同 — AI 有记忆、有判断、认识你           ← v8 新增
    （"上次也这样...""陈婕的例子告诉我们...""你在 Product Sense 上一直偏松..."）

L3: 决策增强 — AI 在判断点提供洞察+可执行操作
    （"候选人意向下降15%，建议安排HM沟通"）

L2: 流程加速 — AI 替代重复认知劳动
    （"已筛选200份简历，12份匹配度>85%"）

L1: 数据沉淀 — AI 结构化非结构化信息
    （"简历解析完成：5项核心技能，3年相关经验"）
```

---

## 四、维度 E：AI 认知层

v8 的核心新增。认知层是 brain 服务内的一个持久化知识子系统，包含三体：

### 4.1 记忆体（Memory）

```
事件记忆    每次录用/拒绝/离职/Offer谈判 → 完整上下文快照
模式记忆    从多次事件中提炼的规律（"来自阿里的候选人Offer接受率高12%但18月离职率高8%"）
对象记忆    每个候选人/面试官/岗位→随时间累积的认知画像
教训记忆    负面结果的事件+归因（"3个表现差的录用，面试时都缺少XXX信号"）
```

### 4.2 判断体（Judgment）

每个判断必须包含：
- **primary**：AI 的核心观点
- **confidence**：置信度 0-1
- **key_evidence**：支撑判断的证据链
- **alternative_view**：另一种可能的解释
- **contradiction**：AI 自己指出的最弱证据和什么情况下判断会改变
- **similar_past_cases**：引用历史上类似案例及其结果

### 4.3 自我体（Self-Model）

AI 对每个用户的认知：
- 决策风格：保守/激进、数据驱动/直觉驱动
- 评分偏差：什么维度系统性偏高/偏低
- 盲区：历史上反复忽视的信号
- 进化轨迹：决策质量随时间变化

详细设计见 [DIMENSION-E-COGNITIVE-LAYER.md](docs/DIMENSION-E-COGNITIVE-LAYER.md)。

DDL 见 [sql/V8__cognitive_layer.sql](sql/V8__cognitive_layer.sql)。

---

## 五、新菜单结构：按协同模式组织

v8 废弃了 v7 的"AI 助手"独立菜单。AI 渗透到每个业务场景中：

```
在招岗位        AI+人协同主战场，每个岗位是共同推进的项目
  ├── 全部岗位   列表旁显示 AI 健康状态（🔥管道枯竭 / ✅进展正常 / ⚠️同质化）
  ├── 候选人管道  Feed+快速裁决（AI排序+标注 + 人滑动决策）
  ├── 面试日历
  ├── 录用通知    AI场景模拟 → 你谈判
  └── 入职

人才发现        AI搜+你搜，合并去重排序
  ├── AI 推荐    不是按匹配度排，是按"AI觉得你该看看"排
  ├── 主动搜索    你搜 + AI同时搜 + AI标注为什么推荐
  ├── 人才库激活  AI扫描存量库 → 匹配新岗位 → 推给你
  ├── Agent调度   你定画像+策略 → AI跑 → 看结果
  ├── 内推
  └── 猎头

面试评估        AI初评+校准+你定级
  ├── 面试准备    AI生成3个最该问的问题，不是长报告
  ├── 面试评估    证据→判断流水线：AI从转录提取证据 → 按维度归档 → 你评分
  ├── 校准会      AI主持："A给4分B给2分，分歧在Product Sense维度"
  └── 面试官质量  AI追踪评分漂移

洞察           AI主动来找你
  ├── 需要处理    P0级：管道停滞、候选人流失、Offer风险
  ├── 值得关注    面试官评分漂移、渠道效率下降
  ├── 长期观察    行业薪酬趋势、人才市场变化
  ├── 需求诊断    结构化画布：AI预填 → 你调整 → AI挑战
  ├── 招聘漏斗
  └── 渠道ROI

知识           AI学到了什么
  ├── 成功画像    哪些条件真的预测了入职绩效？（AI从历史数据算）
  ├── 决策记录    为什么录用/拒绝了？（回流训练AI）
  ├── 教训库      负面结果有什么共同信号？
  ├── 我的模式    AI告诉你的决策偏好和盲区
  └── 周报

设置
  ├── 租户设置
  ├── 组织架构
  ├── 角色管理
  ├── 用户管理
  └── AI 设置    租户级：激进/标准/保守模式 + 触点开关 + 阈值配置
```

---

## 六、关键产品决策（v8 新增）

| 编号 | 决策 | 理由 |
|------|------|------|
| G15 | 移除"AI 助手"独立一级菜单 | AI 应渗透到每个协同场景，独立菜单暗示"AI是单独功能" |
| G16 | 菜单项显示 AI 状态指示器（数字徽标/颜色标记） | 菜单不是功能导航，是AI和人的共同状态视图 |
| G17 | 每条 AI 判断必须包含 contradiction（自我质疑） | 强制 AI 自我校准，让用户理解 AI 的局限 |
| G18 | 用户画像只用于暴露盲区，不用于限制 AI 建议 | AI 可以调整表达方式，但不能因此隐藏重要信号 |
| G19 | 认知层采用事件溯源（Event Sourcing）模式 | 记忆不可变、可回溯、可重放，模式可随时从事件重新计算 |
| G20 | AI 主动观察有失效期 | 过期洞察自动降级，防止已处理的问题持续骚扰用户 |

---

## 七、路线图（修订）

```
Phase 1 — MVP ✅
Phase 2 — 渠道运营 + 进化门禁 ✅
Phase 2.5 — 决策智能深化 ✅
Phase 3 — AI-Native 改造 ✅ (v7)

Phase 3.5 — 认知层基础设施 【当前 v8】 (2026-Q2/Q3)

  Batch E1: 记忆基础设施 (Week 1)
    - 7 张 cognitive_* 表创建
    - 进化引擎事件 → cognitive_event_memory 写入管道
    - cognitive_object_memory 冷启动（从现有数据初始化）

  Batch E2: 判断生成 (Week 2)
    - CognitiveJudgmentEngine
    - contradiction 自检逻辑（LLM prompt）
    - cognitive_observation 推送管道

  Batch E3: 用户建模 + 菜单联动 (Week 3)
    - cognitive_user_model 初始化和更新
    - 菜单 AI 状态指示器前端组件
    - Observation 分级推送 UI

Phase 4 — Enterprise 集成 (2026-Q4)
Phase 5 — 智能化运营 (ongoing)
```

---

## 八、文档索引

| 文档 | 用途 |
|------|------|
| [PRD-v7.md](./PRD-v7.md) | v7 全文（AI-Native 改造基线） |
| [DIMENSION-A-DATA-LAYER.md](docs/DIMENSION-A-DATA-LAYER.md) | 维度 A：数据层改造 |
| [DIMENSION-B-MODEL-LAYER.md](docs/DIMENSION-B-MODEL-LAYER.md) | 维度 B：模型层改造 |
| [DIMENSION-C-EMBEDDING-LAYER.md](docs/DIMENSION-C-EMBEDDING-LAYER.md) | 维度 C：嵌入层改造 |
| [DIMENSION-D-OPERATIONS-LAYER.md](docs/DIMENSION-D-OPERATIONS-LAYER.md) | 维度 D：运营层改造 |
| [DIMENSION-E-COGNITIVE-LAYER.md](docs/DIMENSION-E-COGNITIVE-LAYER.md) | **维度 E：AI 认知层（v8 新增）** |
| [sql/V8__cognitive_layer.sql](sql/V8__cognitive_layer.sql) | **认知层 DDL（v8 新增）** |
| [AI-NATIVE-BLUEPRINT.md](docs/AI-NATIVE-BLUEPRINT.md) | v7 AI原生重构蓝图 |
| [DEEP-AUDIT-MULTI-PERSPECTIVE.md](docs/DEEP-AUDIT-MULTI-PERSPECTIVE.md) | v7 改造前置四角色审计 |
| [IMPLEMENTATION-ROADMAP-v7.md](docs/IMPLEMENTATION-ROADMAP-v7.md) | v7 落地路线图 |
