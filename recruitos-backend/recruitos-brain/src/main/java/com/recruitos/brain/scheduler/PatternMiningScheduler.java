package com.recruitos.brain.scheduler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.recruitos.brain.mapper.CognitiveMapper;
import com.recruitos.brain.TenantResolver;
import com.recruitos.brain.service.CognitiveMemoryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.*;

/**
 * 模式挖掘调度器 — 增强版
 * 定期从 cognitive_event_memory 中自动发现统计规律，写入 cognitive_pattern_memory。
 *
 * 挖掘类型（7种）:
 *   1. 渠道-绩效关联（已有）
 *   2. 面试官评分偏差（已有）
 *   3. 离职预警信号（已有）
 *   4. 团队构成风险（已有）
 *   5. Offer 接受率因子（已有）
 *   6. Pipeline 阻塞模式（新增）
 *   7. 技能维度-留存率关联（新增）
 *
 * 这是 AI "学到东西"的核心——不是人告诉它规律，而是它自己从数据里发现。
 */
@Component
public class PatternMiningScheduler {

    private static final Logger log = LoggerFactory.getLogger(PatternMiningScheduler.class);
    private static final ObjectMapper JSON = new ObjectMapper();

    @Resource private CognitiveMapper cognitiveMapper;
    @Resource private CognitiveMemoryService memory;
    @Resource private TenantResolver tenantResolver;

    /**
     * 每天凌晨 3 点执行模式挖掘
     */
    @Scheduled(cron = "0 0 3 * * ?")
    public void minePatterns() {
        log.info("PatternMiningScheduler: starting daily pattern discovery");

        List<Long> tenantIds = getActiveTenantIds();

        for (Long tenantId : tenantIds) {
            try {
                mineSourcePerformance(tenantId);
                mineInterviewerBias(tenantId);
                mineDepartureSignals(tenantId);
                mineTeamCompositionRisk(tenantId);
                mineOfferAcceptanceFactors(tenantId);
                minePipelineBottleneck(tenantId);        // 新增
                mineSkillRetentionCorrelation(tenantId); // 新增
            } catch (Exception e) {
                log.error("Pattern mining failed for tenant {}", tenantId, e);
            }
        }

        log.info("PatternMiningScheduler: daily pattern discovery complete");
    }

    // ================================================================
    // 1. 渠道-绩效关联
    // ================================================================
    private void mineSourcePerformance(Long tenantId) {
        List<Map<String, Object>> hireEvents = cognitiveMapper.findEventsByType(tenantId, "HIRE", 200);
        if (hireEvents == null || hireEvents.size() < 10) return;

        Map<String, int[]> sourceStats = new LinkedHashMap<>();
        for (Map<String, Object> evt : hireEvents) {
            String ctxJson = (String) evt.get("contextJson");
            String quality = (String) evt.get("decisionQuality");
            if (ctxJson == null) continue;
            String source = extractSource(ctxJson);
            if (source == null) continue;
            sourceStats.putIfAbsent(source, new int[]{0, 0});
            sourceStats.get(source)[0]++;
            if ("POOR".equals(quality)) sourceStats.get(source)[1]++;
        }

        for (Map.Entry<String, int[]> entry : sourceStats.entrySet()) {
            String source = entry.getKey();
            int total = entry.getValue()[0];
            int poor = entry.getValue()[1];
            if (total < 3) continue;

            double poorRate = (double) poor / total;
            double avgPoorRate = sourceStats.values().stream()
                .mapToInt(arr -> arr[1]).sum()
                / (double) sourceStats.values().stream().mapToInt(arr -> arr[0]).sum();

            double confidence = 1.0 - (1.0 / Math.sqrt(total));
            if (confidence < 0.5) continue;

            String statement;
            if (poorRate > avgPoorRate * 1.3) {
                statement = String.format("来自%s的候选人入职后表现不佳率%.0f%%，高于平均水平%.0f%%。"
                    + "建议在面试中增加适配性验证环节。",
                    source, poorRate * 100, avgPoorRate * 100);
            } else if (poorRate < avgPoorRate * 0.7) {
                statement = String.format("来自%s的候选人入职后表现稳定，不佳率仅%.0f%%，"
                    + "低于平均水平%.0f%%。该渠道值得加大投入。",
                    source, poorRate * 100, avgPoorRate * 100);
            } else {
                continue;
            }

            Map<String, Object> rule = Map.of(
                "statement", statement,
                "confidence", Math.round(confidence * 100) / 100.0,
                "sampleSize", total,
                "poorRate", Math.round(poorRate * 100) / 100.0,
                "avgPoorRate", Math.round(avgPoorRate * 100) / 100.0
            );

            if (patternExists(tenantId, "CANDIDATE_SOURCE_PERFORMANCE", source)) continue;

            memory.discoverPattern(tenantId, "CANDIDATE_SOURCE_PERFORMANCE",
                "渠道来源: " + source, rule,
                extractEventIdsByKeyword(hireEvents, source),
                confidence, total);
        }
    }

