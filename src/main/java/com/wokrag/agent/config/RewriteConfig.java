package com.wokrag.agent.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "rewrite")
public class RewriteConfig {
    private boolean enabled;
    private String model;
}
