package com.wokrag.agent.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "milvus")
public class MilvusConfig {
    private String uri;
    private String collectionName;
    private int vectorDim;
}
