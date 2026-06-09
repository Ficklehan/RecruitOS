# RecruitOS Phase 2 — 渠道运营 + 策略进化闭环

**依据**：`PRD-v5.md` v5.1 · §17.2 Phase 2  
**周期**：6 周（可 2 人并行）  
**验收主线**：渠道招聘 → 产信号 → 出建议 → **HR 确认** → 新版本仅作用于**新 Campaign**

---

## 依赖关系（先做什么）

```
Sprint 0  Schema + 公共 SDK
    ↓
Sprint 1  运营包 v1 + Campaign 绑定          ← 可演示「JD → 启动招聘」
    ↓
Sprint 2  两阶段筛选 + 信号 emit (L3/L5)     ← 可演示「可解释淘汰」
    ↓
Sprint 3  进化建议 + HR 待确认              ← 可演示「进化闭环」
    ↓
Sprint 4  沟通 Profile + 复聊 + 打招呼策略
    ↓
Sprint 5  渠道暂存库 + 全链路联调 + 演示数据
```

---

## Sprint 0（第 1 周前半）— 地基，**立即开始**

### T0.1 数据库 migration-v10

**文件**：`recruitos-backend/sql/migration-v10-phase2-evolution-ops.sql`

| 表/变更 | 说明 |
|---------|------|
| `evolution_signal` | 对齐 PRD：`signal_level`、`confidence`、`tag_adjustments` JSON；废弃/迁移旧字段 |
| `evolution_proposal` | 新建：§6.9 待确认队列 |
| `job_sourcing_ops_pack` | 新建：岗位运营包版本（`job_id`, `version`, `status`, `pack_json`, `confirmed_by`, `confirmed_at`） |
| `campaign_candidate_trace` | 增加 `screen_stage`, `skip_reason_json`, `greet_strategy_applied`, `ops_pack_version` |
| `job_sourcing_campaign` | 增加 `ops_pack_id`, `ops_pack_version` |
| `channel_staging_candidate` | 新建：渠道暂存库 |
| `communication_profile` | 新建：租户/岗位六模块配置 |
| `job_platform_mapping` | 新建：内部岗位 ↔ 平台职位映射 |

**验收**：
- [ ] migration 在空库 + 现有 test-data 库均可执行
- [ ] 与 Java Entity 字段一一对应

---

### T0.2 公共 SDK：EvolutionSignalEmitter

**模块**：`recruitos-common`

```
EvolutionSignalEmitter.emit(EvolutionEmitRequest)
  → HTTP POST /api/evolution/signals/emit（Feign 或 RestTemplate）
  → 失败写本地重试表或日志，不阻塞主业务
```

**验收**：
- [ ] agent / interview / offer 模块可依赖 common 发包
- [ ] 单元测试：emit 失败不抛到业务事务

---

### T0.3 进化服务 Schema 对齐

**模块**：`recruitos-evolution`

- 重写 `EvolutionSignal` Entity 对齐 DB
- `POST /api/evolution/signals/emit` 新接口（内部 + 可鉴权）
- 保留旧 `submitSignal` 作兼容或标记 deprecated

**验收**：
- [ ] emit 写入 `signal_level` + `tag_adjustments`
- [ ] `GET /api/evolution/signals` 按 job_id 可分页

---

## Sprint 1（第 1 周后半 – 第 2 周）— 运营包 v1 + AI 代理模式

### T1.1 运营包领域服务

**模块**：`recruitos-job`（或新建 `recruitos-sourcing` 子模块，优先 job 内避免拆服）

| API | 说明 |
|-----|------|
| `POST /api/jobs/{id}/ops-pack/generate` | 从 JD 标签生成 v1 草案（`DRAFT`） |
| `GET /api/jobs/{id}/ops-pack` | 当前生效版（`ACTIVE`） |
| `GET /api/jobs/{id}/ops-pack/versions` | 历史版本 |
| `PUT /api/jobs/{id}/ops-pack/{version}` | 编辑草案 |
| `POST /api/jobs/{id}/ops-pack/{version}/confirm` | HR 确认 → `ACTIVE`，旧版 `ARCHIVED` |

**生成逻辑（MVP）**：
- `screeningProfile`：硬性标签 → stage2 规则；软性 → 加分项
- `searchKeywords`：`search_weight` Top-N 标签
- `communicationProfile`：租户默认 Profile + JD 岗位信息填充
- `rechatPolicy` / `greetStrategy` / `platformQuotas`：默认值
- **合规扫描**：歧视词剔除

**验收**：
- [ ] 有 JD 标签的岗位 30s 内生成可解析 JSON
- [ ] 确认后仅一个 `ACTIVE` 版本

---

### T1.2 JD 工作台 UI

**文件**：`recruitos-frontend/src/views/job/JdEditor.vue`

- 侧栏/Tab「渠道运营包」
- 生成 / 编辑 / diff（与上一版）
- 按钮「确认运营包」
- 按钮「确认并启动渠道招聘」→ `/planning/jobs/{id}/sourcing?opsPackVersion=x`

