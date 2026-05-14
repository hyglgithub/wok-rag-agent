package com.wokrag.agent.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "tool")
public class ToolConfig {
    private boolean enabled = false;
    private String model = "Qwen/Qwen2.5-7B-Instruct";
}
