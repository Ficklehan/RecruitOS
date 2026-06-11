# 维度 A：数据层改造方案

**改造目标**：brain 服务从"硬编码模拟数据"升级为"接入真实微服务数据"，所有 AI 结果持久化，LLM 调用可靠化，重计算异步化。

---

## A1. 真实数据接入：Brain 服务接入各领域微服务

### 现状问题

`BrainDataAggregator` 每个方法返回硬编码 Map：
```java
m.put("replySpeed", 0.6);        // 写死的
m.put("questionsDepth", 0.5);    // 写死的
m.put("interviewEngagement", 0.7); // 写死的
```

这不是"数据聚合"，这是"数据捏造"。8 个引擎基于假数据计算，所有前端页面展示假结果。

### 改造方案

#### A1.1 引入 Feign 客户端层

在 `recruitos-brain` 中新建 `client/` 包，为每个需要数据的微服务建立 Feign 接口：

```
recruitos-brain/src/main/java/com/recruitos/brain/client/
├── CandidateClient.java        → recruitos-candidate (8085)
├── InterviewClient.java        → recruitos-interview (8086)
├── CommunicationClient.java    → recruitos-communication (8089)
├── OfferClient.java            → recruitos-offer (8087)
├── EvolutionClient.java        → recruitos-evolution (8090)
├── JobClient.java              → recruitos-job (8084)
├── DemandClient.java           → recruitos-demand (8083)
└── AnalyticsClient.java        → recruitos-analytics (8094)
```

**示例：CandidateClient**
```java
@FeignClient(name = "recruitos-candidate", url = "${service.candidate.url}")
public interface CandidateClient {
    @GetMapping("/api/candidate/{id}")
    R<CandidateDetailVO> getDetail(@PathVariable Long id);

    @GetMapping("/api/candidate/{id}/pipeline-status")
    R<PipelineStatusVO> getPipelineStatus(@PathVariable Long id);

    @GetMapping("/api/candidate/{id}/skills")
    R<List<SkillVO>> getSkills(@PathVariable Long id);
}
```

#### A1.2 改造 BrainDataAggregator

聚合器不再返回 mock 数据，而是：
1. 并行调用多个 Feign 客户端
2. 聚合结果
3. 缓存热点数据（Redis，TTL 5 分钟）

**示例：fetchIntentSignals 的真实实现逻辑**
```java
public Map<String, Object> fetchIntentSignals(Long candidateId, Long jobId) {
    // 1. 从 communication 微服务获取最近 N 条消息的时间戳 → 计算回复速度
    List<MessageVO> messages = communicationClient.getRecentMessages(candidateId, 20);
    double replySpeed = calculateReplySpeed(messages);

    // 2. 从 candidate 微服务获取简历 + 面试评价文本 → NLP 分析提问深度
    String resumeText = candidateClient.getParsedResume(candidateId).getContent();
    double questionsDepth = nlpAnalyzer.analyzeDepth(resumeText, messages);

    // 3. 从 interview 微服务获取面试出勤 + 评价 → 计算投入度
    List<InterviewVO> interviews = interviewClient.getByCandidate(candidateId);
    double engagement = calculateEngagement(interviews);

    // 4. 从 offer 微服务获取薪酬预期 vs 岗位预算 → 计算差距
    SalaryExpectationVO expectation = candidateClient.getSalaryExpectation(candidateId);
    JobBudgetVO budget = jobClient.getBudget(jobId);
    double salaryGap = calculateSalaryGap(expectation, budget);

    // 5. 返回真实信号
    Map<String, Object> signals = new LinkedHashMap<>();
    signals.put("replySpeed", replySpeed);
    signals.put("questionsDepth", questionsDepth);
    signals.put("interviewEngagement", engagement);
    signals.put("salaryGapPercent", salaryGap);
    return signals;
}
```

#### A1.3 数据接入优先级

| 数据需求 | 来源微服务 | 用于触点 | 优先级 |
|----------|-----------|----------|--------|
| 候选人管道状态 | candidate (8085) | 4, 5, 6 | P0 |
| 面试评价历史 | interview (8086) | 2, 3, 7 | P0 |
| 沟通消息时间戳 | communication (8089) | 4 | P0 |
| Offer 薪资数据 | offer (8087) | 4, 6 | P0 |
| 岗位标签体系 | job (8084) | 1, 5, 8 | P0 |
| 进化 L1-L6 信号 | evolution (8090) | 4, 5, 7, 8 | P1 |
| 分析漏斗数据 | analytics (8094) | 5 | P1 |
| 需求审批历史 | demand (8083) | 1 | P2 |

---

