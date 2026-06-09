# RecruitOS UI 重构落地清单

## A. 系统层

### A1. Token 与样式收口
- 统一 [variables.scss](/Users/apple/Documents/Project/ZQ/RecruitOS/recruitos-frontend/src/assets/styles/variables.scss)
- 统一 [global.scss](/Users/apple/Documents/Project/ZQ/RecruitOS/recruitos-frontend/src/assets/styles/global.scss)
- 建立 4 级字号、6 档间距、3 档阴影、4 档圆角
- 建立统一状态标签语义映射

### A2. 页面骨架收口
- 统一使用 [PageShell.vue](/Users/apple/Documents/Project/ZQ/RecruitOS/recruitos-frontend/src/components/Layout/PageShell.vue) 或 [ListPageLayout.vue](/Users/apple/Documents/Project/ZQ/RecruitOS/recruitos-frontend/src/components/Layout/ListPageLayout.vue)
- 消除 `page-container page-stack` 的碎片化用法
- 定义并固定 5 类标准页面模板

### A3. 组件库补全
- PageHeader
- FilterBar
- StatsRow
- StatusTag
- EmptyState
- BoardCard
- ActionRail

---

## B. 模块层

### B1. 导航与全局壳
- 优化 [TopNav.vue](/Users/apple/Documents/Project/ZQ/RecruitOS/recruitos-frontend/src/components/Layout/TopNav.vue)
- 优化 [Sidebar.vue](/Users/apple/Documents/Project/ZQ/RecruitOS/recruitos-frontend/src/components/Layout/Sidebar.vue)
- 统一 [AppLayout.vue](/Users/apple/Documents/Project/ZQ/RecruitOS/recruitos-frontend/src/components/Layout/AppLayout.vue)
- 增加统一对象上下文条

### B2. 列表类页面
- [CandidateList.vue](/Users/apple/Documents/Project/ZQ/RecruitOS/recruitos-frontend/src/views/candidate/CandidateList.vue)
- [ResumeList.vue](/Users/apple/Documents/Project/ZQ/RecruitOS/recruitos-frontend/src/views/resume/ResumeList.vue)
- [OfferList.vue](/Users/apple/Documents/Project/ZQ/RecruitOS/recruitos-frontend/src/views/offer/OfferList.vue)
- [DemandList.vue](/Users/apple/Documents/Project/ZQ/RecruitOS/recruitos-frontend/src/views/demand/DemandList.vue)
- [ReferralList.vue](/Users/apple/Documents/Project/ZQ/RecruitOS/recruitos-frontend/src/views/referral/ReferralList.vue)
- [TenantList.vue](/Users/apple/Documents/Project/ZQ/RecruitOS/recruitos-frontend/src/views/platform/TenantList.vue)
- [UserManage.vue](/Users/apple/Documents/Project/ZQ/RecruitOS/recruitos-frontend/src/views/settings/UserManage.vue)
- [OrgManage.vue](/Users/apple/Documents/Project/ZQ/RecruitOS/recruitos-frontend/src/views/settings/OrgManage.vue)
- [RoleManage.vue](/Users/apple/Documents/Project/ZQ/RecruitOS/recruitos-frontend/src/views/settings/RoleManage.vue)

### B3. 看板类页面
- [InterviewBoard.vue](/Users/apple/Documents/Project/ZQ/RecruitOS/recruitos-frontend/src/views/interview/InterviewBoard.vue)
- [PipelineBoard.vue](/Users/apple/Documents/Project/ZQ/RecruitOS/recruitos-frontend/src/views/pipeline/PipelineBoard.vue)
- [DemandBoard.vue](/Users/apple/Documents/Project/ZQ/RecruitOS/recruitos-frontend/src/views/demand/DemandBoard.vue)

### B4. 工作台类页面
- [Dashboard.vue](/Users/apple/Documents/Project/ZQ/RecruitOS/recruitos-frontend/src/views/dashboard/Dashboard.vue)
- [CandidateWorkspace.vue](/Users/apple/Documents/Project/ZQ/RecruitOS/recruitos-frontend/src/views/candidate/CandidateWorkspace.vue)
- [JobDetail.vue](/Users/apple/Documents/Project/ZQ/RecruitOS/recruitos-frontend/src/views/planning/JobDetail.vue)
- [Today.vue](/Users/apple/Documents/Project/ZQ/RecruitOS/recruitos-frontend/src/views/workspace/Today.vue)
- [Inbox.vue](/Users/apple/Documents/Project/ZQ/RecruitOS/recruitos-frontend/src/views/workspace/Inbox.vue)
- [ChannelStaging.vue](/Users/apple/Documents/Project/ZQ/RecruitOS/recruitos-frontend/src/views/channel/ChannelStaging.vue)

### B5. 详情类页面
- [CandidateDetail.vue](/Users/apple/Documents/Project/ZQ/RecruitOS/recruitos-frontend/src/views/pipeline/CandidateDetail.vue)
- [OfferDetail.vue](/Users/apple/Documents/Project/ZQ/RecruitOS/recruitos-frontend/src/views/pipeline/OfferDetail.vue)
- [ResumeDetail.vue](/Users/apple/Documents/Project/ZQ/RecruitOS/recruitos-frontend/src/views/resume/ResumeDetail.vue)
- [TenantDetail.vue](/Users/apple/Documents/Project/ZQ/RecruitOS/recruitos-frontend/src/views/platform/TenantDetail.vue)
- [DemandDetail.vue](/Users/apple/Documents/Project/ZQ/RecruitOS/recruitos-frontend/src/views/demand/DemandDetail.vue)

### B6. 设置类页面
- [TenantSetting.vue](/Users/apple/Documents/Project/ZQ/RecruitOS/recruitos-frontend/src/views/settings/TenantSetting.vue)
- [SsoConfig.vue](/Users/apple/Documents/Project/ZQ/RecruitOS/recruitos-frontend/src/views/settings/SsoConfig.vue)
- [LicenseInfo.vue](/Users/apple/Documents/Project/ZQ/RecruitOS/recruitos-frontend/src/views/settings/LicenseInfo.vue)
- [CommunicationSafety.vue](/Users/apple/Documents/Project/ZQ/RecruitOS/recruitos-frontend/src/views/communication/CommunicationSafety.vue)

---

## C. 执行顺序

### P0
1. Dashboard
2. Job Workspace
3. CandidateWorkspace
4. CandidateList

### P1
5. ResumeList
6. ResumeUpload
7. InterviewBoard
8. OfferList
9. Inbox

### P2
10. DemandList / DemandBoard
11. PipelineBoard
12. CandidatePool
13. Settings pages

---

## D. 验收标准

- 所有核心页面遵循统一页面骨架
- 列表、工作台、详情、看板、配置页各有固定范式
- 状态标签、空状态、筛选区、表格节奏全系统统一
- 页面主操作清晰，不超过视觉主路径
- 不同页面之间切换不产生明显视觉跳跃

---

## E. 建议的工作方式

- 先改系统层，再改页面层
- 先改 P0 页面，再扩展到其他页面
- 每次页面重构都必须对照 `RECRUITOS-UI-REDESIGN-SPEC.md`
- 避免页面单独发明新的布局模式

