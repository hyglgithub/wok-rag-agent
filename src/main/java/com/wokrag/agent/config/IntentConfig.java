package com.wokrag.agent.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "intent")
public class IntentConfig {
    private boolean enabled = true;
    private String model = "Qwen/Qwen2.5-7B-Instruct";
    private double confidenceThreshold = 0.5;
}
