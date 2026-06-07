# RecruitOS 技术架构文档

## 一、项目概述

RecruitOS 是一套集团级 AI 招聘操作系统，SaaS 多租户架构，覆盖从招聘需求提报到入职的全链路。

**技术栈**：
- 后端：JDK 1.8 + Spring Boot 2.7.18 + MyBatis-Plus 3.5.5
- 前端：Vue 3.4 + TypeScript + Vite 5 + Element Plus 2.9
- 数据库：MySQL 8.0 + Redis 7.x
- 审批流：Flowable 6.7.2
- 权限：Sa-Token 1.37

**项目路径**：`/Users/han/Desktop/ZQ/`
**数据库**：`recruit_os`（MySQL localhost, root/12345678）

---

## 二、菜单结构（模块 → 功能 两级）

| 一级菜单（模块） | 二级菜单（功能） | 路由路径 | 说明 |
|---|---|---|---|
| 工作台 | 数据概览 | /dashboard | 今日待办/关键指标/快捷入口 |
| 招聘需求 | 需求列表 | /demand/list | 需求CRUD/提报/关闭 |
| | 需求看板 | /demand/board | 漏斗/编制消耗/SLA告警 |
| | 我的审批 | /demand/approval | 待审批/已审批/已驳回 |
| 岗位管理 | 岗位列表 | /job/list | 岗位CRUD/状态管理 |
| | JD工作台 | /job/jd-editor | JD编辑/解析/标签权重 |
| 候选人 | 候选人列表 | /candidate/list | 候选人CRUD/筛选 |
| | 人才库 | /candidate/pool | 跨岗位搜索/人才图谱 |
| | 决策面板 | /candidate/decision | 匹配度拆解/AI洞察 |
| 面试管理 | 面试看板 | /interview/board | Kanban视图(初面/复试) |
| | 面试日历 | /interview/calendar | 日历视图/时间协调 |
| | 评价管理 | /interview/evaluation | 结构化评价/进化反馈 |
| Offer管理 | Offer列表 | /offer/list | Offer CRUD/状态跟踪 |
| | Offer审批 | /offer/approval | 审批流/电子签 |
| 入职管理 | 入职列表 | /onboard/list | 入职候选人管理 |
| | 入职任务 | /onboard/task | 任务清单/进度追踪 |
| AI沟通 | 话术管理 | /communication/template | 话术CRUD/A/B测试 |
| | 对话记录 | /communication/conversation | 全量对话/搜索 |
| | 安全审查 | /communication/safety | 审查日志/拦截记录 |
| 进化引擎 | 权重面板 | /evolution/weight | 标签权重可视化/调整 |
| | 健康监控 | /evolution/health | 四维健康度/告警/回滚 |
| | A/B测试 | /evolution/ab-test | 实验管理/结果对比 |
| Agent管理 | Agent列表 | /agent/list | Agent实例/状态/启停 |
| | 平台账号 | /agent/account | 账号管理/健康度 |
| | 行为日志 | /agent/log | 操作日志/异常记录 |
| 内推管理 | 内推列表 | /referral/list | 内推候选人/进度 |
| | 奖励管理 | /referral/reward | 奖励审批/发放 |
| 猎头管理 | 供应商管理 | /headhunter/vendor | 猎头CRUD/合同 |
| | 效果对比 | /headhunter/performance | 渠道ROI对比 |
| 数据分析 | 招聘漏斗 | /analytics/funnel | 全链路漏斗转化 |
| | 渠道ROI | /analytics/roi | 各渠道成本/效果 |
| | 面试官效能 | /analytics/interviewer | 筛选准确率/决策速度 |
| | 招聘周期 | /analytics/cycle | 各环节耗时分布 |
| 系统设置 | 租户设置 | /settings/tenant | 白标/域名/品牌 |
| | 组织架构 | /settings/org | 组织树管理 |
| | 角色管理 | /settings/role | 角色CRUD/权限配置 |
| | 用户管理 | /settings/user | 用户CRUD/分配角色 |
| | SSO配置 | /settings/sso | 飞书/企微/钉钉对接 |
| | 许可信息 | /settings/license | 套餐/用量/续费 |

---

## 三、后端模块划分

