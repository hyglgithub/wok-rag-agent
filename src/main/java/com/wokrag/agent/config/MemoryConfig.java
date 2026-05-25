package com.wokrag.agent.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "memory")
public class MemoryConfig {
    private int maxRounds;
    private int tokenThreshold;
    private int sessionTimeoutMinutes;
    private String summaryModel;
}
