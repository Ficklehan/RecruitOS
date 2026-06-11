# RecruitOS — 集团级 AI 招聘操作系统

**版本**：v6.0  
**日期**：2026-06-10  
**部署模式**：纯 SaaS 云端多租户  
**文档类型**：产品需求规格（含实现状态与建设路线）  
**上一版**：[PRD-v5.md](./PRD-v5.md)（v5.1，2025 草案；部分能力与路线图已过时）

---

## v6 变更摘要

1. **对齐真实实现**：区分「已闭环 / 有壳未深 / 规划未建」，修正 v5 中「候选人 Portal、行业预训练、Milvus 语义匹配、真实 Boss RPA」等超前描述。  
2. **重排路线图**：Phase 2 渠道运营 + 进化门禁标为 **已完成**；新增 **Phase 2.5 决策智能深化** 为当前建设主线。  
3. **统一产品用语**：与 `PHASE2-UX-SPEC.md`、`businessLabels.ts` 一致（招人方式、平台招人任务、待联系池等）。  
4. **建设优先级**：匹配引擎统一 → 进化可运营化 → 沟通决策树运行时 → 分析/内推/Enterprise 集成。

---

## 一、产品愿景（不变）

**Layered on your ATS. Define the job. The system finds, evaluates, converses, interviews, onboards, and learns. You decide.**

RecruitOS 是叠加在客户 ATS 之上的**招聘决策智能层**，覆盖「业务提需求 → 主动找人 → 筛选沟通 → 面试录用 → 结果回流进化」全链路。核心差异化：**岗位模型随招聘结果进化，且所有策略变更须经 HR 确认（G3）**。

### 设计原则

| 原则 | 含义 |
|------|------|
| 算法即活体 | 岗位标签权重、招人方式随 L1–L6 信号进化 |
| 人在回路 | HR/面试官做价值判断；系统做搜索、初筛、话术草稿 |
| 一职位一主线 | 在招职位工作台 = 找人 + 管人枢纽 |
| 平台无关 | 企业自有招聘平台账号 + RPA（生产环境）；演示环境可用模拟器 |
| 租户即王国 | 数据、模型、配置租户隔离 |

---

## 二、实现状态总览（截至 2026-06）

图例：**✅ 闭环** · **△ 可用未深** · **○ 规划/未建**

| 业务域 | 能力 | 状态 | 说明 |
|--------|------|------|------|
| **租户治理** | 多租户、组织、角色、许可 402 | ✅ | 15 微服务，Gateway 许可校验 |
| **招聘规划** | 需求 CRUD、审批、看板、建岗 | ✅ | 需求→岗位链路可用 |
| **JD / 标签** | 解析、三元权重、任职要求编辑 | △ | 规则/模拟解析；标签结构已对齐 PRD |
| **招人方式** | 运营包生成、HR 确认、版本管理 | ✅ | 规则从标签生成 + 合规扫描 |
| **平台招人** | Campaign、两阶段筛选、轨迹、配额 | ✅ | 默认 simulated RPA；Playwright 可选 |
| **待联系池** | 暂存、批量操作、MiMo 问答 | ✅ | COLLECT_ONLY 模式 |
| **管道** | 7 阶段 Kanban、决策、时间线 | ✅ | HIRE 门槛后 Offer |
| **匹配/决策** | 标签对照、匹配分、决策面板 | △→✅ | **v6 建设**：`JdTagMatcher` 统一计分 |
| **面试录用** | 两轮、结构化评价、Offer 审批 | ✅ | L2/L1 信号已 emit |
| **入职** | 任务清单、编制回写 | △ | L6 Demo Webhook；无真实 HRone |
| **进化引擎** | L1–L6 信号、建议、HR 确认、回滚建议 | ✅ | 协方差为 shrinkage 骨架 |
| **进化运营 UI** | 健康、权重、A/B | △→✅ | **v6 建设**：接真实 API |
| **沟通** | 模板、Profile、安全审查、对话 | △ | **v6 建设**：最小决策树运行时 |
| **内推/猎头** | HR 端列表、奖励、分享链接 | △→✅ | **v6 建设**：分享链接 + 公开投递 + 奖励审批 API |
| **分析** | 漏斗、ROI、周期、面试官 | △→✅ | **v6 建设**：漏斗含 Campaign 寻源段 + 渠道对比 |
| **候选人 Portal** | H5 自助 | ○ | v5 提及，未实现 |
| **HRone / 电子签 / 背调** | Enterprise | ○ | Offer 背调状态字段仅有 |

---

## 三、核心业务闭环（已验收）

### 3.1 渠道运营 + 进化门禁（Phase 2 ✅）