## A2. 持久化：7 张 AI 数据表

### 现状问题

所有 domain 对象（DemandDiagnosis、CandidateIntent、CalibrationSession 等）只存在于 Java 内存，HTTP 响应后即丢弃。无法回溯历史、无法对比版本、无法训练模型。

### 新增表结构

#### brain_demand_diagnosis（需求诊断历史）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK | |
| tenant_id | BIGINT | 租户隔离 |
| demand_id | BIGINT | 关联需求 |
| business_objective | TEXT | 业务目标原文 |
| diagnosis_json | JSON | 完整诊断结果 (DemandDiagnosis JSON) |
| confidence | DOUBLE | 置信度 |
| llm_model | VARCHAR(50) | 使用的 LLM 模型版本 |
| llm_prompt_hash | VARCHAR(64) | Prompt 版本哈希 |
| status | VARCHAR(20) | GENERATED / ACCEPTED / REJECTED / EXPIRED |
| accepted_by | BIGINT | 采纳人 |
| created_at | DATETIME | |

#### brain_candidate_intent（意向评分历史）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK | |
| tenant_id | BIGINT | |
| candidate_id | BIGINT | |
| job_id | BIGINT | |
| intent_score | DOUBLE | 0-100 |
| intent_level | VARCHAR(10) | HIGH / MEDIUM / LOW |
| feature_vector_json | JSON | 特征值快照（用于模型训练） |
| model_version | VARCHAR(30) | 模型版本 |
| confidence | DOUBLE | |
| created_at | DATETIME | |

#### brain_calibration_session（校准会记录）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK | |
| tenant_id | BIGINT | |
| candidate_id | BIGINT | |
| job_id | BIGINT | |
| evaluators_json | JSON | 参与面试官及其评分 |
| dimension_scores_json | JSON | 各维度统计（均值/方差/最大分歧） |
| cohen_kappa_json | JSON | 各维度 Cohen's Kappa 值 |
| moderator_script | TEXT | AI 主持脚本 |
| final_decision | VARCHAR(30) | 最终录用决定 |
| consensus_score | DOUBLE | 共识分 |
| created_at | DATETIME | |

#### brain_interviewer_profile（面试官质量档案）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK | |
| tenant_id | BIGINT | |
| interviewer_id | BIGINT | |
| total_evaluations | INT | 累计评价数 |
| leniency_index | DOUBLE | 宽松指数（相对全局均值） |
| avg_score | DOUBLE | 平均评分 |
| prediction_accuracy | DOUBLE | 预测准确度（评价 vs 入职绩效） |
| bias_tags_json | JSON | 偏差标签列表 |
| quality_score | DOUBLE | 综合质量评分 |
| needs_recertification | TINYINT | 是否需要重新认证 |
| trend_snapshot_json | JSON | 最近 6 周期趋势数据 |
| last_evaluated_at | DATETIME | 最后评估时间 |
| updated_at | DATETIME | |

#### brain_cycle_prediction（周期预测快照）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK | |
| tenant_id | BIGINT | |
| job_id | BIGINT | |
| estimated_days | INT | 预计天数 |
| risk_level | VARCHAR(10) | LOW / MEDIUM / HIGH |
| bottleneck_json | JSON | 瓶颈详情 |
| intervention_json | JSON | 干预建议 |
| pipeline_snapshot_json | JSON | 当时的管道数据快照 |
| created_at | DATETIME | |

#### brain_talent_density_snapshot（人才密度快照）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK | |
| tenant_id | BIGINT | |
| org_id | BIGINT | |
| density_score | DOUBLE | 密度评分 |
| heatmap_json | JSON | 能力热力图 |
| bar_raiser_verdict | TEXT | Bar Raiser 判断 |
| snapshot_period | VARCHAR(20) | 快照周期 (2026-Q3) |
| created_at | DATETIME | |

#### brain_ai_decision_log（AI 决策审计日志）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK | |
| tenant_id | BIGINT | |
| touchpoint | VARCHAR(30) | 触点编号 (DEMAND_DIAGNOSIS / INTENT / CALIBRATION 等) |
| target_type | VARCHAR(20) | CANDIDATE / JOB / INTERVIEW / OFFER |
| target_id | BIGINT | |
| ai_output_json | JSON | AI 完整输出 |
| ai_confidence | DOUBLE | AI 置信度 |
| user_action | VARCHAR(20) | ACCEPTED / IGNORED / OVERRIDDEN |
| user_id | BIGINT | |
| override_reason | VARCHAR(255) | 覆盖/忽略原因 |
| business_outcome | VARCHAR(30) | 最终业务结果（如 OFFER_ACCEPTED / HIRED_90D_PASS） |
| outcome_recorded_at | DATETIME | 结果记录时间 |
| created_at | DATETIME | |

