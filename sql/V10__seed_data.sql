-- ================================================================
-- RecruitOS V10: 认知层种子数据
-- 为 PatternMiningScheduler / 认知层引擎提供初始数据
-- 包含 30+ 条事件、5 条预发现模式、8 条初始观察、7 条对象画像
-- ================================================================

SET @tenant_id = 1;

-- ================================================================
-- 1. cognitive_event_memory — 事件记忆（30+条真实场景）
-- ================================================================

-- 录用事件（13条）— 不同来源、不同结果质量
INSERT INTO cognitive_event_memory (id, tenant_id, event_type, event_subject, subject_id, context_json, outcome, outcome_reason, decision_quality, occurred_at) VALUES
(101, @tenant_id, 'HIRE', 'CANDIDATE', 1001, '{"source":"内推","company":"阿里","level":"P7","role":"后端工程师","team":"基础架构","interviewerCount":4,"avgScore":4.2,"weakDimensions":["Leadership"],"salaryOffer":65,"daysToOffer":22,"yearOfExp":7}', 'ACCEPTED', '薪资匹配，团队氛围好', 'GOOD', '2025-07-15 10:00:00'),
(102, @tenant_id, 'HIRE', 'CANDIDATE', 1002, '{"source":"内推","company":"腾讯","level":"P6","role":"前端工程师","team":"增长","interviewerCount":3,"avgScore":4.5,"weakDimensions":[],"salaryOffer":55,"daysToOffer":18,"yearOfExp":5}', 'ACCEPTED', '候选人主动选择，看重技术栈', 'GOOD', '2025-08-01 10:00:00'),
(103, @tenant_id, 'HIRE', 'CANDIDATE', 1003, '{"source":"内推","company":"字节","level":"P7","role":"算法工程师","team":"搜索","interviewerCount":4,"avgScore":4.0,"weakDimensions":["跨部门协作"],"salaryOffer":70,"daysToOffer":25,"yearOfExp":6}', 'ACCEPTED', 'Offer竞争胜出', 'GOOD', '2025-08-20 10:00:00'),
(104, @tenant_id, 'HIRE', 'CANDIDATE', 1004, '{"source":"猎聘","company":"美团","level":"P6","role":"测试工程师","team":"质量","interviewerCount":3,"avgScore":3.2,"weakDimensions":["Craft","技术深度"],"salaryOffer":40,"daysToOffer":35,"yearOfExp":4}', 'ACCEPTED', '急招岗位放宽标准', 'POOR', '2025-09-01 10:00:00'),
(105, @tenant_id, 'HIRE', 'CANDIDATE', 1005, '{"source":"猎聘","company":"百度","level":"P6","role":"后端工程师","team":"数据平台","interviewerCount":3,"avgScore":3.5,"weakDimensions":["用户洞察"],"salaryOffer":45,"daysToOffer":40,"yearOfExp":3}', 'ACCEPTED', '薪资低但有意愿', 'NEUTRAL', '2025-09-15 10:00:00'),
(106, @tenant_id, 'HIRE', 'CANDIDATE', 1006, '{"source":"BOSS直聘","company":"华为","level":"P8","role":"架构师","team":"基础架构","interviewerCount":5,"avgScore":4.8,"weakDimensions":[],"salaryOffer":90,"daysToOffer":15,"yearOfExp":10}', 'ACCEPTED', '全球招聘，人选卓越', 'GOOD', '2025-10-01 10:00:00'),
(107, @tenant_id, 'HIRE', 'CANDIDATE', 1007, '{"source":"BOSS直聘","company":"蚂蚁","level":"P6","role":"产品经理","team":"支付","interviewerCount":3,"avgScore":3.0,"weakDimensions":["数据驱动","Execution"],"salaryOffer":38,"daysToOffer":42,"yearOfExp":3}', 'ACCEPTED', '长期空缺岗位，妥协录用', 'POOR', '2025-10-15 10:00:00'),
(108, @tenant_id, 'HIRE', 'CANDIDATE', 1008, '{"source":"内推","company":"字节","level":"P7","role":"后端工程师","team":"推荐","interviewerCount":4,"avgScore":4.3,"weakDimensions":[],"salaryOffer":68,"daysToOffer":20,"yearOfExp":8}', 'ACCEPTED', 'Bar Raiser强力推荐', 'GOOD', '2025-11-01 10:00:00'),
(109, @tenant_id, 'HIRE', 'CANDIDATE', 1009, '{"source":"猎聘","company":"京东","level":"P7","role":"前端工程师","team":"交易","interviewerCount":3,"avgScore":3.1,"weakDimensions":["Culture","Leadership"],"salaryOffer":60,"daysToOffer":50,"yearOfExp':6}', 'ACCEPTED', '面试官李四评分偏高', 'POOR', '2025-11-15 10:00:00'),
(110, @tenant_id, 'HIRE', 'CANDIDATE', 1010, '{"source":"内推","company":"阿里","level":"P8","role":"技术经理","team":"安全","interviewerCount":5,"avgScore":4.6,"weakDimensions":[],"salaryOffer":95,"daysToOffer":28,"yearOfExp":12}', 'ACCEPTED', '管理+技术复合型人才', 'GOOD', '2025-12-01 10:00:00'),
(111, @tenant_id, 'HIRE', 'CANDIDATE', 1011, '{"source":"猎头","company":"快手","level":"P6","role":"运营","team":"内容","interviewerCount":3,"avgScore":3.3,"weakDimensions":["数据驱动"],"salaryOffer":35,"daysToOffer":55,"yearOfExp":2}', 'ACCEPTED', '流程过长但仍接受', 'NEUTRAL', '2025-12-10 10:00:00'),
(112, @tenant_id, 'HIRE', 'CANDIDATE', 1012, '{"source":"内推","company":"腾讯","level":"P7","role":"后端工程师","team":"微信","interviewerCount":4,"avgScore":4.1,"weakDimensions":["Culture"],"salaryOffer":66,"daysToOffer":19,"yearOfExp':6}', 'ACCEPTED', '候选人拒绝竞品Offer选择本公司', 'GOOD', '2026-01-05 10:00:00'),
(113, @tenant_id, 'HIRE', 'CANDIDATE', 1013, '{"source":"猎聘","company":"拼多多","level":"P5","role":"测试工程师","team":"电商","interviewerCount":2,"avgScore":2.8,"weakDimensions":["技术深度","Craft","Culture"],"salaryOffer":30,"daysToOffer":60,"yearOfExp':2}', 'ACCEPTED', '面试官王五评分极低但仍录用', 'POOR', '2026-01-20 10:00:00');

