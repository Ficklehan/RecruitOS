package com.recruitos.agent.service;

import com.recruitos.agent.entity.AgentAccount;
import com.recruitos.agent.mapper.AgentAccountMapper;
import com.recruitos.agent.platform.RpaPlatformBridge;
import com.recruitos.agent.rpa.PlaywrightManager;
import com.recruitos.agent.rpa.RpaSessionStorage;
import com.recruitos.common.exception.BizException;
import com.recruitos.common.tenant.TenantContext;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
public class RpaLoginService {

    @Resource
    private AgentAccountMapper accountMapper;
    @Resource
    private RpaPlatformBridge rpaBridge;
    @Resource
    private RpaSafetyService rpaSafetyService;
    @Resource
    private RpaSessionStorage sessionStorage;
    @Resource
    private PlaywrightManager playwrightManager;

    public Map<String, Object> interactiveLogin(Long accountId) {
        if (!rpaSafetyService.isPlatformAccessAllowed()) {
            throw new BizException(rpaSafetyService.blockedMessage());
        }
        AgentAccount account = requireAccount(accountId);
        rpaBridge.login(account);
        account.setLastActiveAt(LocalDateTime.now());
        accountMapper.updateById(account);

        Map<String, Object> result = new HashMap<>();
        result.put("accountId", accountId);
        result.put("platform", account.getPlatform());
        result.put("sessionSaved", sessionStorage.hasSession(account));
        result.put("message", "平台登录成功，会话已保存");
        return result;
    }

    public Map<String, Object> testSession(Long accountId) {
        AgentAccount account = requireAccount(accountId);
        Map<String, Object> result = new HashMap<>();
        result.put("accountId", accountId);
        result.put("platform", account.getPlatform());
        result.put("sessionExists", sessionStorage.hasSession(account));
        Map<String, Object> safety = rpaSafetyService.status();
        result.put("rpaEnabled", safety.get("enabled"));
        result.put("platformAccessEnabled", safety.get("platformAccessEnabled"));
        result.put("platformAccessAllowed", safety.get("platformAccessAllowed"));
        result.put("testingLocked", safety.get("testingLocked"));
        if (Boolean.TRUE.equals(safety.get("platformAccessAllowed"))) {
            try {
                rpaBridge.login(account);
                result.put("loggedIn", true);
                result.put("message", "会话有效");
            } catch (Exception e) {
                result.put("loggedIn", false);
                result.put("message", e.getMessage());
            }
        } else {
            result.put("loggedIn", false);
            result.put("message", "测试模式：未连接真实平台，仅检查本地会话文件");
        }
        return result;
    }

    public void logoutSession(Long accountId) {
        AgentAccount account = requireAccount(accountId);
        playwrightManager.closeContext(accountId);
        sessionStorage.deleteSession(account);
    }

    public Map<String, Object> status() {
        return rpaSafetyService.status();
    }

    private AgentAccount requireAccount(Long accountId) {
        Long tenantId = TenantContext.getTenantId();
        AgentAccount account = accountMapper.selectById(accountId);
        if (account == null || !account.getTenantId().equals(tenantId)) {
            throw new BizException("账号不存在");
        }
        if (!"BOSS".equalsIgnoreCase(account.getPlatform()) && !"LIEPIN".equalsIgnoreCase(account.getPlatform())) {
            throw new BizException("当前仅支持 BOSS / LIEPIN 真实 RPA");
        }
        return account;
    }
}