---

## A3. LLM 调用可靠化

### 现状问题

手动 `indexOf('{')` + `lastIndexOf('}')` 解析 LLM 返回的 JSON —— 生产环境不可靠。

### 改造方案

#### A3.1 Structured Output（首选）

如果 LLM 支持 function calling 或 structured output：
```java
LlmStructuredRequest req = new LlmStructuredRequest();
req.setSystemPrompt("...");
req.setUserPrompt("...");
req.setOutputSchema(DemandDiagnosis.class);  // 指定输出 JSON Schema
DemandDiagnosis result = llmClient.chatStructured(req, DemandDiagnosis.class);
```

#### A3.2 JSON Schema 校验 + 字段级 Fallback（兜底）

```java
public DemandDiagnosis parseDiagnosis(String llmRaw) {
    try {
        String json = extractJson(llmRaw);  // 用正则提取，非 indexOf
        DemandDiagnosis diag = objectMapper.readValue(json, DemandDiagnosis.class);
        // Schema 校验
        validateOrFallback(diag);
        return diag;
    } catch (Exception e) {
        log.warn("LLM parse failed, using rules-based fallback", e);
        return rulesBasedDiagnosis();  // 规则兜底
    }
}
```

#### A3.3 重试 + 指数退避

```java
@Retryable(maxAttempts = 3, backoff = @Backoff(delay = 1000, multiplier = 2))
public String callLlm(LlmChatRequest req) { ... }
```

#### A3.4 LLM Trace 记录

每次 LLM 调用记录到 `brain_llm_trace` 表：
- 输入的 prompt 哈希
- 输出的原始文本
- 解析成功/失败
- 耗时
- 用于后续 prompt 优化和成本分析

---

## A4. 异步计算

### 现状问题

所有 AI 计算都是同步 HTTP 请求。校准分析可能需要比较 5 个面试官的 20 个维度评分，这些计算不应阻塞用户。

### 改造方案

#### A4.1 异步任务模式

对于耗时 > 500ms 的计算，采用「提交 → 轮询」模式：

```
POST /api/brain/calibration/{candidateId}/analyze
  → 返回 { taskId: "cal-123", status: "PROCESSING" }

GET /api/brain/tasks/{taskId}
  → 返回 { status: "COMPLETED", result: { ... } }
      或 { status: "PROCESSING", progress: 0.6 }
      或 { status: "FAILED", error: "..." }
```

#### A4.2 消息队列引入

```
信号发生（面试评价提交、Offer 接受）
  → recruitos-evolution 发射事件到 Kafka/RabbitMQ
  → recruitos-brain 消费事件
  → 异步更新意向评分、面试官档案、人才密度
```

**事件类型**：

| 事件 | 来源 | 触发更新 |
|------|------|----------|
| `interview.evaluation.submitted` | interview 8086 | 触点 7（面试官质量档案）+ 触点 2（面试辅助信号） |
| `offer.accepted` | offer 8087 | 触点 4（意向预测标签） |
| `offer.rejected` | offer 8087 | 触点 4（意向预测标签） |
| `candidate.message.sent` | communication 8089 | 触点 4（回复速度更新） |
| `onboard.probation.passed` | onboard 8088 | 触点 7（面试官预测准确度）+ 触点 8（人才密度） |
| `job.status.changed` | job 8084 | 触点 5（周期预测触发） |

#### A4.3 缓存策略

| 数据类型 | 缓存位置 | TTL | 刷新机制 |
|----------|----------|-----|----------|
| 候选人管道状态 | Redis | 1 分钟 | 事件触发刷新 |
| 面试官质量档案 | Redis | 1 小时 | 新评价提交后刷新 |
| 人才密度快照 | Redis | 24 小时 | 定时任务 + 事件触发 |
| LLM 需求诊断结果 | Redis | 同需求生命周期 | 需求变更时失效 |
| 周期预测 | Redis | 1 小时 | 管道变更时刷新 |

---

## A5. 数据层改造验收标准

- [ ] BrainDataAggregator 不再包含任何硬编码数据，全部通过 Feign 客户端获取
- [ ] 7 张新表在 `recruit_os` 数据库中创建完毕
- [ ] 每个触点引擎的结果写入对应持久化表
- [ ] LLM 调用增加 JSON Schema 校验 + 重试 + trace 记录
- [ ] 耗时 > 500ms 的计算异步化
- [ ] 消息队列消费 3 个以上事件类型
- [ ] 核心数据查询响应时间 < 200ms（缓存命中）
