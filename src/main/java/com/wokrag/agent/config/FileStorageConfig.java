package com.wokrag.agent.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "file.storage")
public class FileStorageConfig {
    private String path = "data/documents/";
}