**验收**：
- [ ] 未确认运营包时，启动 Campaign 提示先确认
- [ ] 歧视条件保存被拦截并提示

---

### T1.3 Campaign 绑定运营包版本

**模块**：`recruitos-agent`

- `createWorkflow` 入参增加 `opsPackId` / `opsPackVersion`（默认取 ACTIVE）
- `config_json` 冗余关键字段（关键词、阈值）供执行器读取
- Campaign 运行中 **不读取** 运营包更新（版本钉死）

**验收**：
- [ ] 活动详情展示绑定的 `ops_pack_version`
- [ ] 运营包出新版后，运行中活动配置不变

---

## Sprint 2（第 3 周）— 两阶段筛选 + 信号 L3/L5

### T2.1 筛选引擎

**模块**：`recruitos-agent` · `CampaignOrchestratorService`

```
SEARCHED
  → screenStage1(cardFields) → SKIP | CARD_PASSED
  → screenStage2(resume + screeningProfile) → SKIP | PASSED
  → 按 greetStrategy 分支
```

- 写入 `skip_reason_json`（结构化）
- `skip_reason` 保留人话摘要

**验收**：
- [ ] Stage1 不通过不打开在线简历（RPA/simulated 均遵守）
- [ ] 轨迹表可展开拒绝原因

---

### T2.2 轨迹 UI

**文件**：`JobSourcing.vue`

- 列：筛选阶段、拒绝原因、运营包版本
- 时间线组件（行为日志聚合）

**验收**：
- [ ] DEMO 数据可演示 3 种淘汰原因

---

### T2.3 信号埋点（第一批）

| 事件 | 模块 | signal_level |
|------|------|--------------|
| 初筛 SKIP/PASS | agent | L3 |
| 打招呼发出 | agent | L5 |
| 收简历 | agent | L5 |

**验收**：
- [ ] 跑一轮 Campaign 后 `evolution_signal` 有记录
- [ ] `processed=0` 待调度消费

---

## Sprint 3（第 4 周）— 进化调度 + HR 待确认

### T3.1 进化调度器

**模块**：`recruitos-evolution`

- `@Scheduled` 每日 02:00 + 手动 `POST /api/evolution/jobs/{id}/propose`
- 触发条件：未处理信号 ≥ 20（可配置）
- 输出：`evolution_proposal`（`PENDING`），`proposal_type=OPS_PACK`
- 计算 MVP：标签 `search_weight` / `match_weight` 微调 + `passThreshold` 建议 + 话术 ID 排名建议
- **禁止写 ACTIVE 运营包**

**验收**：
- [ ] 有足够信号时自动生成 1 条 proposal
- [ ] `diff_json` + `evidence_json` 人可读

---

### T3.2 HR 待确认 API + UI

| API | 说明 |
|-----|------|
| `GET /api/evolution/proposals` | 待确认列表（按岗位/状态筛选） |
| `GET /api/evolution/proposals/{id}` | 详情 + diff |
| `POST /api/evolution/proposals/{id}/apply` | 确认 → 新 ops_pack 版本 ACTIVE |
| `POST /api/evolution/proposals/{id}/reject` | 驳回 + 原因 |
| `@Scheduled` 过期 | `PENDING` → `EXPIRED`（7 天） |

**前端**：`src/views/evolution/EvolutionProposal.vue`（新建）  
**菜单**：招聘需求 / 岗位管理下「策略进化待确认」，恢复 evolution 路由

**验收**：
- [ ] HR 不点确认，Campaign 仍用旧版
- [ ] 确认后新版本仅新 Campaign 生效
- [ ] 驳回后信号标记已消费且不应用

---

### T3.3 信号埋点（第二批）

| 事件 | 模块 | signal_level |
|------|------|--------------|
| 面试通过/淘汰 | interview | L2 |
| Offer 接受/拒绝 | offer | L1 |
| 候选人回复/沉默 | communication | L4 |

---

## Sprint 4（第 5 周）— 沟通 Profile + 复聊 + 打招呼策略

### T4.1 CommunicationProfile

**模块**：`recruitos-communication`

- 租户默认 CRUD
- 岗位 override CRUD
- Campaign/聊天读取：租户默认 merge 岗位覆盖

**验收**：
- [ ] 新岗位继承租户六模块
- [ ] 发送前仍走安全审查

---

### T4.2 复聊 RechatPolicy

- 表字段或在 `ops_pack.rechatPolicy`
- 调度任务：扫描沉默对话 → 生成复聊任务（遵守 maxAttempts/interval）
- 拒绝关键词 → 停止复聊

**验收**：
- [ ] 复聊不超过 2 次
- [ ] 每次复聊发 L4 信号

---

### T4.3 打招呼策略 + 配额

**`JobSourcing` 启动弹窗**：
- `greetStrategy`：SCREEN_THEN_GREET / COLLECT_ONLY / CARD_GREET
- `searchSource`：推荐/搜索/最新
- `platformQuotas`：按平台日上限