-- 拒绝事件（8条）
INSERT INTO cognitive_event_memory (id, tenant_id, event_type, event_subject, subject_id, context_json, outcome, outcome_reason, decision_quality, occurred_at) VALUES
(201, @tenant_id, 'REJECT', 'CANDIDATE', 2001, '{"source":"BOSS直聘","company":"未知","level":"P6","role":"后端工程师","team":"基础架构","interviewerCount":3,"avgScore":2.5,"weakDimensions":["技术深度","Craft"],"salaryOffer":null,"daysToOffer":null,"yearOfExp":4}', 'REJECTED', '技术深度不达标', 'GOOD', '2025-08-10 10:00:00'),
(202, @tenant_id, 'REJECT', 'CANDIDATE', 2002, '{"source":"内推","company":"美团","level":"P7","role":"前端工程师","team":"增长","interviewerCount":3,"avgScore":3.8,"weakDimensions":[],"salaryOffer":null,"daysToOffer":null,"yearOfExp':6}', 'REJECTED', 'Bar Raiser否决，Culture fit不足', 'GOOD', '2025-09-05 10:00:00'),
(203, @tenant_id, 'REJECT', 'CANDIDATE', 2003, '{"source":"猎聘","company":"未知","level":"P6","role":"算法工程师","team":"搜索","interviewerCount":2,"avgScore":3.5,"weakDimensions":["Leadership"],"salaryOffer":null,"daysToOffer":null,"yearOfExp':5}', 'REJECTED', '面试官张三评分偏严格', 'POOR', '2025-10-01 10:00:00'),
(204, @tenant_id, 'REJECT', 'CANDIDATE', 2004, '{"source":"猎聘","company":"百度","level":"P5","role":"测试工程师","team":"质量","interviewerCount":2,"avgScore":2.0,"weakDimensions":["Craft","技术深度","数据驱动"],"salaryOffer":null,"daysToOffer":null,"yearOfExp':2}', 'REJECTED', '多维度不达标', 'GOOD', '2025-10-20 10:00:00'),
(205, @tenant_id, 'REJECT', 'CANDIDATE', 2005, '{"source":"猎头","company":"字节","level":"P8","role":"架构师","team":"基础架构","interviewerCount":4,"avgScore":4.0,"weakDimensions":["Culture"],"salaryOffer":null,"daysToOffer":null,"yearOfExp':11}', 'REJECTED', 'Culture fit严重不符合，面试官李四均分偏高', 'NEUTRAL', '2025-11-10 10:00:00'),
(206, @tenant_id, 'REJECT', 'CANDIDATE', 2006, '{"source":"BOSS直聘","company":"华为","level":"P7","role":"产品经理","team":"支付","interviewerCount":3,"avgScore":3.2,"weakDimensions":["Execution","用户洞察"],"salaryOffer":null,"daysToOffer":null,"yearOfExp':7}', 'REJECTED', '产品Sense不足', 'GOOD', '2025-12-15 10:00:00'),
(207, @tenant_id, 'REJECT', 'CANDIDATE', 2007, '{"source":"官网","company":"未知","level":"P6","role":"运营","team":"内容","interviewerCount":2,"avgScore":2.8,"weakDimensions":["数据驱动","Craft"],"salaryOffer":null,"daysToOffer":null,"yearOfExp':3}', 'REJECTED', '缺乏数据思维', 'GOOD', '2026-01-10 10:00:00'),
(208, @tenant_id, 'REJECT', 'CANDIDATE', 2008, '{"source":"内推","company":"阿里","level":"P7","role":"后端工程师","team":"搜索","interviewerCount":3,"avgScore":3.6,"weakDimensions":[],"salaryOffer":null,"daysToOffer":null,"yearOfExp':6}', 'REJECTED', '面试官王五评分偏低导致误判', 'POOR', '2026-02-01 10:00:00');

