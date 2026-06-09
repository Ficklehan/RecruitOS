# Phase 2 UX 剩余任务总览（v3 + v3.1）

**基准文档**：`PHASE2-UX-SPEC.md` · **更新**：P0 + P1 + P2 全部完成  
**目标**：跑通 15 分钟 Demo 全链路 + 主界面零工程词

---

## 进度快照

| 批次 | 状态 | 内容 |
|------|------|------|
| **P0-1** | ✅ 完成 | businessLabels v2 · 职位 4 Tab · PipelineKanban · 收件箱动词 Tab · 纳入仪式化 · 菜单改名 |
| **P0-2** | ✅ 本批完成 | 招人方式向导 · 3 步启动 · 建议页人话化 · 收件箱待确认 · JdEditor 迁移 |
| **P1** | ✅ 本批完成 | 面试评价 · 发起 Offer · 变更记录 · 租户开关 · 职位列表招人状态 · 总览双漏斗 · Offer→入职待办 · 需求漏斗联动 |
| **P2** | ✅ 本批完成 | 两轮面试 · 驾驶舱双漏斗 · Demo 种子 · MIN_SIGNALS · 审计导出 · 可用性测试文档 · CI 扫描 |

---

## P0-2 任务清单（当前冲刺）

| ID | 任务 | 文件/模块 | 验收 |
|----|------|-----------|------|
| P0-2.1 | ✅ | **招人方式 5 段向导** | `SourcingMethodWizard.vue` · `JobDetail.vue` |
| P0-2.2 | ✅ | **平台任务 3 步启动向导** | `JobSourcing.vue` |
| P0-2.3 | ✅ | **招人方式建议页人话化** | `EvolutionProposal.vue` · `opsPackSummary.ts` |
| P0-2.4 | ✅ | **收件箱「待你确认」聚合** | `workspace.ts` |
| P0-2.5 | ✅ | **JobSourcing 用语清扫** | `JobSourcing.vue` |
| P0-2.6 | ✅ | **JdEditor 招人方式面板迁移** | `JdEditor.vue` |
| P0-2.7 | ✅ | **job API 补全** | `job.ts` |

---

## P1 任务清单

| ID | 任务 | 说明 |
|----|------|------|
| P1-1 | ✅ | 候选人工作台「发起录用通知」 | `CandidateWorkspace.vue` |
| P1-2 | ✅ | 面试官结构化评价表单 | `InterviewEvalDrawer.vue` · Today · Inbox |
| P1-3 | ✅ | 职位规则 Tab「变更记录」时间线 | `JobAuditTimeline.vue` |
| P1-4 | ✅ | 租户设置「渠道招聘安全」开关 | `TenantSetting.vue` · `JobSourcing.vue` |
| P1-5 | ✅ | 在招职位列表招人状态列 | `JobList.vue` · `jobRecruitStatus.ts` |
| P1-6 | ✅ | 总览双漏斗渠道数据 | `JobDetail.vue` Campaign stats |
| P1-7 | ✅ | Offer→入职自动待办 | `workspace.ts` onboard items |
| P1-8 | ✅ | 需求详情 ↔ 职位漏斗联动 | `DemandDetail.vue` |

---

## P2 任务清单

| ID | 任务 | 说明 |
|----|------|------|
| P2-1 | ✅ | PRD 两轮面试完整状态机 | `JobHiringSummary.vue` · `InterviewEvalDrawer` · 评价模板 |
| P2-2 | ✅ | 工作台驾驶舱双漏斗 | `Dashboard.vue` · `dashboard.ts` |
| P2-3 | ✅ | Demo 种子 Phase2 扩展 | `test-data-phase2-seed.sql` · `reseed-demo.sh` |
| P2-4 | ✅ | 进化 MIN_SIGNALS 租户可配 | `EvolutionSettingsService` · `TenantSetting.vue` |
| P2-5 | ✅ | 审计 CSV 导出 | `JobAuditTimeline.vue` |
| P2-6 | ✅ | 5 人 15 分钟无培训测试 | `PHASE2-USABILITY-TEST.md` 脚本与记分表 |
| P2-7 | ✅ | FORBIDDEN_UI_TERMS CI | `npm run check:terms` |

---

## 依赖与顺序

```
P0-2.7 (API) → P0-2.1 (Wizard) → P0-2.2 (启动绑定版本)
P0-2.3 (建议页) ∥ P0-2.4 (收件箱)
P0-2.5 · P0-2.6 (清扫) 可在 Wizard 后批量
P1 依赖 P0-2 全部完成
P2 可与 P1 部分并行
```

---

## Demo 验收脚本（15 分钟）

1. 登录 zhangsan → 收件箱看动词 Tab  
2. 在招职位 → 高级前端 → **规则 → 招人方式** → 生成并确认  
3. **找人 → 平台招人任务** → 3 步启动  
4. 待你确认 → 确认联系 / 纳入候选人  
5. **管人 → 在招候选人** Kanban 初筛  
6. 管人 → 面试与录用 → 准备录用跳转  
7. 招人方式建议 → 查看 → 采纳（二次确认）  
8. 全路径无 OpsPack / CARD_GREET / evolution 外露

---

## 风险登记

| 风险 | 缓解 |
|------|------|
| 收件箱聚合 N+1 请求 | 限 10 个活跃职位；P1 后端 inbox API |
| Wizard 与 JdEditor 双维护 | P0-2.6 统一组件 |
| 编译环境 JDK/vue-tsc | 本地 dev 验证为主 |
