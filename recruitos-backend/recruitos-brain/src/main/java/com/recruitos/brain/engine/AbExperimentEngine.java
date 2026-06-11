package com.recruitos.brain.engine;

import org.springframework.stereotype.Component;
import java.util.*;

@Component
public class AbExperimentEngine {

    /**
     * 判断某个 jobId 是否应分配到实验组。
     * 使用 jobId 的哈希做确定性分组，确保同一 job 始终在同一组。
     */
    public boolean isInExperimentGroup(Long jobId, String experimentKey, double trafficSplit) {
        if (trafficSplit <= 0) return false;
        if (trafficSplit >= 1.0) return true;
        int hash = Math.abs((jobId + experimentKey).hashCode());
        return (hash % 100) < (trafficSplit * 100);
    }

    /**
     * 两样本比例 Z 检验（简化版）。
     * 判断实验组和对照组是否有统计显著差异。
     * @return p-value 近似值
     */
    public double proportionZTest(int expSuccess, int expTotal, int ctrlSuccess, int ctrlTotal) {
        if (expTotal == 0 || ctrlTotal == 0) return 1.0;
        double p1 = (double) expSuccess / expTotal;
        double p2 = (double) ctrlSuccess / ctrlTotal;
        double pPool = (double) (expSuccess + ctrlSuccess) / (expTotal + ctrlTotal);
        double se = Math.sqrt(pPool * (1 - pPool) * (1.0 / expTotal + 1.0 / ctrlTotal));
        if (se < 1e-10) return 1.0;
        double z = (p1 - p2) / se;
        // 双边检验 p-value（正态近似）
        return 2 * (1 - normalCdf(Math.abs(z)));
    }

    /**
     * 标准正态 CDF（Abramowitz and Stegun 近似）
     */
    private double normalCdf(double x) {
        double[] a = {0.254829592, -0.284496736, 1.421413741, -1.453152027, 1.061405429};
        double p = 0.3275911;
        int sign = x < 0 ? -1 : 1;
        x = Math.abs(x) / Math.sqrt(2);
        double t = 1.0 / (1.0 + p * x);
        double y = 1.0 - ((((a[4] * t + a[3]) * t + a[2]) * t + a[1]) * t + a[0]) * t * Math.exp(-x * x);
        return 0.5 * (1.0 + sign * y);
    }

    public Map<String, Object> evaluate(int expSuccess, int expTotal, int ctrlSuccess, int ctrlTotal) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("experimentRate", expTotal > 0 ? Math.round(10000.0 * expSuccess / expTotal) / 100.0 : 0);
        result.put("controlRate", ctrlTotal > 0 ? Math.round(10000.0 * ctrlSuccess / ctrlTotal) / 100.0 : 0);
        result.put("lift", ctrlTotal > 0 && ctrlSuccess > 0
            ? Math.round(10000.0 * (((double) expSuccess / expTotal) / ((double) ctrlSuccess / ctrlTotal) - 1)) / 100.0 : 0);
        double pValue = proportionZTest(expSuccess, expTotal, ctrlSuccess, ctrlTotal);
        result.put("pValue", Math.round(pValue * 10000.0) / 10000.0);
        result.put("significant", pValue < 0.05);
        return result;
    }
}
