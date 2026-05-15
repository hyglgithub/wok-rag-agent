package com.wokrag.agent.health;

import com.wokrag.agent.client.MilvusClientWrapper;
import com.wokrag.agent.client.SiliconFlowClient;
import com.wokrag.agent.config.SiliconFlowConfig;
import org.junit.jupiter.api.Test;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class HealthIndicatorTest {

    @Test
    void testMilvusHealthUp() {
        MilvusClientWrapper client = mock(MilvusClientWrapper.class);
        when(client.isHealthy()).thenReturn(true);

        MilvusHealthIndicator indicator = new MilvusHealthIndicator(client);
        Health health = indicator.health();

        assertEquals(Status.UP, health.getStatus());
    }

    @Test
    void testMilvusHealthDown() {
        MilvusClientWrapper client = mock(MilvusClientWrapper.class);
        when(client.isHealthy()).thenReturn(false);

        MilvusHealthIndicator indicator = new MilvusHealthIndicator(client);
        Health health = indicator.health();

        assertEquals(Status.DOWN, health.getStatus());
    }

    @Test
    void testMilvusHealthException() {
        MilvusClientWrapper client = mock(MilvusClientWrapper.class);
        when(client.isHealthy()).thenThrow(new RuntimeException("Connection refused"));

        MilvusHealthIndicator indicator = new MilvusHealthIndicator(client);
        Health health = indicator.health();

        assertEquals(Status.DOWN, health.getStatus());
    }

    @Test
    void testSiliconFlowHealthIndicatorConstructs() {
        SiliconFlowClient client = mock(SiliconFlowClient.class);
        SiliconFlowConfig config = new SiliconFlowConfig();
        config.setBaseUrl("https://api.siliconflow.cn/v1");
        config.setApiKey("test-key");

        SiliconFlowHealthIndicator indicator = new SiliconFlowHealthIndicator(client, config);
        assertNotNull(indicator);
    }
}
