package com.recruitos.agent.rpa;

import com.recruitos.agent.entity.AgentAccount;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Component
public class RpaSessionStorage {

    @Resource
    private RpaProperties properties;

    public Path sessionFile(AgentAccount account) {
        Path dir = Paths.get(properties.getSessionDir(),
                String.valueOf(account.getTenantId()),
                account.getPlatform().toLowerCase(),
                String.valueOf(account.getId()));
        try {
            Files.createDirectories(dir);
        } catch (Exception ignored) {
            // best effort
        }
        return dir.resolve("storage-state.json");
    }

    public Path downloadDir(AgentAccount account) {
        Path dir = sessionFile(account).getParent().resolve("downloads");
        try {
            Files.createDirectories(dir);
        } catch (Exception ignored) {
            // best effort
        }
        return dir;
    }

    public boolean hasSession(AgentAccount account) {
        File f = sessionFile(account).toFile();
        return f.exists() && f.length() > 10;
    }

    public void deleteSession(AgentAccount account) {
        try {
            Files.deleteIfExists(sessionFile(account));
        } catch (Exception ignored) {
            // best effort
        }
    }
}