    // ================================================================
    // 2. 面试官评分偏差
    // ================================================================
    private void mineInterviewerBias(Long tenantId) {
        List<Map<String, Object>> disagreeEvents = cognitiveMapper.findEventsByType(
            tenantId, "INTERVIEW_DISAGREEMENT", 100);
        if (disagreeEvents == null || disagreeEvents.size() < 3) return;

        Map<String, Integer> interviewerDisputeCount = new LinkedHashMap<>();
        for (Map<String, Object> evt : disagreeEvents) {
            String ctxJson = (String) evt.get("contextJson");
            if (ctxJson == null) continue;
            Set<String> names = extractEvaluatorNames(ctxJson);
            for (String name : names)
                interviewerDisputeCount.merge(name, 1, Integer::sum);
        }

        for (Map.Entry<String, Integer> entry : interviewerDisputeCount.entrySet()) {
            if (entry.getValue() < 2) continue;

            String interviewer = entry.getKey();
            int disputes = entry.getValue();

            Map<String, Object> rule = Map.of(
                "statement", String.format("面试官%s在过去%d次校准会中被标记为评分分歧方，"
                    + "建议检查其评分标准是否与团队偏离。", interviewer, disputes),
                "confidence", Math.min(0.85, 0.5 + disputes * 0.05),
                "disputeCount", disputes
            );

            if (patternExists(tenantId, "INTERVIEWER_SCORING_BIAS", interviewer)) continue;

            memory.discoverPattern(tenantId, "INTERVIEWER_SCORING_BIAS",
                "面试官: " + interviewer, rule,
                extractEventIdsByKeyword(disagreeEvents, interviewer),
                Math.min(0.85, 0.5 + disputes * 0.05),
                disputes);
        }
    }

    // ================================================================
    // 3. 离职预警信号
    // ================================================================
    private void mineDepartureSignals(Long tenantId) {
        List<Map<String, Object>> departureEvents = cognitiveMapper.findEventsByType(
            tenantId, "DEPARTURE", 50);
        if (departureEvents == null || departureEvents.size() < 3) return;

        Map<String, Integer> missingSignalCounts = new LinkedHashMap<>();
        int totalDepartures = 0;

        for (Map<String, Object> depEvt : departureEvents) {
            Long subjectId = (Long) depEvt.get("subjectId");
            List<Map<String, Object>> candidateEvents = cognitiveMapper.findEventsBySubject(
                tenantId, "CANDIDATE", subjectId);

            Map<String, Object> hireEvent = null;
            if (candidateEvents != null) {
                for (Map<String, Object> evt : candidateEvents) {
                    if ("HIRE".equals(evt.get("eventType"))) { hireEvent = evt; break; }
                }
            }
            if (hireEvent == null) continue;
            totalDepartures++;

            Set<String> weakDimensions = extractWeakDimensions(
                (String) hireEvent.get("contextJson"));
            for (String dim : weakDimensions)
                missingSignalCounts.merge(dim, 1, Integer::sum);
        }

        for (Map.Entry<String, Integer> entry : missingSignalCounts.entrySet()) {
            double ratio = (double) entry.getValue() / totalDepartures;
            if (ratio < 0.4 || totalDepartures < 3) continue;

            String dimName = entry.getKey();
            Map<String, Object> rule = Map.of(
                "statement", String.format("%d个短期离职者中有%d个在面试时'%s'维度信号不足"
                    + "（占比%.0f%%）。建议在面试中加强该维度的评估。",
                    totalDepartures, entry.getValue(), dimName, ratio * 100),
                "confidence", Math.min(0.8, ratio),
                "affectedDepartures", entry.getValue(),
                "totalDepartures", totalDepartures
            );

            if (patternExists(tenantId, "DEPARTURE_EARLY_SIGNAL", dimName)) continue;

            memory.discoverPattern(tenantId, "DEPARTURE_EARLY_SIGNAL",
                "离职信号: " + dimName, rule,
                extractAllIds(departureEvents),
                Math.min(0.8, ratio),
                totalDepartures);
        }
    }