-- 离职事件（4条）
INSERT INTO cognitive_event_memory (id, tenant_id, event_type, event_subject, subject_id, context_json, outcome, outcome_reason, decision_quality, occurred_at) VALUES
(301, @tenant_id, 'DEPARTURE', 'CANDIDATE', 1004, '{"source":"猎聘","tenureMonths":14,"reason":"项目不匹配","weakDimensionsAfterHire":["Craft","技术深度"],"originalHireQuality":"POOR"}', 'RESIGNED', '入职后发现技术栈不匹配', 'POOR', '2026-02-15 10:00:00'),
(302, @tenant_id, 'DEPARTURE', 'CANDIDATE', 1007, '{"source":"BOSS直聘","tenureMonths":8,"reason":"无法适应节奏","weakDimensionsAfterHire":["Execution","数据驱动"],"originalHireQuality":"POOR"}', 'RESIGNED', '入职后表现持续不佳', 'POOR', '2026-02-20 10:00:00'),
(303, @tenant_id, 'DEPARTURE', 'CANDIDATE', 1009, '{"source":"猎聘","tenureMonths":6,"reason":"文化不融合","weakDimensionsAfterHire":["Culture","Leadership"],"originalHireQuality":"POOR"}', 'RESIGNED', '团队协作冲突频繁', 'POOR', '2026-03-01 10:00:00'),
(304, @tenant_id, 'DEPARTURE', 'CANDIDATE', 1005, '{"source":"猎聘","tenureMonths":18,"reason":"另谋高就","weakDimensionsAfterHire":["用户洞察"],"originalHireQuality":"NEUTRAL"}', 'RESIGNED', '收到更好的外部机会', 'NEUTRAL', '2026-03-15 10:00:00');

