# 维度 B：模型层改造方案

**改造目标**：从 if-else 规则引擎升级为统计学习模型，每个引擎有明确的训练数据、特征工程、评估指标、可解释性。

---

## B1. 意向预测：LightGBM 二分类模型

### 现状

```java
if (speed < 0.5) { score -= 15; }
else if (speed < 1.0) { score += 5; }
else { score += 10; }
```

这是硬编码规则，不是 AI。阈值和权重完全主观。

### 改造方案

#### B1.1 模型选型：LightGBM

选择 LightGBM 的理由：
- 表格数据上性能优异，与 XGBoost 相当
- 原生支持类别特征（候选人来源渠道、岗位级别等）
- 训练速度快，适合定期重训练
- 有成熟的 Java 绑定（通过 PMML 或 ONNX 导出）

#### B1.2 训练数据

**Label 定义**：Offer 接受 = 1（正样本），Offer 拒绝 = 0（负样本）

**数据来源**：`recruitos-offer` 微服务的 Offer 历史记录，取近 12 个月、同租户或同行业的数据。

**最小样本量**：
- 冷启动（< 50 样本）：使用行业模板模型
- 过渡期（50–200 样本）：行业模型 + 租户微调
- 成熟期（> 200 样本）：租户专属模型

#### B1.3 特征工程

| 特征类别 | 特征名 | 类型 | 数据来源 | 说明 |
|----------|--------|------|----------|------|
| **通信特征** | `reply_speed_avg` | float | communication 8089 | 候选人平均回复间隔（小时） |
| | `reply_speed_trend` | float | communication 8089 | 回复速度趋势（加速/减速） |
| | `message_length_avg` | int | communication 8089 | 候选人消息平均长度（字符） |
| | `question_count` | int | communication 8089 | 候选人主动提问的数量 |
| | `question_types` | categorical | NLP 分析 | 提问类型分布（薪资/业务/团队/技术） |
| **行为特征** | `interview_attendance` | float | interview 8086 | 面试出席率 |
| | `interview_reschedule_count` | int | interview 8086 | 改期次数 |
| | `interview_late_minutes` | int | interview 8086 | 迟到分钟数（平均） |
| **简历特征** | `past_tenure_avg` | float | candidate 8085 | 历史平均在职时长（月） |
| | `job_hop_count_3y` | int | candidate 8085 | 近 3 年跳槽次数 |
| | `current_company_tier` | categorical | candidate 8085 | 当前公司级别（BAT/TMD/其他） |
| **匹配特征** | `match_score` | float | candidate 8085 | 岗位匹配分 |
| | `match_score_vs_avg` | float | candidate 8085 | 匹配分 vs 该岗位候选人平均分 |
| **薪资特征** | `salary_gap_pct` | float | offer 8087 | 候选人期望 vs 岗位预算差距（%） |
| | `current_salary_percentile` | float | offer 8087 | 期望薪酬在同级别的分位数 |
| **进度特征** | `pipeline_stage_days` | int | candidate 8085 | 从初筛到当前阶段的天数 |
| | `stages_completed` | int | candidate 8085 | 已完成的阶段数 |
| **竞品特征** | `competing_offers_flag` | bool | 人工标注 | 是否有竞品 Offer |
| | `market_hotness` | float | 外部数据 | 该岗位当前市场招聘热度 |

#### B1.4 模型评估

| 指标 | 目标值 | 说明 |
|------|--------|------|
| AUC | ≥ 0.75 | 区分度 |
| Precision@30% | ≥ 0.60 | 对前 30% 高意向候选人的预测准确率 |
| Recall@30% | ≥ 0.70 | 对前 30% 高意向候选人的召回率 |

#### B1.5 模型产出

不只是 "意向 75 分"，而是：