```
任职要求/标签 → 生成招人方式草案 → HR 确认生效
    → 启动平台招人任务（绑定 ops_pack_version）
    → 两阶段筛选 + 可解释淘汰 → L3/L5 信号
    → 调度器生成 evolution_proposal（PENDING）
    → HR 在「招人方式建议」确认/驳回
    → 仅新启动的任务使用新版本运营包（运行中任务不热更新）
```

**强制产品决策（继承 v5.1）**

| 编号 | 决策 |
|------|------|
| G3 | 所有进化变更须 HR 确认后才生效 |
| G4 | 健康 CRITICAL 仅生成 ROLLBACK 建议，不自动回滚 |
| G5 | 信号采集与渠道运营并行交付 |

验收脚本：`recruitos-backend/scripts/verify-phase2-e2e.sh` · 手册：`DEMO-VERIFICATION.md`

### 3.2 招聘执行主链（Phase 1 ✅）

```
管道推进 → 结构化面试评价（L2）→ 录用决策（HIRE）
    → Offer 审批/发送/接受拒绝（L1）→ 入职任务
```

---

## 四、六级进化信号（实现映射）

| 级别 | 业务事件 | 发射模块 | 状态 |
|------|----------|----------|------|
| L3 | 初筛通过/淘汰 | recruitos-agent | ✅ |
| L4 | 候选人回复/沉默 | recruitos-communication | ✅ |
| L5 | 打招呼/收简历 | agent + communication | ✅ |
| L2 | 面试通过/不通过 | recruitos-interview | ✅ |
| L1 | Offer 接受/拒绝 | recruitos-offer | ✅ |
| L6 | 试用期通过/淘汰 | recruitos-onboard（Demo Webhook） | △ |

进化对象（G2）：**搜索关键词 / 筛选策略 / 话术策略** 三类均进入 `evolution_proposal`。

---

## 五、Phase 2.5 — 决策智能深化（当前建设，6–8 周）

> 目标：从「能演示的 ATS + 状态机」升级为 PRD 承诺的「决策智能层」。

### Epic D1 — 统一匹配模型（P0，第 1–2 周）

**问题**：匹配分、筛选分、决策面板曾使用不同启发式逻辑，客户无法信任「同一套标准」。

**方案**：

- 共用 `JdTagMatcher` + `TagMatchScoreCalculator`（recruitos-common）
- 输入：岗位 `tags` JSON + 候选人技能来源（tags、职位、简历 parsed_json）
- 输出：0–100 匹配分 + 与 `MatchVerdictJsonBuilder` 一致的 breakdown
- **消费者**：`CandidateService`、`CampaignOrchestrator`（筛选前写入 `PlatformCandidate.matchScore`）

**验收**：

- [ ] 同一候选人在「决策面板」与「平台筛选轨迹」分数一致（±1）
- [ ] 必备标签未命中时，匹配分 &lt; 及格线且 cons 含「必备要求」

### Epic D2 — 进化可运营化（P0，第 2–3 周）

**问题**：`HealthService`、权重快照、A/B 后端已有，HR 页面曾用 Mock，无法感知 G4 回滚。

**方案**：

- `EvolutionHealth` ← `/api/evolution/health/*`
- `EvolutionWeight` ← `/api/evolution/weight/{jobId}` + history
- `EvolutionAbTest` ← `/api/evolution/abtest/*`（次优先）
- 收件箱/职位工作台展示待确认建议数（已有 API 聚合）

**验收**：

- [ ] CRITICAL 岗位在健康页可见，且存在 ROLLBACK 类型 proposal 可跳转
- [ ] 采纳建议后权重历史新增一条 snapshot

### Epic D3 — 沟通决策树运行时（P1，第 3–4 周）

**问题**：CommunicationProfile 可配置，但发信未按场景分支选话术。

**方案（MVP）**：

```
IF 候选人公司 ∈ 竞品列表 → 竞品定向开场
ELIF 工作年限 ≤ 2 → 应届生/初级话术
ELIF 复聊场景 → 复聊模板
ELSE → 标准初次沟通（读 Profile persona/logic）
```

- 绩效仍走 L4/L5；话术类 proposal 证据引用回复率

**验收**：

- [ ] 同一 Profile 下，竞品公司与普通公司生成的招呼语文案不同
- [ ] 未通过安全审查的文案被拦截

### Epic D4 — 分析漏斗补全（P1，第 4–5 周）

- 漏斗增加：平台检索 → 打招呼 → 收简历 → 纳入候选人（来自 Campaign 统计）
- 渠道对比表：自招 vs 内推 vs 猎头（初版）

### Epic D5 — 内推 v1（P2，第 5–6 周）

- 分享链接 → 简历进管道（SOURCE=REFERRAL）
- 奖励状态随 Offer/入职更新
- 不含企微小程序（后续）

---

## 六、路线图（修订）

