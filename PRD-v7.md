# RecruitOS — 集团级 AI 招聘操作系统

**版本**：v7.0  
**日期**：2026-06-11  
**部署模式**：纯 SaaS 云端多租户  
**文档类型**：产品需求规格（含 AI-Native 改造方案）  
**上一版**：[PRD-v6.md](./PRD-v6.md)（v6.0，2026-06-10，实现状态基线）

---

## v7 变更摘要

v6 完成了「ATS with AI features」的完整可演示状态。v7 的目标是从 **ATS with AI** 升级为 **AI-Native Recruiting OS**。

核心转变：
- **v6**：AI 作为附加功能，独立页面，独立引擎
- **v7**：AI 嵌入每个业务决策点，在工作流中自然出现，每个洞察通向可执行动作
- **设计原则新增**：Every insight must have an action. 没有 action 的 AI 输出 = 噪音。

v7 不推翻 v6 的架构，而是在其上叠加四个改造维度：
- **维度 A**（数据层）：让 AI 吃到真实数据
- **维度 B**（模型层）：从规则引擎到统计学习
- **维度 C**（嵌入层）：让 AI 出现在工作流中
- **维度 D**（运营层）：可度量、可进化、可配置

详细方案见：
- [维度 A：数据层改造](docs/DIMENSION-A-DATA-LAYER.md)
- [维度 B：模型层改造](docs/DIMENSION-B-MODEL-LAYER.md)
- [维度 C：嵌入层改造](docs/DIMENSION-C-EMBEDDING-LAYER.md)
- [维度 D：运营层改造](docs/DIMENSION-D-OPERATIONS-LAYER.md)
- [落地路线图](docs/IMPLEMENTATION-ROADMAP-v7.md)

---

## 一、产品愿景（修订）

**Layered on your ATS. Define the job. The AI diagnoses, searches, evaluates, converses, interviews, onboards, calibrates, and learns — embedded in your workflow. You decide, act, and the system evolves.**

修订要点：
- "The system finds, evaluates..." → "The AI diagnoses, searches, evaluates..."
- 新增 calibrates（校准）、embedded in your workflow（嵌入工作流）
- "You decide" → "You decide, act, and the system evolves" — 强调 action 闭环

---

## 二、设计原则（新增 3 条）

| # | 原则 | 说明 | 版本 |
|---|------|------|------|
| 1 | 算法即活体 | 标签权重、招人方式随 L1–L6 信号进化 | v5 |
| 2 | 人在回路 | HR/面试官做价值判断；系统做搜索、初筛、话术草稿 | v5 |
| 3 | 一职位一主线 | 在招职位工作台 = 找人 + 管人枢纽 | v5 |
| 4 | 平台无关 | 企业自有招聘平台账号 + RPA | v5 |
| 5 | 租户即王国 | 数据、模型、配置租户隔离 | v5 |
| **6** | **Insight → Action** | **每条 AI 洞察必须通向一个可执行的操作按钮** | **v7 新增** |
| **7** | **Embedded, Not Separate** | **AI 在用户自然工作流中出现，不是独立页面** | **v7 新增** |
| **8** | **Measurable AI** | **每个 AI 能力必须有启用/未启用的效果度量** | **v7 新增** |

---

## 三、AI 能力三层模型

```
L3: 决策增强 — AI 在判断点提供洞察+可执行操作
    （「候选人意向下降 15%，建议安排 Hiring Manager 沟通 [一键安排]」）

L2: 流程加速 — AI 替代重复认知劳动
    （「已筛选 200 份简历，其中 12 份匹配度 > 85%」）

L1: 数据沉淀 — AI 结构化非结构化信息
    （「简历解析完成：5 项核心技能，3 年相关经验」）
```

RecruitOS v6 状态：L1 部分有，L2 有框架，L3 几乎为零。

v7 目标：L1 全覆盖，L2 深度增强，**L3 成为 RecruitOS 的核心差异化能力**。

---

## 四、8 个 AI 业务触点（v7 完整规格）

### 触点总览

| # | 触点 | 层级 | 用户 | 核心问题 | v6 状态 | v7 目标 |
|---|------|------|------|----------|---------|---------|
| 1 | AI 需求诊断 | L3 | 业务 Leader / HRBP | 「我真的需要招人吗？招什么样的人？」 | △ (mock 数据) | 对话式诊断 + 真实团队数据 |
| 2 | AI 面试实时辅助 | L3 | 面试官 | 「我现在该问什么？有没有偏差？」 | △ (面试前准备) | 面试中实时侧边栏：偏差提醒+追问建议 |
| 3 | AI 校准会主持 | L3 | HR / Bar Raiser | 「评分为什么不一致？该信谁？」 | △ (静态矩阵) | 交互式校准：可修改评分+统计检验+主持脚本 |
| 4 | AI 候选人意向预测 | L3 | HR / Recruiter | 「这个候选人真的会接 Offer 吗？」 | ○ (规则引擎) | LightGBM 分类器 + 风险因子 + 干预建议 |
| 5 | AI 招聘周期预测 | L3 | HR / 业务 Leader | 「什么时候能到岗？卡在哪里？」 | ○ (简单算术) | 管道建模 + 瓶颈识别 + 主动干预 |
| 6 | AI Offer 谈判策略 | L3 | HR / Hiring Manager | 「该出多少钱？怎么谈？」 | ○ (固定逻辑) | 市场数据驱动 + 候选人信号 + 策略建议 |
| 7 | AI 面试官质量治理 | L2→L3 | HR / TA Lead | 「谁的面试靠谱？谁在漂移？」 | ○ (静态计算) | 时间序列偏差追踪 + 预测准确度 + Coaching |
| 8 | AI 人才密度评估 | L3 | 业务 Leader / CHRO | 「招了人，团队真变强了？」 | ○ (硬编码) | 真实标签体系计算 + Bar Raiser 视角 |

