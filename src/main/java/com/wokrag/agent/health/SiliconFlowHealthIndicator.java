package com.wokrag.agent.health;

import com.wokrag.agent.client.SiliconFlowClient;
import com.wokrag.agent.config.SiliconFlowConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class SiliconFlowHealthIndicator implements HealthIndicator {

    private final SiliconFlowClient client;
    private final SiliconFlowConfig config;

    @Override
    public Health health() {
        try {
            client.embed(java.util.List.of("health"));
            return Health.up()
                    .withDetail("api", "SiliconFlow")
                    .withDetail("baseUrl", config.getBaseUrl())
                    .build();
        } catch (Exception e) {
            log.warn("SiliconFlow health check failed: {}", e.getMessage());
            return Health.down()
                    .withDetail("api", "SiliconFlow")
                    .withDetail("baseUrl", config.getBaseUrl())
                    .withException(e)
                    .build();
        }
    }
}