**Orchestrator**：
- `COLLECT_ONLY` → 写 `channel_staging_candidate`
- 配额耗尽 → pause run + 告警

**验收**：
- [ ] 三种策略均可 DEMO 演示
- [ ] `CARD_GREET` 需勾选风险提示

---

## Sprint 5（第 6 周）— 暂存库 + 联调 + 演示

### T5.1 渠道暂存库

**路由**：`/talent/channel-staging`

- 列表、排序、自定义字段配置
- AI 问答（调 LLM 服务，答案写回字段）
- 批量：打招呼 / 确认入库 / 不合适

**验收**：
- [ ] 入库后进入正式候选人，暂存标 `IMPORTED`
- [ ] 截图仅附件，主数据结构化

---

### T5.2 端到端演示脚本

**文件**：`DEMO-VERIFICATION.md` 增补 Phase 2 章节

```
1. JD 解析 → 生成运营包 → 确认
2. 启动渠道招聘（半自动）
3. 查看轨迹拒绝原因
4. 确认打招呼 / 入库
5. 提交面试评价
6. 等待/触发进化建议
7. HR 待确认 → 应用 v2
8. 新 Campaign 使用 v2
```

---

### T5.3 健康告警 → 回滚建议（MVP）

- `HealthService` 阈值触发 → 创建 `proposal_type=ROLLBACK` 的 proposal
- **不自动回滚**，走 T3.2 确认流

---

## 并行分工建议（2 人）

| 角色 | Sprint 0–2 | Sprint 3–5 |
|------|------------|------------|
| **后端 A** | T0 migration、evolution、调度器、proposal | 信号第二批、复聊、健康回滚建议 |
| **后端 B** | ops-pack 服务、orchestrator 两阶段筛选 | CommunicationProfile、暂存库 API |
| **前端** | JdEditor 运营包、JobSourcing 增强 | EvolutionProposal、ChannelStaging、启动弹窗 |

单人则严格按 Sprint 0 → 1 → 2 → 3 顺序，**不要跳**到 UI 花哨功能。

---

## 本周立即开工（Day 1–3 Checklist）

```text
Day 1
- [x] T0.1 起草 migration-v10 SQL
- [x] T0.2 EvolutionSignalEmitter 接口定义（common）
- [x] 评审 ops_pack JSON Schema（与 PRD §7.8 对齐）

Day 2
- [ ] T0.1 migration 在本地 DB 执行通过（需手动跑 SQL）
- [x] T0.3 evolution emit API 可用
- [x] T1.1 ops-pack generate 骨架（规则版）

Day 3
- [x] T1.2 JdEditor 最小 UI（生成 + 确认）
- [x] T1.3 createWorkflow 绑定 ops_pack_version
- [ ] 跑通 E2E：JD → 生成 → 确认 → 启动 Campaign（待 migration + 服务重启）
```

### 下一步（Sprint 2）
- [ ] T2.1 两阶段筛选引擎
- [ ] T2.2 轨迹 UI（拒绝原因）
- [ ] T2.3 Agent 埋点 L3/L5（EvolutionSignalEmitter）
- [ ] T3.1 evolution_proposal 调度器
- [ ] T3.2 HR 待确认工作台

---

## 刻意延后（Phase 3）

- 协方差矩阵完整公式
- A/B 流量自动分流（仅 proposal 上标注 ab_result）
- 进化权重面板与 init.sql 旧表完全统一
- LLM 生成运营包文案（MVP 用规则 + 模板）

---

## 风险与缓解

| 风险 | 缓解 |
|------|------|
| evolution 表结构与旧代码不一致 | Sprint 0 一次性对齐，删 deprecated 字段 |
| RPA 两阶段难测 | simulated 模式先实现状态机，Boss RPA 跟进 |
| 范围膨胀 | Phase 2 不做暂存库 AI 问答深度优化，能问答即可 |
| HR 不确认导致无进化 | 待办提醒 + 岗位页 Badge 数 |

---

## 任务 ID 速查

| ID | 名称 | 优先级 |
|----|------|--------|
| T0.1 | migration-v10 | P0 |
| T0.2 | SignalEmitter SDK | P0 |
| T0.3 | evolution emit API | P0 |
| T1.1 | ops-pack 服务 | P0 |
| T1.2 | JdEditor UI | P0 |
| T1.3 | Campaign 绑定版本 | P0 |
| T2.1 | 两阶段筛选 | P0 |
| T2.2 | 轨迹 UI | P0 |
| T2.3 | 信号 L3/L5 | P0 |
| T3.1 | 进化调度器 | P0 |
| T3.2 | HR 待确认 | P0 |
| T3.3 | 信号 L2/L1/L4 | P1 |
| T4.1 | CommunicationProfile | P1 |
| T4.2 | 复聊 | P1 |
| T4.3 | 打招呼策略 | P1 |
| T5.1 | 渠道暂存库 | P1 |
| T5.2 | E2E 演示 | P0 |
| T5.3 | 回滚建议 | P2 |

**建议第一个 PR**：T0.1 + T0.2 + T0.3（地基，约 2–3 天）
