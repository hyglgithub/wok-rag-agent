package com.wokrag.agent.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "rag")
public class RagConfig {
    private int chunkSize;
    private int chunkOverlap;
    private int topK;
    private int denseRecallTopK;
    private int sparseRecallTopK;
    private int rrfK;
}