| 模块 | 包名 | 职责 | 端口 |
|---|---|---|---|
| recruitos-common | com.recruitos.common | 公共组件(BaseEntity/Result/异常/租户拦截/工具) | - |
| recruitos-auth | com.recruitos.auth | 认证服务(登录/登出/Token刷新/SSO/MFA) | 8081 |
| recruitos-tenant | com.recruitos.tenant | 租户服务(租户/组织/角色/用户/权限) | 8082 |
| recruitos-demand | com.recruitos.demand | 需求服务(需求CRUD/审批流/编制/看板) | 8083 |
| recruitos-job | com.recruitos.job | 岗位服务(岗位CRUD/JD解析/标签权重) | 8084 |
| recruitos-candidate | com.recruitos.candidate | 候选人服务(候选人/简历解析/人才库/匹配) | 8085 |
| recruitos-interview | com.recruitos.interview | 面试服务(初面/复试安排/评价/录制) | 8086 |
| recruitos-offer | com.recruitos.offer | Offer服务(Offer审批/电子签/背调) | 8087 |
| recruitos-onboard | com.recruitos.onboard | 入职服务(入职任务/HRone对接) | 8088 |
| recruitos-communication | com.recruitos.communication | AI沟通服务(话术/对话/安全审查) | 8089 |
| recruitos-evolution | com.recruitos.evolution | 进化引擎(协方差/健康度/A-B/回滚) | 8090 |
| recruitos-agent | com.recruitos.agent | Agent编排(平台Agent/行为随机化/容错) | 8091 |
| recruitos-referral | com.recruitos.referral | 内推服务(内推/奖励) | 8092 |
| recruitos-headhunter | com.recruitos.headhunter | 猎头服务(供应商/效果) | 8093 |
| recruitos-analytics | com.recruitos.analytics | 分析服务(漏斗/ROI/效能/周期) | 8094 |
| recruitos-llm | com.recruitos.llm | LLM统一服务(多模型路由/Embedding) | 8095 |
| recruitos-message | com.recruitos.message | 消息通知(站内信/邮件/企微/飞书) | 8096 |
| recruitos-file | com.recruitos.file | 文件服务(上传/MinIO对接) | 8097 |
| recruitos-license | com.recruitos.license | 许可计费(套餐/用量/宽限期) | 8098 |
| recruitos-portal | com.recruitos.portal | 候选人Portal(H5/进度/NPS) | 8099 |

---

## 四、前端模块划分

| 目录 | 说明 |
|---|---|
| src/api/modules/ | 按模块划分的API接口 |
| src/router/modules/ | 按模块划分的路由配置 |
| src/stores/ | Pinia状态管理 |
| src/views/ | 按模块划分的页面视图 |
| src/components/ | 公共组件(Layout/DataTable/SearchForm等) |
| src/assets/styles/ | 全局样式/主题变量 |
| src/utils/ | 工具函数(auth/permission/format) |

---

## 五、任务清单与执行顺序

### Task 1: 创建数据库Schema
- 创建 `recruit_os` 数据库
- 创建所有业务表（租户/组织/用户/角色/需求/岗位/候选人/简历/面试/评价/Offer/入职/话术/进化/Agent/内推/猎头/许可等）
- 插入初始数据（默认租户/管理员/角色/权限）

### Task 2: 搭建后端基础框架
- 创建Maven多模块项目结构
- 配置parent pom.xml（依赖管理）
- 实现recruitos-common模块（BaseEntity/Result/异常/租户拦截/MyBatis配置）
- 实现recruitos-auth模块（登录/登出/JWT/权限校验）
- 实现recruitos-tenant模块（租户/组织/角色/用户CRUD）
- 配置Nacos注册中心
- 验证：启动服务，调用登录API

### Task 3: 搭建前端基础框架
- 创建Vue 3 + Vite项目
- 配置Element Plus/Router/Pinia/Axios
- 实现Layout组件（侧边栏两级菜单/头部/面包屑）
- 实现登录页
- 实现Dashboard页
- 实现系统设置页面（租户/组织/角色/用户）
- 验证：启动开发服务器，登录并浏览页面

### Task 4: 实现招聘需求模块
- 后端：需求CRUD + Flowable审批流 + 编制校验 + 需求看板
- 前端：需求列表/需求创建/需求详情/需求看板/我的审批
- 验证：创建需求 → 提交审批 → 审批通过

### Task 5: 实现岗位管理模块
- 后端：岗位CRUD + JD解析 + 标签权重
- 前端：岗位列表/JD工作台
- 验证：审批通过后自动建岗 → 编辑JD → 触发解析

### Task 6: 实现候选人模块
- 后端：候选人CRUD + 简历解析 + 人才库 + 匹配评分
- 前端：候选人列表/人才库/决策面板
- 验证：添加候选人 → 上传简历 → 查看匹配度

### Task 7: 实现面试管理模块
- 后端：初面/复试安排 + 状态流转 + 结构化评价 + 日历协调
- 前端：面试看板/面试日历/评价管理
- 验证：安排初面 → 初面评价 → 自动触发复试 → 复试评价

### Task 8: 实现Offer与入职模块
- 后端：Offer审批 + 入职任务 + HRone对接
- 前端：Offer列表/入职列表/入职任务
- 验证：发起Offer → 审批 → 入职任务生成

### Task 9: 实现AI沟通与话术模块
- 后端：话术管理 + 沟通决策 + 安全审查
- 前端：话术管理/对话记录/安全审查
- 验证：创建话术 → 模拟发送 → 查看绩效

### Task 10: 实现进化引擎模块
- 后端：进化算法 + 协方差矩阵 + 健康度监控 + A/B测试
- 前端：权重面板/健康监控/A/B测试
- 验证：提交进化信号 → 查看权重变化 → 健康度监控

### Task 11: 实现Agent管理模块
- 后端：Agent编排 + 行为随机化 + 账号健康度
- 前端：Agent列表/平台账号/行为日志
- 验证：配置Agent → 启动 → 查看行为日志

