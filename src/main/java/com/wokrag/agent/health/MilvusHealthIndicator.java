package com.wokrag.agent.health;

import com.wokrag.agent.client.MilvusClientWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class MilvusHealthIndicator implements HealthIndicator {

    private final MilvusClientWrapper milvusClient;

    @Override
    public Health health() {
        try {
            boolean healthy = milvusClient.isHealthy();
            if (healthy) {
                return Health.up()
                        .withDetail("database", "Milvus")
                        .withDetail("collection", "accessible")
                        .build();
            }
            return Health.down()
                    .withDetail("database", "Milvus")
                    .withDetail("reason", "Collection not accessible")
                    .build();
        } catch (Exception e) {
            log.warn("Milvus health check failed", e);
            return Health.down()
                    .withDetail("database", "Milvus")
                    .withException(e)
                    .build();
        }
    }
}