    // ================================================================
    // 4. 团队构成风险
    // ================================================================
    private void mineTeamCompositionRisk(Long tenantId) {
        List<Map<String, Object>> hireEvents = cognitiveMapper.findEventsByType(
            tenantId, "HIRE", 100);
        if (hireEvents == null || hireEvents.size() < 5) return;

        Map<String, Integer> companyCounts = new LinkedHashMap<>();
        Map<String, Integer> companyDepartures = new LinkedHashMap<>();
        int totalHires = 0;

        for (Map<String, Object> evt : hireEvents) {
            String ctxJson = (String) evt.get("contextJson");
            String company = extractCompany(ctxJson);
            if (company == null) continue;
            totalHires++;
            companyCounts.merge(company, 1, Integer::sum);
            if ("POOR".equals(evt.get("decisionQuality")))
                companyDepartures.merge(company, 1, Integer::sum);
        }

        for (Map.Entry<String, Integer> entry : companyCounts.entrySet()) {
            String company = entry.getKey();
            int count = entry.getValue();
            double share = (double) count / totalHires;
            int departed = companyDepartures.getOrDefault(company, 0);

            if (share > 0.3 && departed > 0) {
                Map<String, Object> rule = Map.of(
                    "statement", String.format("当前团队%d%%的成员来自%s（%d/%d），"
                        + "其中%d人已离职。文化同质化可能导致留存问题。",
                        (int)(share * 100), company, count, totalHires, departed),
                    "confidence", Math.min(0.8, share),
                    "share", share,
                    "departedFromSource", departed
                );

                if (patternExists(tenantId, "TEAM_COMPOSITION_RISK", company)) continue;

                memory.discoverPattern(tenantId, "TEAM_COMPOSITION_RISK",
                    "团队同质化: " + company, rule,
                    extractAllIds(hireEvents),
                    Math.min(0.8, share), count);
            }
        }
    }

    // ================================================================
    // 5. Offer 接受率因子
    // ================================================================
    private void mineOfferAcceptanceFactors(Long tenantId) {
        List<Map<String, Object>> hireEvents = cognitiveMapper.findEventsByType(
            tenantId, "HIRE", 100);
        List<Map<String, Object>> rejectEvents = cognitiveMapper.findEventsByType(
            tenantId, "REJECT", 100);

        if (hireEvents == null || rejectEvents == null || hireEvents.size() < 5) return;

        int fastAccepted = 0, fastTotal = 0;
        int slowAccepted = 0, slowTotal = 0;

        for (Map<String, Object> evt : hireEvents) {
            String ctxJson = (String) evt.get("contextJson");
            if (ctxJson == null || !ctxJson.contains("\"daysToOffer\"")) continue;
            int days = extractDaysToOffer(ctxJson);
            if (days < 30) { fastAccepted++; fastTotal++; }
            else { slowAccepted++; slowTotal++; }
        }
        for (Map<String, Object> evt : rejectEvents) {
            String ctxJson = (String) evt.get("contextJson");
            if (ctxJson == null || !ctxJson.contains("\"daysToOffer\"")) continue;
            int days = extractDaysToOffer(ctxJson);
            if (days < 30) fastTotal++;
            else slowTotal++;
        }

        if (fastTotal > 3 && slowTotal > 3) {
            double fastRate = (double) fastAccepted / fastTotal;
            double slowRate = (double) slowAccepted / slowTotal;
            double diff = fastRate - slowRate;

            if (Math.abs(diff) > 0.10) {
                String statement = diff > 0
                    ? String.format("30天内发Offer的接受率（%.0f%%）显著高于超30天（%.0f%%），"
                        + "差距%.0f%%。建议加速招聘流程以提升Offer接受率。",
                        fastRate * 100, slowRate * 100, diff * 100)
                    : String.format("30天以上发Offer的接受率（%.0f%%）反而高于快速流程（%.0f%%），"
                        + "可能因为复杂岗位需要更充分的候选人沟通。",
                        slowRate * 100, fastRate * 100);

                Map<String, Object> rule = Map.of(
                    "statement", statement,
                    "confidence", 0.7,
                    "fastRate", fastRate, "slowRate", slowRate,
                    "fastSample", fastTotal, "slowSample", slowTotal
                );

                memory.discoverPattern(tenantId, "OFFER_ACCEPTANCE_FACTOR",
                    "Offer周期影响", rule, new ArrayList<>(), 0.7,
                    fastTotal + slowTotal);
            }
        }
    }

