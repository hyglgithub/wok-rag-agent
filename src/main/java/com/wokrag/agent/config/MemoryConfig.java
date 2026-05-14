package com.wokrag.agent.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "memory")
public class MemoryConfig {
    private String strategy = "sliding_window";
    private int maxRounds = 5;
    private int tokenThreshold = 3000;
    private int sessionTimeoutMinutes = 30;
}