-- 面试分歧事件（5条）
INSERT INTO cognitive_event_memory (id, tenant_id, event_type, event_subject, subject_id, context_json, outcome, outcome_reason, decision_quality, occurred_at) VALUES
(401, @tenant_id, 'INTERVIEW_DISAGREEMENT', 'INTERVIEWER', 5001, '{"interviewerName":"张三","disputeDimension":"Leadership","interviewerScore":2,"barRaiserScore":4,"delta":2.0,"candidateId":2003}', 'DISAGREEMENT', '张三对Leadership维度评分偏离Bar Raiser 2分', 'NEUTRAL', '2025-10-01 10:00:00'),
(402, @tenant_id, 'INTERVIEW_DISAGREEMENT', 'INTERVIEWER', 5001, '{"interviewerName":"张三","disputeDimension":"Culture","interviewerScore":2,"barRaiserScore":4,"delta":2.0,"candidateId":2006}', 'DISAGREEMENT', '张三对Culture维度评分偏低', 'NEUTRAL', '2025-12-15 10:00:00'),
(403, @tenant_id, 'INTERVIEW_DISAGREEMENT', 'INTERVIEWER', 5002, '{"interviewerName":"李四","disputeDimension":"Craft","interviewerScore":5,"barRaiserScore":3,"delta":2.0,"candidateId":1009}', 'DISAGREEMENT', '李四对Craft维度评分偏高', 'NEUTRAL', '2025-11-15 10:00:00'),
(404, @tenant_id, 'INTERVIEW_DISAGREEMENT', 'INTERVIEWER', 5002, '{"interviewerName":"李四","disputeDimension":"Culture","interviewerScore":5,"barRaiserScore":2,"delta":3.0,"candidateId":2005}', 'DISAGREEMENT', '李四严重高估Culture fit', 'NEUTRAL', '2025-11-10 10:00:00'),
(405, @tenant_id, 'INTERVIEW_DISAGREEMENT', 'INTERVIEWER', 5003, '{"interviewerName":"王五","disputeDimension":"技术深度","interviewerScore":2,"barRaiserScore":3,"delta":1.0,"candidateId":2008}', 'DISAGREEMENT', '王五技术深度评分偏低', 'NEUTRAL', '2026-02-01 10:00:00');

-- 面试官评估事件（5条）— 供 CUSUM drift 分析
INSERT INTO cognitive_event_memory (id, tenant_id, event_type, event_subject, subject_id, context_json, outcome, outcome_reason, decision_quality, occurred_at) VALUES
(501, @tenant_id, 'INTERVIEWER_ASSESSMENT', 'INTERVIEWER', 5001, '{"qualityScore":45.0,"qualityLevel":"NEEDS_IMPROVEMENT","leniencyIndex":0.65,"avgScore":2.4,"globalAvgScore":3.5,"predictionAccuracy":0.45,"totalEvaluations":12,"hasDrift":true,"driftDirection":"向下","cusumValue":5.2,"needsRecertification":true,"biasTags":["严格偏差","向下漂移"]}', 'NEEDS_IMPROVEMENT', 'DRIFT_DETECTED', 'UNKNOWN', '2026-01-15 10:00:00'),
(502, @tenant_id, 'INTERVIEWER_ASSESSMENT', 'INTERVIEWER', 5002, '{"qualityScore":40.0,"qualityLevel":"NEEDS_IMPROVEMENT","leniencyIndex":1.45,"avgScore":4.6,"globalAvgScore":3.5,"predictionAccuracy":0.50,"totalEvaluations":15,"hasDrift":true,"driftDirection":"向上","cusumValue":5.8,"needsRecertification":true,"biasTags":["宽松偏差","向上漂移"]}', 'NEEDS_IMPROVEMENT', 'DRIFT_DETECTED', 'UNKNOWN', '2026-01-15 10:00:00'),
(503, @tenant_id, 'INTERVIEWER_ASSESSMENT', 'INTERVIEWER', 5003, '{"qualityScore":55.0,"qualityLevel":"NEEDS_IMPROVEMENT","leniencyIndex":0.72,"avgScore":3.0,"globalAvgScore":3.5,"predictionAccuracy":0.55,"totalEvaluations":8,"hasDrift":false,"driftDirection":null,"cusumValue":2.1,"needsRecertification":false,"biasTags":[]}', 'NEEDS_IMPROVEMENT', 'STABLE', 'UNKNOWN', '2026-01-15 10:00:00'),
(504, @tenant_id, 'INTERVIEWER_ASSESSMENT', 'INTERVIEWER', 5004, '{"qualityScore":85.0,"qualityLevel":"EXCELLENT","leniencyIndex":1.05,"avgScore":3.6,"globalAvgScore":3.5,"predictionAccuracy":0.78,"totalEvaluations":20,"hasDrift":false,"driftDirection":null,"cusumValue":1.5,"needsRecertification":false,"biasTags":[]}', 'EXCELLENT', 'STABLE', 'UNKNOWN', '2026-01-15 10:00:00'),
(505, @tenant_id, 'INTERVIEWER_ASSESSMENT', 'INTERVIEWER', 5005, '{"qualityScore":72.0,"qualityLevel":"GOOD","leniencyIndex":0.95,"avgScore":3.3,"globalAvgScore":3.5,"predictionAccuracy":0.62,"totalEvaluations":10,"hasDrift":false,"driftDirection":null,"cusumValue":2.8,"needsRecertification":false,"biasTags":[]}', 'GOOD', 'STABLE', 'UNKNOWN', '2026-01-15 10:00:00');

