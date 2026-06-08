package com.recruitos.agent.platform;

import com.recruitos.common.exception.BizException;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class PlatformAdapterRegistry {

    @Resource
    private List<PlatformAdapter> adapters;

    public PlatformAdapter get(String platform) {
        for (PlatformAdapter adapter : adapters) {
            if (adapter.platform().equalsIgnoreCase(platform)) {
                return adapter;
            }
        }
        throw new BizException("Unsupported platform: " + platform);
    }

    public Map<String, PlatformAdapter> asMap() {
        Map<String, PlatformAdapter> map = new HashMap<>();
        for (PlatformAdapter adapter : adapters) {
            map.put(adapter.platform(), adapter);
        }
        return map;
    }
}
