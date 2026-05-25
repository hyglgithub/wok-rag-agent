package com.wokrag.agent.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "siliconflow")
public class SiliconFlowConfig {
    private String apiKey;
    private String baseUrl;
    private String embeddingModel;
    private String chatModel;
    private String rerankerModel;
}