-- PIPELINE 阻塞事件（3条）
INSERT INTO cognitive_event_memory (id, tenant_id, event_type, event_subject, subject_id, context_json, outcome, outcome_reason, decision_quality, occurred_at) VALUES
(601, @tenant_id, 'PIPELINE_BLOCKAGE', 'JOB', 3001, '{"jobTitle":"高级后端工程师","team":"基础架构","openDays":45,"candidatesInPipeline":3,"stage":"终面","bottleneck":"面试官张三排期饱和"}', 'BLOCKED', '终面面试官资源不足', 'UNKNOWN', '2026-02-10 10:00:00'),
(602, @tenant_id, 'PIPELINE_BLOCKAGE', 'JOB', 3002, '{"jobTitle":"前端工程师","team":"增长","openDays':60,"candidatesInPipeline":2,"stage":"Offer","bottleneck":"薪资审批流程过长"}', 'BLOCKED', 'Offer审批超时，候选人流失', 'UNKNOWN', '2026-02-25 10:00:00'),
(603, @tenant_id, 'PIPELINE_BLOCKAGE', 'JOB', 3003, '{"jobTitle":"产品经理","team":"支付","openDays":70,"candidatesInPipeline":1,"stage":"简历筛选","bottleneck":"猎聘渠道ROI低"}', 'BLOCKED', '渠道推送候选人质量差', 'POOR', '2026-03-05 10:00:00');


-- ================================================================
-- 2. cognitive_pattern_memory — 预发现模式（5条）
-- ================================================================
INSERT INTO cognitive_pattern_memory (id, tenant_id, pattern_type, pattern_name, pattern_rule, evidence_events, confidence, sample_size, status, discovered_at) VALUES
(1001, @tenant_id, 'CANDIDATE_SOURCE_PERFORMANCE',
    '渠道来源: 内推',
    '{"statement":"来自内推的候选人入职后表现稳定，不佳率仅15%，远低于平均水平35%。该渠道值得加大投入。","confidence":0.87,"sampleSize":7,"poorRate":0.15,"avgPoorRate":0.35}',
    '[101,102,103,108,110,112,202]', 0.870, 7, 'ACTIVE', '2026-01-10 10:00:00'),

(1002, @tenant_id, 'CANDIDATE_SOURCE_PERFORMANCE',
    '渠道来源: 猎聘',
    '{"statement":"来自猎聘的候选人入职后表现不佳率高达67%，远高于平均水平35%。建议在面试中增加适配性验证环节。","confidence":0.71,"sampleSize":6,"poorRate":0.67,"avgPoorRate":0.35}',
    '[104,105,109,111,113,301]', 0.710, 6, 'ACTIVE', '2026-01-10 10:00:00'),

(1003, @tenant_id, 'INTERVIEWER_SCORING_BIAS',
    '面试官: 张三',
    '{"statement":"面试官张三在过去3次校准会中被标记为评分分歧方，建议检查其评分标准是否与团队偏离。","confidence":0.65,"disputeCount":3}',
    '[401,402,203]', 0.650, 3, 'ACTIVE', '2026-01-15 10:00:00'),

(1004, @tenant_id, 'INTERVIEWER_SCORING_BIAS',
    '面试官: 李四',
    '{"statement":"面试官李四在过去2次校准会中被标记为评分分歧方(均为偏高)，建议安排影子面试验证评分一致性。","confidence":0.60,"disputeCount":2}',
    '[403,404]', 0.600, 2, 'ACTIVE', '2026-01-15 10:00:00'),

(1005, @tenant_id, 'DEPARTURE_EARLY_SIGNAL',
    '离职信号: Craft',
    '{"statement":"3个离职者中有2个在面试时Craft维度信号不足(占比67%)。建议在面试中加强该维度的评估。","confidence":0.67,"affectedDepartures":2,"totalDepartures":3}',
    '[301,302,303]', 0.670, 3, 'ACTIVE', '2026-03-10 10:00:00'),

(1006, @tenant_id, 'TEAM_COMPOSITION_RISK',
    '团队同质化: 阿里',
    '{"statement":"当前团队31%的成员来自阿里(4/13)，文化同质化可能导致留存问题，建议在新招聘中增加背景多样性。","confidence":0.31,"share":0.31,"departedFromSource":0}',
    '[101,110,103,108]', 0.310, 4, 'ACTIVE', '2026-01-05 10:00:00'),