    // ================================================================
    // 6. Pipeline 阻塞模式（新增）
    // 分析 PIPELINE_BLOCKAGE 事件，发现系统性的流程瓶颈
    // ================================================================
    private void minePipelineBottleneck(Long tenantId) {
        List<Map<String, Object>> blockageEvents = cognitiveMapper.findEventsByType(
            tenantId, "PIPELINE_BLOCKAGE", 50);
        if (blockageEvents == null || blockageEvents.size() < 2) return;

        // 按阻塞阶段分类
        Map<String, List<Map<String, Object>>> stageMap = new LinkedHashMap<>();
        for (Map<String, Object> evt : blockageEvents) {
            String ctxJson = (String) evt.get("contextJson");
            if (ctxJson == null) continue;
            String stage = extractJsonField(ctxJson, "stage");
            if (stage == null) continue;
            stageMap.computeIfAbsent(stage, k -> new ArrayList<>()).add(evt);
        }

        for (Map.Entry<String, List<Map<String, Object>>> entry : stageMap.entrySet()) {
            String stage = entry.getKey();
            List<Map<String, Object>> events = entry.getValue();
            if (events.size() < 2) continue;

            // 统计该阶段的阻塞原因
            Map<String, Integer> bottleneckReasons = new LinkedHashMap<>();
            Map<String, Double> avgOpenDays = new LinkedHashMap<>();
            Set<String> jobTitles = new LinkedHashSet<>();

            for (Map<String, Object> evt : events) {
                String ctxJson = (String) evt.get("contextJson");
                if (ctxJson == null) continue;
                String bottleneck = extractJsonField(ctxJson, "bottleneck");
                String jobTitle = extractJsonField(ctxJson, "jobTitle");
                if (bottleneck != null) bottleneckReasons.merge(bottleneck, 1, Integer::sum);
                if (jobTitle != null) jobTitles.add(jobTitle);

                int days = extractJsonInt(ctxJson, "openDays", 30);
                avgOpenDays.merge(bottleneck != null ? bottleneck : "unknown",
                    (double) days, (a, b) -> a + b);
            }

            // 找出最主要阻塞原因
            String topBottleneck = bottleneckReasons.entrySet().stream()
                .max(Map.Entry.comparingByValue()).map(Map.Entry::getKey).orElse(null);
            if (topBottleneck == null) continue;

            int totalBlockages = events.size();
            int topCount = bottleneckReasons.get(topBottleneck);
            double bottleneckShare = (double) topCount / totalBlockages;

            if (bottleneckShare < 0.4) continue; // 不够集中

            String statement = String.format("Pipeline在'%s'阶段出现系统性阻塞（%d个岗位受影响的%d%%），"
                + "最主要原因：%s。影响岗位：%s。建议优化该阶段的资源配置或调整流程SLA。",
                stage, totalBlockages, (int)(bottleneckShare * 100),
                topBottleneck, String.join("、", jobTitles));

            Map<String, Object> rule = Map.of(
                "statement", statement,
                "confidence", Math.min(0.85, bottleneckShare + 0.1),
                "blockedStage", stage,
                "topBottleneck", topBottleneck,
                "affectedJobs", totalBlockages,
                "bottleneckShare", bottleneckShare,
                "jobTitles", new ArrayList<>(jobTitles)
            );

            if (patternExists(tenantId, "PIPELINE_BOTTLENECK", stage)) continue;

            List<Long> evidenceIds = new ArrayList<>();
            for (Map<String, Object> evt : events) evidenceIds.add((Long) evt.get("id"));

            memory.discoverPattern(tenantId, "PIPELINE_BOTTLENECK",
                "阻塞: " + stage + " → " + topBottleneck,
                rule, evidenceIds,
                Math.min(0.85, bottleneckShare + 0.1), totalBlockages);
        }
    }