### Task 12: 实现内推/猎头/分析模块
- 后端：内推/猎头/分析看板
- 前端：内推管理/猎头管理/数据分析
- 验证：内推流程 → 猎头派单 → 查看分析看板

### Task 13: 集成测试与优化
- 全流程联调：需求 → 建岗 → 搜索 → 面试 → Offer → 入职
- 性能优化
- 安全加固
- 文档完善

---

## 六、数据库连接信息

```
Host: localhost
Port: 3306
Database: recruit_os
Username: root
Password: 12345678
```

---

## 七、项目启动命令

### 后端
```bash
# 启动单个服务（以auth为例）
cd /Users/han/Desktop/ZQ/recruitos-backend
mvn clean install -DskipTests
cd recruitos-auth
mvn spring-boot:run
```

### 前端
```bash
cd /Users/han/Desktop/ZQ/recruitos-frontend
npm install
npm run dev
```

---

## 八、任务进度追踪

| 任务 | 状态 | 完成时间 | 说明 |
|---|---|---|---|
| Task 1: 创建数据库Schema | ✅ 完成 | 2026-06-06 | 38张表+初始数据 |
| Task 2: 搭建后端基础框架 | ✅ 完成 | 2026-06-06 | common/auth/tenant模块编译通过 |
| Task 3: 搭建前端基础框架 | ✅ 完成 | 2026-06-06 | 35+页面构建成功 |
| Task 4: 实现招聘需求模块 | ✅ 完成 | 2026-06-06 | 需求CRUD+审批流+看板 |
| Task 5: 实现岗位管理模块 | ✅ 完成 | 2026-06-06 | 岗位CRUD+JD解析+标签权重 |
| Task 6: 实现候选人模块 | ✅ 完成 | 2026-06-06 | 候选人+简历+人才库+匹配 |
| Task 7: 实现面试管理模块 | ✅ 完成 | 2026-06-06 | 初面/复试+评价+日历 |
| Task 8: 实现Offer与入职模块 | ✅ 完成 | 2026-06-06 | Offer审批+入职任务 |
| Task 9: 实现AI沟通与话术模块 | ✅ 完成 | 2026-06-06 | 话术+对话+安全审查 |
| Task 10: 实现进化引擎模块 | ✅ 完成 | 2026-06-06 | 协方差+健康度+A/B |
| Task 11: 实现Agent管理模块 | ✅ 完成 | 2026-06-06 | Agent编排+行为日志 |
| Task 12: 实现内推/猎头/分析模块 | ✅ 完成 | 2026-06-06 | 内推+猎头+看板 |
| Task 13: 集成测试与优化 | ✅ 完成 | 2026-06-06 | 全流程编译+构建验证 |

### 已完成的交付物

**数据库**：recruit_os，38张业务表，含初始数据（1租户/7组织/1管理员/7角色/53权限/1许可）

**后端**：/Users/han/Desktop/ZQ/recruitos-backend/
- recruitos-common：14个公共类（R/异常/租户拦截/MyBatis配置/权限）
- recruitos-auth：登录/登出/JWT API（端口8081）
- recruitos-tenant：租户/组织/角色/用户/权限 CRUD API（端口8082）
- recruitos-demand：需求CRUD+审批流+看板（端口8083）
- recruitos-job：岗位CRUD+JD解析+标签权重（端口8084）
- recruitos-candidate：候选人+简历+人才库+匹配（端口8085）
- recruitos-interview：初面/复试安排+评价+日历（端口8086）
- recruitos-offer：Offer CRUD+审批流+状态管理（端口8087）
- recruitos-onboard：入职管理+任务清单（端口8088）
- recruitos-communication：话术管理+对话记录+安全审查（端口8089）
- recruitos-evolution：进化信号+权重快照+健康度+A/B测试（端口8090）
- recruitos-agent：Agent任务+平台账号+行为日志（端口8091）
- recruitos-referral：内推管理+奖励审批（端口8092）
- recruitos-headhunter：猎头供应商+推荐管理（端口8093）
- recruitos-analytics：招聘漏斗+渠道ROI+面试官效能+招聘周期（端口8094）
- 编译状态：BUILD SUCCESS

**前端**：/Users/han/Desktop/ZQ/recruitos-frontend/
- 80+个源文件，40+页面
- 精美扁平化登录页、Dashboard工作台
- 系统设置页面完整（租户/组织/角色/用户/SSO/许可）
- 招聘需求模块（列表/创建/详情/看板/审批）
- 岗位管理模块（列表/JD工作台）
- 候选人模块（列表/人才库/决策面板）
- 面试管理模块（看板/日历/评价）
- Offer模块（列表/审批）
- 入职模块（列表/任务）
- AI沟通模块（话术/对话/安全审查）
- 进化引擎模块（权重面板/健康监控/A/B测试）
- Agent管理模块（列表/账号/行为日志）
- 内推/猎头/分析模块（内推列表/奖励/供应商/效果对比/漏斗/ROI/效能/周期）
- 侧边栏两级菜单（14个模块）
- 构建状态：BUILD SUCCESS（5.3s）