(1007, @tenant_id, 'OFFER_ACCEPTANCE_FACTOR',
    'Offer周期影响',
    '{"statement":"30天内发Offer的接受率(78%)显著高于超30天(30%)，差距48%。建议加速招聘流程以提升Offer接受率。","confidence":0.70,"fastRate":0.78,"slowRate":0.30,"fastSample":9,"slowSample":10}',
    '[]', 0.700, 19, 'ACTIVE', '2026-01-20 10:00:00'),

(1008, @tenant_id, 'DEPARTURE_EARLY_SIGNAL',
    '离职信号: Culture',
    '{"statement":"3个离职者中有2个在面试时Culture维度信号不足(占比67%)。建议在面试中加入文化适配情景题。","confidence":0.67,"affectedDepartures":2,"totalDepartures":3}',
    '[302,303]', 0.670, 3, 'ACTIVE', '2026-03-10 10:00:00');


-- ================================================================
-- 3. cognitive_observation — AI主动观察（8条）
-- ================================================================
INSERT INTO cognitive_observation (id, tenant_id, observation_type, severity, title, body, related_objects, suggested_action, action_taken, created_at, expires_at) VALUES
(2001, @tenant_id, 'ALERT', 'CRITICAL',
    '[CRITICAL] 面试官 李四 评分出现向上漂移',
    'CUSUM检测发现面试官 李四 的评分自第 9 次评价起出现统计显著的向上漂移（CUSUM值=5.8，阈值=4.0）。当前质量分=40.0（NEEDS_IMPROVEMENT），宽松指数=1.45，共计 15 次评价。建议立即检查该面试官最近的评分标准是否发生变化，必要时安排校准会谈。',
    '[{"type":"INTERVIEWER","id":5002,"name":"李四"}]',
    '{"text":"查看面试官 李四 的详细评估报告并安排校准会谈","action_type":"NAVIGATE","params":{"route":"/interviewer-quality/5002"}}',
    'PENDING', '2026-01-15 10:00:00', '2026-02-14 10:00:00'),

(2002, @tenant_id, 'ALERT', 'WARNING',
    '[WARNING] 面试官 张三 评分出现向下漂移',
    'CUSUM检测发现面试官 张三 的评分自第 7 次评价起出现统计显著的向下漂移（CUSUM值=5.2，阈值=4.0）。当前质量分=45.0（NEEDS_IMPROVEMENT），宽松指数=0.65，共计 12 次评价。建议立即检查该面试官最近的评分标准是否发生变化，必要时安排校准会谈。',
    '[{"type":"INTERVIEWER","id":5001,"name":"张三"}]',
    '{"text":"查看面试官 张三 的详细评估报告并安排校准会谈","action_type":"NAVIGATE","params":{"route":"/interviewer-quality/5001"}}',
    'PENDING', '2026-01-15 10:00:00', '2026-02-14 10:00:00'),

(2003, @tenant_id, 'ALERT', 'WARNING',
    '面试官 张三 质量评分过低（45）',
    '面试官 张三 的综合质量评分为 45.0（等级：NEEDS_IMPROVEMENT），低于60分阈值。宽松指数=0.65，预测准确度=45%，共计 12 次评价。建议：(1) 安排Bar Raiser进行3场影子面试；(2) 参加校准会培训；(3) 暂时限制该面试官评估关键岗位候选人。',
    '[{"type":"INTERVIEWER","id":5001,"name":"张三"}]',
    '{"text":"为该面试官安排影子面试及校准培训","action_type":"CREATE_TASK","params":{"taskType":"RECERTIFICATION","interviewerId":5001}}',
    'PENDING', '2026-01-15 10:00:00', '2026-01-29 10:00:00'),

(2004, @tenant_id, 'ALERT', 'WARNING',
    '面试官 李四 质量评分过低（40）',
    '面试官 李四 的综合质量评分为 40.0（等级：NEEDS_IMPROVEMENT），低于60分阈值。宽松指数=1.45，预测准确度=50%，共计 15 次评价。建议：(1) 安排Bar Raiser进行3场影子面试；(2) 参加校准会培训；(3) 暂时限制该面试官评估关键岗位候选人。',
    '[{"type":"INTERVIEWER","id":5002,"name":"李四"}]',
    '{"text":"为该面试官安排影子面试及校准培训","action_type":"CREATE_TASK","params":{"taskType":"RECERTIFICATION","interviewerId":5002}}',
    'PENDING', '2026-01-15 10:00:00', '2026-01-29 10:00:00'),

