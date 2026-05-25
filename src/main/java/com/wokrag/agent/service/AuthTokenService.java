package com.wokrag.agent.service;

import jakarta.annotation.PostConstruct;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.UUID;

@Slf4j
@Service
public class AuthTokenService {

    @Setter
    private String tokenFilePath = "data/auth-token.txt";

    private volatile String token;

    @PostConstruct
    public void init() {
        token = UUID.randomUUID().toString();
        try {
            Path path = Path.of(tokenFilePath);
            Files.createDirectories(path.getParent());
            Files.writeString(path, token);
        } catch (IOException e) {
            log.error("Failed to persist auth token to {}: {}", tokenFilePath, e.getMessage());
        }
        log.info("\n========================================\n  API Token: {}\n========================================", token);
    }

    public String getToken() {
        return token;
    }

    public boolean validate(String provided) {
        if (provided == null || provided.isBlank() || token == null) {
            return false;
        }
        try {
            return MessageDigest.isEqual(
                    token.getBytes(StandardCharsets.UTF_8),
                    provided.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            return false;
        }
    }
}
