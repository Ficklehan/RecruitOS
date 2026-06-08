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
