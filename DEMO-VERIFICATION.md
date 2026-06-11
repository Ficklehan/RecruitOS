# RecruitOS 演示数据验收手册

执行 `recruitos-backend/scripts/reseed-demo.sh` 后，按本手册逐页验证。所有账号密码均为 **Admin@123456**。

## 一键重载数据

```bash
cd RecruitOS/recruitos-backend
./scripts/reseed-demo.sh
# 重启 candidate(8085) offer(8087) interview(8086) agent(8091) demand(8083)
```

## 测试账号

| 账号 | 角色 | 用途 |
|------|------|------|
| admin | 超级管理员 | 审批需求/Offer、收件箱 |
| zhangsan | 招聘 HR | 管道操作、发 Offer |
| lisi | HR 负责人 | 提交需求审批 |
| wangwu / wujiu | 面试官 | 今日面试 |
| zhouba | 技术负责人 | 复试面试官 |

## 核心数据关联图

```
需求 DEM-2026-001 (高级前端)
  └── 岗位 JOB-2026-001
        ├── 寻源活动 #1 (BOSS+猎聘，RUNNING)
        ├── 候选人管道
        │     ├── 张伟  → EVALUATED  → Offer#3 待审批
        │     ├── 李娜  → INTERVIEWING → 今日 10:00 初面
        │     ├── 陈浩  → OFFER       → Offer#1 已发送
        │     └── 杨帆  → SCREENING
        └── 简历 #1-#6 已关联候选人

需求 DEM-2026-005 (数据分析师) → 岗位 JOB-2026-004
  └── 刘洋 → HIRED → 入职#1 CONFIRMED
```

## 按页面验收

### 1. 工作台 → 今日 (`/workspace/today`)

| 预期 | 数据 ID |
|------|---------|
| 李娜 10:00 初面（进行中） | interview #3 |
| 张伟 14:00 复试（已安排） | interview #2 |

### 2. 工作台 → 收件箱 (`/workspace/inbox`)

| 类型 | 预期 |
|------|------|
| 需求审批 | 产品经理 DEM-2026-003（admin 审批） |
| Offer 待审 | 张伟 Offer #3 |
| 面试安排 | 今日复试/初面通知 |

### 3. 招聘执行 → 岗位管道 (`/pipeline/board`)

选择岗位 **高级前端工程师**，各列应有候选人：

| 阶段 | 候选人 |
|------|--------|
| 筛选中 | 杨帆 |
| 沟通中 | —（Java 岗：王强、周婷在 job#2） |
| 面试中 | 李娜、马丽 |
| 待决策 | 张伟 |
| 录用中 | 陈浩 |
| 已入职 | —（数据分析师岗：刘洋在 job#4） |

点击候选人卡片 → 进入 360 页，**时间线**应有多条阶段变更记录。

### 4. 招聘执行 → 候选人 (`/pipeline/candidates`)

列表应显示：**公司、职位、学历中文、薪资 45K 格式**（非空白/45000K）。

推荐验证：张伟、陈浩、刘洋。

### 5. 人才储备 → 简历收件 (`/talent/resumes`)

| 预期 | 说明 |
|------|------|
| 总简历 ≥ 10 | 含已解析/待解析/失败 |
| 已解析 8 份 | 显示姓名、公司、职位 |
| 待解析 1 份 | unknown.pdf |
| 解析失败 1 份 | broken.pdf |

### 6. 人才储备 → 人才库 (`/talent/pool`)

搜索 **Vue3** 或 **张伟**，应命中候选人；卡片可跳转关联简历。

### 7. 招聘规划 → 岗位 → 寻源 (`/planning/jobs/1` 寻源 Tab)

| 预期 | 说明 |
|------|------|
| 活动运行中 | 高级前端工程师-渠道寻源 |
| 统计漏斗 | 已检索 156、已打招呼 45、已收简历 18 等 |
| 待人工确认 | 杨帆（确认打招呼）、王强（确认入库） |
| 候选人轨迹 | 7 条记录 |

