package com.recruitos.brain.engine;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.*;

/**
 * 行业冷启动模板服务 (T3.2)。
 * 为新租户提供基于行业的模型初始参数、特征权重和校准基准。
 * 
 * <p>当新客户数据不足时（< 100 samples），使用行业模板初始化 ML 模型。
 * 随着数据累积（≥ 100 samples），自动切换到租户专属模型。
 */
@Service
public class IndustryColdStartService {
    private static final Logger log = LoggerFactory.getLogger(IndustryColdStartService.class);

    /** 行业 → 模板映射 */
    private final Map<String, IndustryTemplate> templates = new LinkedHashMap<>();

    @PostConstruct
    public void init() {
        // 互联网/科技
        register("TECH", new IndustryTemplate()
            .industryName("互联网/科技")
            .featureWeights(buildWeights(
                "reply_speed_avg", 0.18, "interview_attendance", 0.14,
                "competing_offers", 0.13, "match_score", 0.12,
                "salary_gap_pct", 0.10, "job_hop_count_3y", 0.08,
                "question_count", 0.07, "pipeline_stage_days", 0.06,
                "market_hotness", 0.05, "current_company_tier", 0.04
            ))
            .calibrationBaseline(0.38)   // 行业平均Kappa
            .intentThreshold(55)          // 意向高分阈值
            .typicalCycleDays(28)         // 典型招聘周期
            .highDemandSkills("系统架构", "分布式系统", "Go", "Rust", "K8s", "AI/ML"));

        // 金融
        register("FINANCE", new IndustryTemplate()
            .industryName("金融")
            .featureWeights(buildWeights(
                "past_tenure_avg_months", 0.16, "salary_gap_pct", 0.15,
                "current_company_tier", 0.13, "interview_attendance", 0.11,
                "match_score", 0.10, "competing_offers", 0.08,
                "reply_speed_avg", 0.07, "job_hop_count_3y", 0.07,
                "stages_completed", 0.05, "market_hotness", 0.05
            ))
            .calibrationBaseline(0.32)
            .intentThreshold(60)
            .typicalCycleDays(42)
            .highDemandSkills("风控建模", "Java", "量化交易", "合规", "数据治理", "SQL"));

        // 制造业
        register("MANUFACTURING", new IndustryTemplate()
            .industryName("制造业")
            .featureWeights(buildWeights(
                "past_tenure_avg_months", 0.18, "match_score", 0.14,
                "salary_gap_pct", 0.12, "pipeline_stage_days", 0.10,
                "interview_attendance", 0.09, "stages_completed", 0.08,
                "job_hop_count_3y", 0.07, "reply_speed_avg", 0.06,
                "current_company_tier", 0.05, "question_count", 0.05
            ))
            .calibrationBaseline(0.30)
            .intentThreshold(55)
            .typicalCycleDays(35)
            .highDemandSkills("PLC编程", "机械设计", "供应链管理", "精益生产", "质量管理", "IoT"));

        // 医疗健康
        register("HEALTHCARE", new IndustryTemplate()
            .industryName("医疗健康")
            .featureWeights(buildWeights(
                "past_tenure_avg_months", 0.16, "match_score", 0.14,
                "interview_attendance", 0.12, "salary_gap_pct", 0.10,
                "job_hop_count_3y", 0.09, "stages_completed", 0.08,
                "question_count", 0.07, "current_company_tier", 0.07,
                "competing_offers", 0.06, "reply_speed_avg", 0.06
            ))
            .calibrationBaseline(0.35)
            .intentThreshold(58)
            .typicalCycleDays(38)
            .highDemandSkills("临床研究", "GMP", "医疗器械注册", "生物统计", "药物警戒", "Python"));

        // 消费品/零售
        register("RETAIL", new IndustryTemplate()
            .industryName("消费品/零售")
            .featureWeights(buildWeights(
                "reply_speed_avg", 0.15, "match_score", 0.13,
                "salary_gap_pct", 0.12, "interview_attendance", 0.11,
                "market_hotness", 0.10, "competing_offers", 0.08,
                "job_hop_count_3y", 0.08, "pipeline_stage_days", 0.07,
                "question_count", 0.06, "current_company_tier", 0.05
            ))
            .calibrationBaseline(0.33)
            .intentThreshold(52)
            .typicalCycleDays(25)
            .highDemandSkills("电商运营", "品牌营销", "供应链", "数据分析", "CRM", "直播运营"));

        log.info("IndustryColdStartService initialized with {} industry templates", templates.size());
    }

    private void register(String code, IndustryTemplate template) {
        template.code = code;
        templates.put(code, template);
    }

    /**
     * 获取行业模板。
     */
    public IndustryTemplate getTemplate(String industryCode) {
        return templates.getOrDefault(industryCode.toUpperCase(), templates.get("TECH"));
    }

    /**
     * 获取所有可用行业。
     */
    public List<Map<String, String>> listIndustries() {
        List<Map<String, String>> list = new ArrayList<>();
        for (IndustryTemplate t : templates.values()) {
            Map<String, String> m = new LinkedHashMap<>();
            m.put("code", t.code);
            m.put("name", t.industryName);
            m.put("typicalCycleDays", String.valueOf(t.typicalCycleDays));
            m.put("calibrationBaseline", String.format("%.2f", t.calibrationBaseline));
            list.add(m);
        }
        return list;
    }

    @SafeVarargs
    private static <K, V> Map<K, V> buildWeights(Object... pairs) {
        Map<K, V> map = new LinkedHashMap<>();
        for (int i = 0; i < pairs.length; i += 2) {
            @SuppressWarnings("unchecked")
            K key = (K) pairs[i];
            @SuppressWarnings("unchecked")
            V value = (V) pairs[i + 1];
            map.put(key, value);
        }
        return map;
    }

    /**
     * 行业模板
     */
    public static class IndustryTemplate {
        public String code;
        public String industryName;
        /** 特征权重（特征名 → 权重） */
        public Map<String, Double> featureWeights = new LinkedHashMap<>();
        /** 行业平均校准Kappa值 */
        public double calibrationBaseline;
        /** 意向高分阈值 */
        public double intentThreshold;
        /** 典型招聘周期（天） */
        public int typicalCycleDays;
        /** 高需求技能列表 */
        public List<String> highDemandSkills = new ArrayList<>();

        public IndustryTemplate industryName(String v) { this.industryName = v; return this; }
        public IndustryTemplate featureWeights(Map<String, Double> v) { this.featureWeights = v; return this; }
        public IndustryTemplate calibrationBaseline(double v) { this.calibrationBaseline = v; return this; }
        public IndustryTemplate intentThreshold(double v) { this.intentThreshold = v; return this; }
        public IndustryTemplate typicalCycleDays(int v) { this.typicalCycleDays = v; return this; }
        public IndustryTemplate highDemandSkills(String... skills) {
            this.highDemandSkills = Arrays.asList(skills); return this;
        }
    }
}