    // ================================================================
    // 7. 技能维度-留存率关联（新增）
    // 分析哪些弱维度信号最常出现在短期离职者的面试记录中
    // ================================================================
    private void mineSkillRetentionCorrelation(Long tenantId) {
        List<Map<String, Object>> departureEvents = cognitiveMapper.findEventsByType(
            tenantId, "DEPARTURE", 50);
        if (departureEvents == null || departureEvents.size() < 3) return;

        // 收集所有离职者的 hire 事件
        List<Map<String, Object>> departedHireEvents = new ArrayList<>();
        for (Map<String, Object> depEvt : departureEvents) {
            Long subjectId = (Long) depEvt.get("subjectId");
            if (subjectId == null) continue;
            List<Map<String, Object>> candidateEvents = cognitiveMapper.findEventsBySubject(
                tenantId, "CANDIDATE", subjectId);
            if (candidateEvents != null) {
                for (Map<String, Object> evt : candidateEvents) {
                    if ("HIRE".equals(evt.get("eventType"))) {
                        // 附加 tenure 信息
                        String depCtx = (String) depEvt.get("contextJson");
                        int tenure = 24; // 默认24个月
                        if (depCtx != null) {
                            try {
                                java.util.Map<?,?> parsed = JSON.readValue(depCtx, java.util.Map.class);
                                Object t = parsed.get("tenureMonths");
                                if (t instanceof Number) tenure = ((Number) t).intValue();
                            } catch (Exception ignored) {}
                        }
                        Map<String, Object> enriched = new LinkedHashMap<>(evt);
                        enriched.put("_tenureMonths", tenure);
                        departedHireEvents.add(enriched);
                        break;
                    }
                }
            }
        }

        // 收集所有留任者的 hire 事件（非离职、非POOR）
        List<Map<String, Object>> allHireEvents = cognitiveMapper.findEventsByType(
            tenantId, "HIRE", 200);
        Set<Long> departedIds = new HashSet<>();
        for (Map<String, Object> depEvt : departureEvents) {
            Long sid = (Long) depEvt.get("subjectId");
            if (sid != null) departedIds.add(sid);
        }

        List<Map<String, Object>> retainedHireEvents = new ArrayList<>();
        if (allHireEvents != null) {
            for (Map<String, Object> evt : allHireEvents) {
                Long sid = (Long) evt.get("subjectId");
                if (sid != null && !departedIds.contains(sid)) {
                    retainedHireEvents.add(evt);
                }
            }
        }

        if (departedHireEvents.size() < 3 || retainedHireEvents.size() < 5) return;

        // 统计各弱维度在离职组 vs 留任组中的出现频率
        String[] allDimensions = {"Craft", "技术深度", "跨部门协作", "Leadership",
            "Culture", "Execution", "用户洞察", "数据驱动", "Product Sense", "架构能力"};
        List<Map<String, Object>> findings = new ArrayList<>();

        for (String dim : allDimensions) {
            int depCount = 0, retCount = 0;
            for (Map<String, Object> evt : departedHireEvents) {
                String ctx = (String) evt.get("contextJson");
                if (ctx != null && hasWeakDimension(ctx, dim)) depCount++;
            }
            for (Map<String, Object> evt : retainedHireEvents) {
                String ctx = (String) evt.get("contextJson");
                if (ctx != null && hasWeakDimension(ctx, dim)) retCount++;
            }

            double depRate = (double) depCount / departedHireEvents.size();
            double retRate = (double) retCount / retainedHireEvents.size();
            double oddsRatio = retRate > 0 ? depRate / retRate : (depRate > 0 ? 99 : 1);
            double lift = depRate - retRate;

            if (lift > 0.15 && depCount >= 2) {
                Map<String, Object> finding = new LinkedHashMap<>();
                finding.put("dimension", dim);
                finding.put("departedWeakRate", Math.round(depRate * 100) / 100.0);
                finding.put("retainedWeakRate", Math.round(retRate * 100) / 100.0);
                finding.put("oddsRatio", Math.round(oddsRatio * 10) / 10.0);
                finding.put("lift", Math.round(lift * 100) / 100.0);
                finding.put("departedCount", depCount);
                finding.put("departedTotal", departedHireEvents.size());
                findings.add(finding);
            }
        }

        // 按 lift 降序，取前 3 个最显著维度
        findings.sort((a, b) -> Double.compare(
            (Double) b.get("lift"), (Double) a.get("lift")));

        for (int i = 0; i < Math.min(3, findings.size()); i++) {
            Map<String, Object> f = findings.get(i);
            String dim = (String) f.get("dimension");
            double lift = (Double) f.get("lift");
            double oddsRatio = (Double) f.get("oddsRatio");
            int depCount = (Integer) f.get("departedCount");

            String statement = String.format("维度'%s'在短期离职者中的弱信号出现率（%.0f%%）"
                + "显著高于留任者（%.0f%%），Odds Ratio=%.1f，提升度=%.0f%%。"
                + "该维度可能是离职的强预测因子，建议在录用决策中对该维度设置更高的通过阈值。",
                dim,
                ((Double) f.get("departedWeakRate")) * 100,
                ((Double) f.get("retainedWeakRate")) * 100,
                oddsRatio,
                lift * 100);

            Map<String, Object> rule = Map.of(
                "statement", statement,
                "confidence", Math.min(0.8, 0.4 + lift),
                "dimension", dim,
                "oddsRatio", oddsRatio,
                "lift", lift,
                "departedWeakRate", f.get("departedWeakRate"),
                "retainedWeakRate", f.get("retainedWeakRate"),
                "departedCount", depCount
            );

            if (patternExists(tenantId, "SKILL_RETENTION_CORRELATION", dim)) continue;

            memory.discoverPattern(tenantId, "SKILL_RETENTION_CORRELATION",
                "留存关联: " + dim + " (OR=" + String.format("%.1f", oddsRatio) + ")",
                rule,
                extractAllIds(departureEvents),
                Math.min(0.8, 0.4 + lift),
                departedHireEvents.size() + retainedHireEvents.size());
        }
    }