```
Phase 1 — MVP ✅
  多租户 · 需求/审批 · 岗位/候选人/管道 · 面试两轮 · Offer/入职
  话术模板 · 许可计费 · 基础分析

Phase 2 — 渠道运营 + 进化门禁 ✅ (2026-Q1/Q2)
  运营包 · 平台招人任务 · 两阶段筛选 · 待联系池
  L1–L6 信号 · proposal 调度 · HR 确认 · Campaign 版本绑定
  MiMo LLM(8095) · Phase2 E2E

Phase 2.5 — 决策智能深化 【当前】 (2026-Q2)
  D1 统一匹配 · D2 进化运营 UI · D3 沟通决策树
  D4 分析漏斗 · D5 内推 v1

Phase 3 — 进化算法 + 面试增强 (2026-Q3)
  协方差完整公式 · A/B 流量接入 Campaign
  进化学习率阶段控制 · 标签/维度锁定 UI
  JD LLM 真解析 · 行业冷启动模板

Phase 4 — Enterprise 集成 (2026-Q4)
  HRone 双向 · 候选人 Portal · 电子签/背调
  猎聘/LinkedIn Agent · SSO 白标 · 雇主品牌站

Phase 5 — 智能化运营 (ongoing)
  招聘预算 · 预测模型 · 薪酬 AI · 开放 API/Webhook 生态
```

---

## 七、关键域规格（精简，细节见 v5 对应章节）

### 7.1 招人方式（原「运营包 / OpsPack」）

JSON 包涵：`searchKeywords`、`screeningProfile`（stage1/stage2、`passThreshold`）、`greetStrategy`、`rechatPolicy` 等。与 `job_weight_snapshot` 版本关联。

### 7.2 两阶段筛选

| 阶段 | 名称 | 默认逻辑 |
|------|------|----------|
| Stage1 | 卡片筛选 CARD | 外露信息 vs `stage1CardRules` 或 minCardScore |
| Stage2 | 简历筛选 FULL_RESUME | 综合匹配分 vs `passThreshold` + 必备规则 |

淘汰须写入 `skip_reason_json` + 人话 `humanReason`。

### 7.3 匹配与决策面板

- 展示：标签行（得分/权重/是否命中）、综合分、岗位内排名、优劣势、建议动作
- 分数来源：**岗位 tags × 候选人技能**（`JdTagMatcher`），叠加年限/学历权重
- 进化后：展示「上次采纳建议」前后的 passThreshold 变化（待 D2）

### 7.4 对外沟通风格（CommunicationProfile）

六模块：AI 人设、公司背景、岗位信息、沟通逻辑、主动索要触发、注意事项。租户默认 + 岗位覆盖。

### 7.5 定价（不变，见 v5 §2.1）

Starter / Professional / Enterprise 三档；用量：岗位、Agent、简历解析、AI 消息。

---

## 八、刻意不做 / 延后（避免范围膨胀）

| 项 | 原因 |
|----|------|
| 进化自动生效 | 违反 G3 |
| 健康自动回滚 | 违反 G4 |
| 运行中 Campaign 热更新策略 | 产品决策 |
| 候选人 Portal 与 Phase 2.5 并行 | 资源优先决策智能 |
| 真实 Boss 全量 RPA 与 D1 并行 | 先统一分数语义，再换执行器 |

---

## 九、文档索引

| 文档 | 用途 |
|------|------|
| [PRD-v5.md](./PRD-v5.md) | 历史全文（安全、技术架构、详细 JSON 示例） |
| [PHASE2-UX-SPEC.md](./PHASE2-UX-SPEC.md) | 用语与信息架构 |
| [PHASE2-IMPLEMENTATION-PLAN.md](./PHASE2-IMPLEMENTATION-PLAN.md) | Phase 2 工程任务（归档） |
| [DEMO-VERIFICATION.md](./DEMO-VERIFICATION.md) | 演示验收手册 |
| [RecruitOS-技术架构文档.md](./RecruitOS-技术架构文档.md) | 15 服务端口与表结构 |

---

## 十、v6 产品决策记录

| 日期 | 决策 | 结论 |
|------|------|------|
| 2026-06-10 | PRD 升版 v6，以实现状态为准 | ✅ |
| 2026-06-10 | 当前建设主线 = Phase 2.5（D1→D2→D3） | ✅ |
| 2026-06-10 | 匹配分统一为 JdTagMatcher 体系 | ✅ |
| 2026-06-10 | 进化健康/权重页必须接 API，禁止 Mock | ✅ |
| 2026-06-10 | 沟通招呼最小决策树（竞品/初级/复聊/标准） | ✅ |
| 2026-06-10 | 分析漏斗补全 Campaign 寻源段 + 渠道对比 | ✅ |
| 2026-06-10 | EvolutionAbTest 接真实 API + ab_test 表对齐 | ✅ |
| 2026-06-10 | 内推 v1：分享链接、公开投递、奖励同步 | ✅ |