(2005, @tenant_id, 'INSIGHT', 'INFO',
    '面试官 张三 存在系统性严格偏差',
    '面试官 张三 的宽松指数为 0.65（正常范围 0.7-1.3），偏差方向：严格偏差。共计 12 次评价，平均评分 2.40 vs 全局平均 3.50。建议为该面试官配置行为锚点评分表，减少主观印象对评分的干扰。',
    '[{"type":"INTERVIEWER","id":5001,"name":"张三"}]',
    '{"text":"为面试官 张三 配置行为锚点评分表","action_type":"CONFIGURE","params":{"configType":"SCORING_RUBRIC","interviewerId":5001}}',
    'PENDING', '2026-01-15 10:00:00', '2026-03-16 10:00:00'),

(2006, @tenant_id, 'INSIGHT', 'INFO',
    '面试官 李四 存在系统性宽松偏差',
    '面试官 李四 的宽松指数为 1.45（正常范围 0.7-1.3），偏差方向：宽松偏差。共计 15 次评价，平均评分 4.60 vs 全局平均 3.50。建议为该面试官配置行为锚点评分表，减少主观印象对评分的干扰。',
    '[{"type":"INTERVIEWER","id":5002,"name":"李四"}]',
    '{"text":"为面试官 李四 配置行为锚点评分表","action_type":"CONFIGURE","params":{"configType":"SCORING_RUBRIC","interviewerId":5002}}',
    'PENDING', '2026-01-15 10:00:00', '2026-03-16 10:00:00'),

(2007, @tenant_id, 'INSIGHT', 'WARNING',
    'Pipeline阻塞：高级后端工程师岗位终面资源不足',
    '岗位"高级后端工程师"已开放 45 天，Pipeline中仅 3 名候选人，阻塞在终面阶段。根因：面试官 张三 排期饱和，无法安排终面。建议：(1) 调派其他面试官替代；(2) 评估此岗位的招聘优先级。',
    '[{"type":"JOB","id":3001,"name":"高级后端工程师"}]',
    '{"text":"调派面试官或调整岗位优先级","action_type":"REASSIGN","params":{"jobId":3001}}',
    'PENDING', '2026-02-10 10:00:00', '2026-02-24 10:00:00'),

(2008, @tenant_id, 'SUGGESTION', 'INFO',
    '招聘周期过长：猎聘渠道Offer周期中位数超50天',
    '来自猎聘渠道的Offer平均周期为 50+ 天（内推渠道仅 22 天），候选人流失风险高。建议：设置猎聘渠道 35 天 SLA 并配置超时自动预警。',
    '[{"type":"CHANNEL","id":2,"name":"猎聘"}]',
    '{"text":"为猎聘渠道配置35天SLA","action_type":"CONFIGURE","params":{"configType":"SLA","channelId":2,"targetDays":35}}',
    'PENDING', '2026-02-20 10:00:00', '2026-04-21 10:00:00');


-- ================================================================
-- 4. cognitive_object_memory — 对象记忆（7条）
-- ================================================================
INSERT INTO cognitive_object_memory (id, tenant_id, object_type, object_id, summary_tldr, evolving_profile, key_signals, risk_flags) VALUES
(3001, @tenant_id, 'CANDIDATE', 1004,
    '来自美团测试工程师，Craft/技术深度薄弱，录用后14个月离职',
    '{"source":"猎聘","level":"P6","role":"测试工程师","team":"质量","quality":"POOR","tenure":14,"departureReason":"项目不匹配"}',
    '[{"signal":"面试多维度弱","value":"Craft+技术深度"},{"signal":"入职后表现不佳","value":"14个月离职"}]',
    '["猎聘渠道高风险","面试标准放宽"]'),

(3002, @tenant_id, 'INTERVIEWER', 5001,
    '张三：系统性严格偏差+CUSUM向下漂移，需复训',
    '{"leniencyIndex":0.65,"qualityScore":45,"bias":"严格偏差","drift":"向下","totalEvals":12,"disputeCount":3}',
    '[{"signal":"CUSUM向下漂移","severity":"CRITICAL"},{"signal":"3次校准分歧","severity":"WARNING"}]',
    '["质量分<50","需限制面试资格"]'),