    // ================================================================
    // 增强的数据提取工具
    // ================================================================

    /** 从JSON字符串中提取指定字段值 */
    private String extractJsonField(String json, String key) {
        if (json == null) return null;
        // 简单JSON字段提取：匹配 "key":"value" 或 "key": "value"
        String pattern = "\"" + key + "\"\\s*:\\s*\"([^\"]+)\"";
        java.util.regex.Matcher m = java.util.regex.Pattern.compile(pattern).matcher(json);
        if (m.find()) return m.group(1);
        return null;
    }

    /** 从JSON字符串中提取指定字段的整数值 */
    private int extractJsonInt(String json, String key, int defaultValue) {
        if (json == null) return defaultValue;
        String pattern = "\"" + key + "\"\\s*:\\s*(\\d+)";
        java.util.regex.Matcher m = java.util.regex.Pattern.compile(pattern).matcher(json);
        if (m.find()) {
            try { return Integer.parseInt(m.group(1)); }
            catch (NumberFormatException e) { return defaultValue; }
        }
        return defaultValue;
    }

    /** 检查context中是否包含某个维度作为弱维度 */
    private boolean hasWeakDimension(String contextJson, String dimension) {
        if (contextJson == null || dimension == null) return false;
        // 检查 weakDimensions 数组或 score<=2 的维度
        if (contextJson.contains("\"weakDimensions\"") && contextJson.contains(dimension))
            return true;
        if ((contextJson.contains("\"score\":2") || contextJson.contains("\"score\":1"))
            && contextJson.contains(dimension))
            return true;
        return false;
    }