### 8. 招聘执行 → Offer (`/pipeline/offers`)

| Offer | 候选人 | 状态 |
|-------|--------|------|
| #1 | 陈浩 | 已发送 SENT |
| #2 | 刘洋 | 已接受 ACCEPTED |
| #3 | 张伟 | 待审批 PENDING |

### 9. 招聘执行 → 入职 (`/pipeline/onboards`)

| 入职 | 候选人 | 状态 | 任务数 |
|------|--------|------|--------|
| #1 | 刘洋 | 已确认 | 7 项（含已完成上传） |
| #2 | 陈浩 | 待确认 | 3 项 |

### 10. 招聘规划 → 审批 (`/planning/approvals/pending`)

admin 登录应看到：
- 需求审批：产品经理（demand #3）
- （Offer 审批走 Offer 列表 PENDING 状态）

## 常见问题

**列表字段空白**  
确认已执行 `reseed-demo.sh` 且重启 candidate 服务；前端需刷新。

**管道列为空**  
确认执行了 `test-data-pipeline-seed.sql` 和 `test-data-linkage-seed.sql`。

**今日无面试**  
linkage seed 会将 interview #2/#3 调整到当天，需重新执行 seed。

**简历列表无姓名**  
需 candidate 服务包含 ResumeService 字段补全逻辑（已修复）。

---

## Phase 2 — 渠道运营 + 策略进化闭环（v5.1）

**前置**：已执行 `migration-v10-phase2-evolution-ops.sql` 与 `test-data-phase2-seed.sql`（`reseed-demo.sh` 已包含）；服务 agent(8091)、job(8084)、evolution(8090)、communication(8089) 已启动。

### P2-1. JD → 运营包 → 启动招聘

| 步骤 | 路径 | 预期 |
|------|------|------|
| 1 | `/planning/jobs/{id}` → 任职要求 | 生成渠道运营包 → **确认运营包** |
| 2 | 同页「寻源」Tab 或 `/planning/jobs/{id}/sourcing` | 选策略/配额 → 启动半自动 Campaign |
| 3 | 活动运行 | 平台执行步骤推进；轨迹表有筛选阶段 |

### P2-2. 轨迹与信号

| 预期 | 说明 |
|------|------|
| 筛选淘汰 | 轨迹列展示 `CARD` / `FULL_RESUME` 阶段 + 人话原因 |
| `evolution_signal` | 跑一轮后有 L3（筛选）、L5（打招呼/简历）记录 |
| 复聊（模拟） | 打招呼后约 2 分钟可触发 L4 复聊信号 |

### P2-3. 渠道暂存库

| 步骤 | 路径 | 预期 |
|------|------|------|
| COLLECT_ONLY 启动 | 寻源启动弹窗选「仅采集入库」 | 轨迹状态 `STAGED` |
| 暂存列表 | `/talent/channel-staging` | 列表、排序、**MiMo LLM 问答**（`recruitos-llm:8095` + `.env` 中 `MIMO_*`）、批量打招呼/入库/不合适 |
| 入库后 | 正式候选人 + 暂存状态 `IMPORTED` | 不可重复入库 |

### P2-4. 策略进化（G3 HR 确认）

| 步骤 | 路径 | 预期 |
|------|------|------|
| 积累信号 | 跑多轮 Campaign 或调低 `MIN_SIGNALS` | `evolution_proposal` 出现 PENDING |
| HR 待确认 | `/ai-tools/evolution/proposals` | 查看 diff → **确认采纳** 或驳回 |
| 新版本生效 | 仅**新** Campaign | 运行中活动仍用旧 `ops_pack_version` |

### P2-5. 健康回滚建议（G4）

| 预期 | 说明 |
|------|------|
| CRITICAL 健康 | `/ai-tools/evolution/health` 分数 &lt; 60 |
| 回滚 proposal | 类型 `ROLLBACK`，须 HR 确认，**不自动回滚** |

