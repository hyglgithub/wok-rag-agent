package com.wokrag.agent.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "security.api-key")
public class ApiKeyConfig {
    
    private boolean enabled;
}