```json
{
  "intent_score": 75,
  "intent_level": "MEDIUM",
  "confidence": 0.82,
  "top_positive_factors": [
    {"feature": "面试投入度", "contribution": +18, "evidence": "面试中主动追问3个深度问题"},
    {"feature": "回复速度", "contribution": +12, "evidence": "平均回复间隔4小时"}
  ],
  "top_negative_factors": [
    {"feature": "薪资差距", "contribution": -15, "evidence": "期望薪资高于预算22%"},
    {"feature": "跳槽频率", "contribution": -8, "evidence": "近3年3次跳槽"}
  ],
  "interventions": [
    {"action": "安排Hiring Manager沟通业务愿景", "button": "schedule_hm_chat"},
    {"action": "评估薪酬弹性空间", "button": "review_budget"}
  ]
}
```

每个特征贡献来自 SHAP 值，让 HR 理解"为什么是这个分数"。

#### B1.6 模型生命周期

```
训练 → 验证 → 部署（PMML/ONNX）
  → 在线推理（Java 加载模型文件）
  → 结果记录（brain_ai_decision_log）
  → 业务结果回流（Offer 接受/拒绝）
  → 定期重训练（每月或累积 100 个新样本时）
```

---

## B2. 校准分析：统计方法替代简单分差

### 现状

```java
d.setDisputed(d.getMaxGap() >= 2);  // 分差 ≥ 2 = 争议
```

太粗糙。没有区分"信号差异"和"标准差异"。

### 改造方案

#### B2.1 核心统计指标

**Cohen's Kappa（评分者间信度）**：

测量两个面试官评分的一致性程度，修正了随机一致的概率。

```
Kappa = (Po - Pe) / (1 - Pe)

Po = 观察一致率（两个面试官打相同分数的比例）
Pe = 期望一致率（随机情况下的预期一致率）
```

解读：
- Kappa > 0.75：优秀一致性
- Kappa 0.40–0.75：中等一致性
- Kappa < 0.40：一致性差，需要校准

**ICC（Intraclass Correlation Coefficient）**：

当有 2 个以上面试官时，ICC 比 Kappa 更适合。双向随机效应模型。

**评分分布对比**：

对每个面试官 × 每个维度：
- 计算评分分布（均值、标准差）
- 与全局分布做 KS 检验（Kolmogorov-Smirnov）
- 如果 KS 检验 p < 0.05，标记为"可能存在系统偏差"

#### B2.2 区分两种评分差异

| 差异类型 | 特征 | 处理 |
|----------|------|------|
| **信号差异** | A 面试官有具体的新证据（候选人说了某件事 B 没问到） | 不判定为偏差，在校准会上交换信息 |
| **标准差异** | 同样的行为证据，A 评 2 分 B 评 4 分 | 判定为校准问题，需要对齐评分标准 |

AI 如何区分？通过比较每个面试官的 evidence 字段：
- 如果 evidence 中包含了对方评价中没有的行为描述 → 信号差异
- 如果 evidence 描述相似但评分不同 → 标准差异

#### B2.3 面试官偏差时间序列

不只是看"这个面试官偏严"，而是追踪他/她是否在漂移：

```
面试官第 1-5 场面试：平均 3.2 分
面试官第 6-10 场面试：平均 3.8 分  ← 在变松？
面试官第 11-15 场面试：平均 3.9 分
```

用 CUSUM（累积和控制图）检测评分均值的漂移。

---

## B3. 面试官质量模型

### 改造方案

#### B3.1 质量评分公式（修订）

```
QualityScore = 0.30 × (1 - |leniency - 1|) × 100      // 偏差惩罚
             + 0.30 × prediction_accuracy × 100         // 预测准确度
             + 0.20 × consistency_score × 100           // 维度间一致性
             + 0.20 × calibration_participation × 100   // 校准会参与度
```

- `prediction_accuracy`：评价为"Hire"的人入职 90 天后绩效达标率
- `consistency_score`：该面试官各维度评分与全局均值的平均绝对偏差
- `calibration_participation`：在校准会上主动修正评分的频率

#### B3.2 预测准确度计算

```
面试官推荐「Hire」的候选人中：
  入职 90 天绩效达标的人数 / 总录用人数
```