(3003, @tenant_id, 'INTERVIEWER', 5002,
    '李四：系统性宽松偏差+CUSUM向上漂移，需复训',
    '{"leniencyIndex":1.45,"qualityScore":40,"bias":"宽松偏差","drift":"向上","totalEvals":15,"disputeCount":2}',
    '[{"signal":"CUSUM向上漂移","severity":"CRITICAL"},{"signal":"2次校准分歧偏高","severity":"WARNING"}]',
    '["质量分<50","需限制面试资格"]'),

(3004, @tenant_id, 'INTERVIEWER', 5004,
    '赵六：优秀面试官，预测准确度78%',
    '{"leniencyIndex":1.05,"qualityScore":85,"bias":"无","drift":"无","totalEvals":20,"disputeCount":0}',
    '[{"signal":"预测准确度78%","severity":"POSITIVE"},{"signal":"无系统偏差","severity":"POSITIVE"}]',
    '[]'),

(3005, @tenant_id, 'JOB', 3001,
    '高级后端工程师-基础架构：开放45天，终面阻塞',
    '{"openDays":45,"pipelineCandidates":3,"bottleneck":"终面","reason":"面试官排期"}',
    '[{"signal":"Pipeline阻塞","severity":"WARNING"},{"signal":"开放超30天","severity":"WARNING"}]',
    '["面试官张三排期饱和","终面资源不足"]'),

(3006, @tenant_id, 'HIRING_MANAGER', 6001,
    '陈七：招聘决策偏风险厌恶，决策周期长',
    '{"decisionSpeed":0.35,"riskTolerance":0.25,"leniencyIndex":0.85,"totalDecisions":8,"avgCycleDays":55}',
    '[{"signal":"决策速度慢","severity":"WARNING"},{"signal":"风险厌恶","severity":"INFO"}]',
    '["Offer周期中位数55天","可能导致候选人流失"]'),

(3007, @tenant_id, 'TEAM', 7001,
    '基础架构团队：31%来自阿里系，存在文化同质化风险',
    '{"totalMembers":13,"topSource":"阿里","topSourceShare":0.31,"diversityScore":0.45,"avgTenure":22}',
    '[{"signal":"阿里系占比31%","severity":"INFO"},{"signal":"多样性评分0.45","severity":"WARNING"}]',
    '["文化同质化","新成员多样性建议"]');


-- ================================================================
-- 5. cognitive_lesson_memory — 教训记忆（3条）
-- ================================================================
INSERT INTO cognitive_lesson_memory (id, tenant_id, lesson_type, title, description, evidence, corrective_action, severity, status, learned_at) VALUES
(4001, @tenant_id, 'BAD_HIRE_PATTERN',
    'Craft维度薄弱的候选人不应在急招时放宽标准',
    '1004和1007两名候选人均在面试时Craft维度信号明显不足，但分别因"急招"和"长期空缺"被录用，均在入职后12个月内离职。',
    '{"events":[301,302],"candidates":[1004,1007],"commonRoot":"Craft维度薄弱+面试标准放宽"}',
    '建立Craft维度硬底线：面试任一维度<2.5分时自动触发Bar Raiser复审',
    'CRITICAL', 'ACTIVE', '2026-03-10 10:00:00'),

(4002, @tenant_id, 'INTERVIEW_BLIND_SPOT',
    '面试官评分偏差未被CUSUM检测前的早期信号',
    '李四在2025年Q4连续2次出现Culture维度偏高判分(+2和+3分偏差)，但当时未被系统检测。错失早期干预窗口，导致后续录用了Culture fit不合格的候选人。',
    '{"events":[403,404],"interviewer":5002,"warningPeriod":"2025Q4","detectionDelay":"2个月"}',
    '建立面试评分实时监控Dashboard，任一面试官单次偏差>2分立即触发Review',
    'CRITICAL', 'ACTIVE', '2026-01-20 10:00:00'),

(4003, @tenant_id, 'PROCESS_FAILURE',
    'Offer审批流程超时导致候选人流失',
    '前端工程师岗位(3002)的Offer审批流程耗时超过15个工作日，期间候选人收到竞品Offer放弃等待。根因：HRBP与财务审批节点串行。',
    '{"events":[602],"jobId":3002,"lossReason":"Offer审批超时","competingOffer":true}',
    '优化审批流：HRBP与财务节点从串行改为并行，SLA从15工作日缩短至5工作日',
    'IMPORTANT', 'ACTIVE', '2026-02-28 10:00:00');

