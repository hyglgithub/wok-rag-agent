package com.wokrag.agent.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "siliconflow")
public class SiliconFlowConfig {
    private String apiKey;
    private String baseUrl;
    private String embeddingModel;
    private String chatModel;
    private String rerankerModel;
}