---

## 五、v7 AI-Native 架构

```
┌─────────────────────────────────────────────────────────────┐
│                     RecruitOS v7                            │
├─────────────────────────────────────────────────────────────┤
│  Embedding Layer (C)   │  AI 出现在每个工作流页面中           │
│  ────────────────────  │  意向→Pipeline卡片 面试辅助→Kanban  │
│                        │  需求诊断→对话式 校准会→交互式        │
├─────────────────────────────────────────────────────────────┤
│  Brain Service (A+B)   │  数据聚合 + 模型推理 + 决策生成       │
│  ────────────────────  │  Feign→各微服务  ML→模型推理         │
│                        │  异步队列→耗时计算  DB→结果持久化     │
├─────────────────────────────────────────────────────────────┤
│  Operations Layer (D)  │  A/B框架 + 审计日志 + 反馈闭环       │
│  ────────────────────  │  管理后台→配置阈值  Dashboard→度量    │
└─────────────────────────────────────────────────────────────┘
```

详细方案见四个维度文档。

---

## 六、关键产品决策（v7 新增）

| 编号 | 决策 | 理由 |
|------|------|------|
| G6 | 需求提报必须经过 AI 诊断后才能建岗（可跳过但记录） | 从源头减少错招 |
| G7 | 面试官的 AI 辅助面板必须在面试页面内（嵌入，非跳转） | 原则 7 |
| G8 | 校准会必须有 AI 生成的对比矩阵和统计检验结果 | 校准可追溯、可量化 |
| G9 | 所有 AI 建议须标注置信度 + 推理链路 | 可解释性 |
| G10 | 外部市场数据仅用于趋势分析，不针对个体候选人 | 合规 |
| G11 | 每个 AI 输出必须包含至少一个可点击 action button | 原则 6（v7 新增） |
| G12 | 每个 AI 能力必须支持租户级「激进/标准/保守」三档配置 | 灵活性（v7 新增） |
| G13 | 所有 AI 决策写入 `brain_ai_decision_log` 审计表 | 可追溯（v7 新增） |
| G14 | 新租户使用行业模板冷启动，积累 200+ 样本后切换到个性化模型 | 冷启动（v7 新增） |

---

## 七、路线图（修订）

```
Phase 1 — MVP ✅
  多租户 · 需求/审批 · 岗位/候选人/管道 · 面试两轮 · Offer/入职
  话术模板 · 许可计费 · 基础分析

Phase 2 — 渠道运营 + 进化门禁 ✅ (2026-Q1/Q2)
  运营包 · 平台招人任务 · 两阶段筛选 · 待联系池
  L1–L6 信号 · proposal 调度 · HR 确认 · Campaign 版本绑定

Phase 2.5 — 决策智能深化 ✅ (2026-Q2)
  D1 统一匹配 · D2 进化运营 UI · D3 沟通决策树
  D4 分析漏斗 · D5 内推 v1

Phase 3 — AI-Native 改造 【当前 v7】 (2026-Q3/Q4)

  Batch 1 — 数据真实化 + 嵌入工作流 (2 周)
    - 维度 A1-A3：Feign 数据接入 + 持久化 + LLM 结构化输出
    - 维度 C1-C4：意向/面试/Offer/需求诊断嵌入工作流
    - 维度 D1：审计日志

  Batch 2 — 模型升级 + 运营体系 (3 周)
    - 维度 B1-B2：意向 LightGBM + 校准统计
    - 维度 D2-D4：A/B 框架 + 反馈机制 + 配置后台

  Batch 3 — 深度模型 + 交互升级 (3 周)
    - 维度 B3-B6：面试官时间序列 + 人才密度 + 冷启动 + SHAP
    - 维度 C5-C7：校准会交互式 + 需求诊断对话式 + Dashboard AI 指标

Phase 4 — Enterprise 集成 (2026-Q4/2027-Q1)
  HRone 双向 · 候选人 Portal · 电子签/背调 · SSO 白标

Phase 5 — 智能化运营 (ongoing)
  招聘预算 · 预测模型 · 薪酬 AI · 开放 API/Webhook 生态
```

---

## 八、文档索引

| 文档 | 用途 |
|------|------|
| [PRD-v6.md](./PRD-v6.md) | v6 全文（实现状态基线） |
| [PRD-v5.md](./PRD-v5.md) | v5 历史全文 |
| [DEEP-AUDIT-MULTI-PERSPECTIVE.md](docs/DEEP-AUDIT-MULTI-PERSPECTIVE.md) | v7 改造前置审计（四角色交叉分析） |
| [DIMENSION-A-DATA-LAYER.md](docs/DIMENSION-A-DATA-LAYER.md) | 维度 A：数据层改造方案 |
| [DIMENSION-B-MODEL-LAYER.md](docs/DIMENSION-B-MODEL-LAYER.md) | 维度 B：模型层改造方案 |
| [DIMENSION-C-EMBEDDING-LAYER.md](docs/DIMENSION-C-EMBEDDING-LAYER.md) | 维度 C：嵌入层改造方案 |
| [DIMENSION-D-OPERATIONS-LAYER.md](docs/DIMENSION-D-OPERATIONS-LAYER.md) | 维度 D：运营层改造方案 |
| [IMPLEMENTATION-ROADMAP-v7.md](docs/IMPLEMENTATION-ROADMAP-v7.md) | v7 落地路线图 + 里程碑 + 验收标准 |