    /** 检查指定类型的模式是否已存在（去重） */
    private boolean patternExists(Long tenantId, String patternType, String nameKeyword) {
        List<Map<String, Object>> existing = cognitiveMapper.findPatternsByType(
            tenantId, patternType);
        if (existing == null) return false;
        return existing.stream().anyMatch(p -> {
            Object name = p.get("patternName");
            return name != null && name.toString().contains(nameKeyword);
        });
    }

    // ---- 原有工具方法 ----

    private List<Long> getActiveTenantIds() {
        return tenantResolver.getActiveTenantIds();
    }

    private String extractSource(String contextJson) {
        if (contextJson == null) return null;
        // 优先从JSON字段读取
        String src = extractJsonField(contextJson, "source");
        if (src != null && !src.isEmpty() && !"未知".equals(src)) return src;
        // 兜底关键词匹配
        if (contextJson.contains("内推") || contextJson.contains("referral")) return "内推";
        if (contextJson.contains("猎聘") || contextJson.contains("liepin")) return "猎聘";
        if (contextJson.contains("BOSS") || contextJson.contains("boss")) return "BOSS直聘";
        if (contextJson.contains("猎头") || contextJson.contains("headhunter")) return "猎头";
        if (contextJson.contains("官网") || contextJson.contains("career")) return "官网";
        return "其他";
    }

    private String extractCompany(String contextJson) {
        if (contextJson == null) return null;
        String comp = extractJsonField(contextJson, "company");
        if (comp != null && !comp.isEmpty() && !"未知".equals(comp)) return comp;
        // 兜底关键词
        String[] keywords = {"阿里", "腾讯", "字节", "美团", "百度", "华为", "蚂蚁", "京东", "拼多多", "快手"};
        for (String kw : keywords)
            if (contextJson.contains(kw)) return kw;
        return null;
    }

    private Set<String> extractEvaluatorNames(String contextJson) {
        Set<String> names = new LinkedHashSet<>();
        if (contextJson == null) return names;
        String name = extractJsonField(contextJson, "interviewerName");
        if (name != null && !name.isEmpty()) {
            names.add(name);
            return names;
        }
        // 兜底
        String[] known = {"张三", "李四", "王五", "赵六", "陈七"};
        for (String n : known)
            if (contextJson.contains(n)) names.add(n);
        return names;
    }

    private Set<String> extractWeakDimensions(String contextJson) {
        Set<String> dims = new LinkedHashSet<>();
        if (contextJson == null) return dims;
        // 尝试提取 weakDimensions JSON数组
        String arrPattern = "\"weakDimensions\"\\s*:\\s*\\[([^\\]]+)\\]";
        java.util.regex.Matcher m = java.util.regex.Pattern.compile(arrPattern).matcher(contextJson);
        if (m.find()) {
            String arr = m.group(1);
            String[] items = arr.split(",");
            for (String item : items) {
                String cleaned = item.replaceAll("[\"\\[\\]\\s]", "");
                if (!cleaned.isEmpty()) dims.add(cleaned);
            }
        }
        // 兜底
        String[] dimensions = {"Craft", "技术深度", "跨部门协作", "Leadership",
            "Culture", "Execution", "用户洞察", "数据驱动", "Product Sense", "架构能力"};
        for (String dim : dimensions) {
            if (hasWeakDimension(contextJson, dim)) dims.add(dim);
        }
        return dims;
    }

    private int extractDaysToOffer(String contextJson) {
        return extractJsonInt(contextJson, "daysToOffer", 45);
    }

    private List<Long> extractEventIdsByKeyword(List<Map<String, Object>> events, String keyword) {
        List<Long> ids = new ArrayList<>();
        for (Map<String, Object> evt : events) {
            String ctx = (String) evt.get("contextJson");
            if (ctx != null && ctx.contains(keyword))
                ids.add((Long) evt.get("id"));
        }
        return ids;
    }

    private List<Long> extractAllIds(List<Map<String, Object>> events) {
        List<Long> ids = new ArrayList<>();
        for (Map<String, Object> evt : events)
            ids.add((Long) evt.get("id"));
        return ids;
    }
}