这个指标需要等候选人入职 90 天后才能计算，所以面试官质量档案有滞后性。在数据不足时（总评价 < 10），该维度权重降低。

---

## B4. 人才密度模型

### 改造方案

#### B4.1 数据源：利用现有标签体系

RecruitOS 已有岗位标签体系（tags），每个岗位和每个候选人都有技能标签。人才密度应该从这个体系中计算：

```
Step 1: 确定该组织的核心能力维度
  - 从各岗位的 tags 中聚合得出
  - 去重后按岗位权重加权

Step 2: 计算每个维度的当前水位
  - 对每个团队成员，根据其简历技能和面试评价映射到能力维度
  - 当前水位 = 该维度下所有成员的技能水平加权和

Step 3: 计算每个维度的目标水位
  - 从未来 6-12 个月业务目标推导
  - 从岗位需求中的 "must-have" 技能标签聚合

Step 4: 密度评分 = avg(当前水位 / 目标水位)
```

#### B4.2 密度评分解释

```
密度 0.9+：高密度团队，招聘应保持 Bar Raising
密度 0.6-0.9：中等，有明确的能力缺口需要外部招聘
密度 < 0.6：低密度，需要优先投资招聘和培训
```

---

## B5. 冷启动策略

### 问题

新租户没有历史数据：没有 Offer 结果、没有面试评价、没有入职绩效。

### 方案：三级模型递进

```
Level 1: 行业模板（0-50 样本）
  - 同行业（互联网/金融/制造）× 同规模（中小/中大/集团）的聚合模型
  - 意向预测模型 = 行业级 LightGBM
  - 面试官偏差 = 行业均值
  - 明确告知用户：「当前基于行业数据，随数据积累将个性化」

Level 2: 租户适配（50-200 样本）
  - 行业模型 + 租户微调（transfer learning）
  - 意向预测特征权重开始向租户特有模式倾斜
  - 「模型正在学习贵司的招聘模式」

Level 3: 租户专属（> 200 样本）
  - 完全基于租户数据训练的模型
  - 置信度最高
```

#### 行业模板数据来源

初始阶段可以内置几个行业的模拟模板（基于公开数据 + 领域知识）：
- 互联网/电商
- 金融科技
- 企业服务/SaaS
- 智能制造

长期目标：真实客户数据脱敏聚合后作为行业模板。

---

## B6. 可解释性

### 要求

每一条 AI 输出必须附带推理链路，让用户可以质疑、理解、信任。

#### B6.1 SHAP 值解释

对于 LightGBM 模型，用 SHAP（SHapley Additive exPlanations）计算每个特征对最终分数的贡献：

```
意向评分 75 分 = 基准 50 分
  + 面试投入度高：+18 分（SHAP 值）
  + 回复速度快：+12 分
  - 薪资差距大：-15 分
  - 跳槽频繁：-8 分
  + 匹配分高：+10 分
  + 消息质量好：+8 分
```

#### B6.2 规则引擎的解释

对于非 ML 的引擎（如校准分析），也需要可解释：

```
「Product Sense 维度存在争议（最大分差 2 分）」
原因：面试官 B 挖到了面试官 A 未覆盖的信号
  - A 的证据：「候选人在设计题中展示了合理的优先级判断」
  - B 的证据：「候选人主动讨论了为什么不做 X，展示了深层产品思维」
建议：两人交换信息后重新评分
```

---

## B7. 模型层改造验收标准

- [ ] 意向预测从规则引擎切换到 LightGBM 模型，AUC ≥ 0.75
- [ ] 校准分析增加 Cohen's Kappa / ICC 统计指标
- [ ] 面试官质量增加时间序列漂移检测
- [ ] 人才密度从标签体系计算，非硬编码
- [ ] 冷启动行业模板就绪（至少 3 个行业）
- [ ] 所有 ML 模型输出附带 SHAP 特征贡献
- [ ] 模型自动重训练管道就绪（每月或累积 100 样本触发）