### Phase 2 端到端脚本（约 15 分钟）

**自动化（推荐）** — 启动全部服务后：

```bash
cd RecruitOS/recruitos-backend
./scripts/start-all-services.sh          # 含 llm(8095)
RESEED=1 ./scripts/verify-phase2-e2e.sh   # 可选重载 phase2 种子

# 仅重建近期改动的服务（进化/Agent/沟通/面试/LLM 等）
./scripts/restart-hot-services.sh

# 或仅跑 TestAgent
cd TestAgent && PYTHONPATH=src python3 -m test_agent.cli phase2
```

覆盖项：登录、运营包 ACTIVE、L3 信号、暂存库、MiMo ask-ai、面试评价 L2、进化建议 confirm → ops-pack v2、候选人回复 L4、Offer 接受 L1。许可 402 需将 `tenant_license.max_jobs` 降至当前岗位数后，在 `agent.yaml` 设 `phase2.test_license_402: true`。

### Phase 2.5 — 决策智能深化（PRD v6）

参考 [PRD-v6.md](./PRD-v6.md) Epic D1–D3。

| 页面 / 能力 | 路径 | 验收要点 |
|-------------|------|----------|
| 统一匹配分 | 候选人列表 / 决策面板 / Agent 轨迹 | 同一岗位+候选人，匹配分与 `JdTagMatcher` 标签对照一致（非固定 50 分启发式） |
| 进化健康 | `/ai-tools/evolution/health` | 顶部四指标来自 `/api/evolution/health/system`；岗位列表逐岗 `/health/job/{id}`；告警来自 `/health/alerts` |
| 进化权重 | `/ai-tools/evolution/weight` | 岗位下拉来自真实岗位 API；权重条来自 `/api/evolution/weight/{jobId}` 的 `tagsSnapshot`；历史来自 `/history` |
| 沟通决策树 | Agent 打招呼轨迹 | 竞品公司 / 初级（≤2年）/ 复聊 / 标准 四类开场文案不同（见 `ScriptDecisionTree`） |
| 分析漏斗 D4 | `/insight/analytics/funnel` | 渠道寻源四段 + 管道漏斗 + 渠道对比表（真实 onboard 入职数） |
| A/B 实验 | `/ai-tools/evolution/ab-test` | 列表/创建/启动/停止接 `/api/evolution/abtest/*`（需先跑 `migration-v11-ab-test-align.sql`） |
| 内推分享 | `/talent/referral` → 生成链接 | `POST /api/referral/link`；公开页 `/referral/submit/{token}` 投递后 `candidate.source=REFERRAL` |
| 内推奖励 | `/talent/referral/rewards` | 统计卡 + 审批/发放接 `/api/referral/reward/stats|approve|pay` |

**数据库迁移（Phase 2.5 续）**：

```bash
mysql -u root -p recruit_os < sql/migration-v11-ab-test-align.sql
mysql -u root -p recruit_os < sql/migration-v12-referral-share-link.sql
```

**UI 栈**：前端已全面迁移至 shadcn 风格组件（`npm run build` 通过），Element Plus 已移除。

**手动 UI 流程**：

```
1. JD 解析标签 → 生成运营包 → HR 确认
2. 启动渠道招聘（半自动 + SCREEN_THEN_GREET）
3. 查看轨迹拒绝原因 / 确认打招呼
4. 再启一轮 COLLECT_ONLY → 渠道暂存库批量入库
5. （可选）提交面试评价 → 等待进化建议
6. AI 工具 → 策略进化待确认 → 采纳 v2
7. 新建 Campaign 验证绑定 v2 运营包
8. 提交面试评价 → 检查 `evolution_signal` L2；Offer 接受/拒绝 → L1
9. 沟通对话候选人回复 API → L4；超额创建岗位/Agent → HTTP 402 hard block
```
