package com.recruitos.brain.scheduler;

import com.recruitos.brain.mapper.BrainMapper;
import com.recruitos.brain.ml.MlModelService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 模型自动重训练调度器 v2 — 真实调用 Python 训练管道。
 * <p>
 * 策略：
 * - 每日凌晨 3:00 检查新样本数
 * - 累积 >= 100 个新样本 → 调用 Python 脚本训练
 * - 训练脚本内部对比 AUC，仅提升时产出新模型
 * - 结果写入 brain_llm_trace 审计表
 */
@Component
public class ModelRetrainingScheduler {
    private static final Logger log = LoggerFactory.getLogger(ModelRetrainingScheduler.class);

    @Value("${recruitos.brain.ml.retrain.min-samples:100}")
    private int minNewSamples;

    @Value("${recruitos.brain.ml.retrain.python-cmd:python3}")
    private String pythonCmd;

    @Value("${recruitos.brain.ml.retrain.script-path:ml-training/train_intent_model.py}")
    private String scriptPath;

    @Value("${recruitos.brain.ml.retrain.output-dir:models}")
    private String outputDir;

    @Value("${recruitos.brain.ml.retrain.db-url:}")
    private String dbUrl;

    @Resource
    private BrainMapper brainMapper;

    @Resource
    private MlModelService mlModelService;

    private int sampleCountAtLastTrain = 0;

    @PostConstruct
    void init() {
        sampleCountAtLastTrain = countTotalDecisions();
        log.info("ModelRetrainingScheduler initialized. Current samples: {}, threshold: {}",
            sampleCountAtLastTrain, minNewSamples);
    }

    @Scheduled(cron = "0 0 3 * * ?")
    public void checkAndRetrain() {
        log.info("Model retraining check at {}", LocalDateTime.now());
        try {
            int total = countTotalDecisions();
            int newSamples = total - sampleCountAtLastTrain;
            log.info("Samples: total={}, new={}, threshold={}", total, newSamples, minNewSamples);

            if (newSamples >= minNewSamples) {
                log.info("Triggering retraining with {} new samples", newSamples);
                triggerRetraining(newSamples);
            }
        } catch (Exception e) {
            log.error("Model retraining check failed", e);
        }
    }

    public Map<String, Object> forceRetrain() {
        return triggerRetraining(countTotalDecisions());
    }

    private Map<String, Object> triggerRetraining(int newSamples) {
        String retrainId = "retrain_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("retrainId", retrainId);

        try {
            // 1. 写入审计日志：训练开始
            Map<String, Object> trace = new LinkedHashMap<>();
            trace.put("id", brainMapper.nextId("brain_llm_trace"));
            trace.put("tenantId", 0L);
            trace.put("traceType", "MODEL_RETRAIN_START");
            trace.put("requestJson", "{\"newSamples\":" + newSamples + ",\"retrainId\":\"" + retrainId + "\"}");
            trace.put("latencyMs", 0L);
            brainMapper.insertLlmTrace(trace);

            // 2. 调用 Python 训练脚本
            List<String> cmd = buildCommand();
            log.info("Executing: {}", String.join(" ", cmd));

            ProcessBuilder pb = new ProcessBuilder(cmd);
            pb.directory(new File(scriptPath).getParentFile() != null
                ? new File(scriptPath).getParentFile()
                : new File("."));
            pb.redirectErrorStream(true);

            Process process = pb.start();
            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                    log.debug("[retrain] {}", line);
                }
            }

            int exitCode = process.waitFor();
            String stdout = output.toString();

            // 3. 解析结果
            boolean success = exitCode == 0;
            String modelVersion = mlModelService.getActiveModelVersion();

            if (success) {
                sampleCountAtLastTrain = countTotalDecisions();
                log.info("Retraining SUCCESS: retrainId={}, samples={}", retrainId, sampleCountAtLastTrain);
                result.put("status", "COMPLETED");
                result.put("message", "模型训练完成");
                result.put("newSamples", newSamples);

                // 尝试从输出中提取 AUC
                for (String line : stdout.split("\n")) {
                    if (line.contains("AUC:")) {
                        result.put("auc", line.substring(line.indexOf("AUC:") + 4).trim().split(" ")[0]);
                    }
                }
            } else if (exitCode == 2) {
                log.info("Retraining skipped: insufficient samples");
                result.put("status", "SKIPPED");
                result.put("message", "样本不足（< " + minNewSamples + "）");
            } else if (exitCode == 3) {
                log.info("Retraining completed but model not improved");
                result.put("status", "NO_IMPROVEMENT");
                result.put("message", "新模型 AUC 未超过当前模型");
            } else {
                log.warn("Retraining FAILED: exitCode={}", exitCode);
                result.put("status", "FAILED");
                result.put("message", "训练脚本异常退出: " + exitCode);
            }

            result.put("output", stdout.length() > 500 ? stdout.substring(0, 500) + "..." : stdout);

            // 4. 写入审计日志：训练结束
            trace.put("id", brainMapper.nextId("brain_llm_trace"));
            trace.put("tenantId", 0L);
            trace.put("traceType", "MODEL_RETRAIN_END");
            trace.put("requestJson", "{\"retrainId\":\"" + retrainId + "\",\"exitCode\":" + exitCode + "}");
            trace.put("responseJson", "{\"status\":\"" + result.get("status") + "\"}");
            trace.put("latencyMs", 0L);
            brainMapper.insertLlmTrace(trace);

        } catch (Exception e) {
            log.error("Retraining failed: {}", retrainId, e);
            result.put("status", "FAILED");
            result.put("error", e.getMessage());
        }

        result.put("activeModelVersion", mlModelService.getActiveModelVersion());
        return result;
    }

    private List<String> buildCommand() {
        List<String> cmd = new ArrayList<>();
        cmd.add(pythonCmd);
        cmd.add(scriptPath);
        cmd.add("--output-dir");
        cmd.add(outputDir);
        cmd.add("--min-samples");
        cmd.add(String.valueOf(minNewSamples));

        if (dbUrl != null && !dbUrl.isEmpty()) {
            cmd.add("--db-url");
            cmd.add(dbUrl);
        } else {
            // 无 DB 配置时使用合成数据训练（开发/测试模式）
            cmd.add("--synthetic");
        }
        return cmd;
    }

    private int countTotalDecisions() {
        try {
            return brainMapper.countDecisions();
        } catch (Exception e) {
            return 0;
        }
    }

    public int getPendingSampleCount() {
        return Math.max(0, countTotalDecisions() - sampleCountAtLastTrain);
    }
}
