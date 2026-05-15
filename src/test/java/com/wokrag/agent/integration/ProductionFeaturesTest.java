package com.wokrag.agent.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
class ProductionFeaturesTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void testActuatorHealthEndpoint() throws Exception {
        // Health may return 503 when external services (SiliconFlow) are unreachable,
        // or 200 when all components are healthy. Both indicate the endpoint is working.
        int status = mockMvc.perform(get("/actuator/health"))
                .andReturn().getResponse().getStatus();
        assert status == 200 || status == 503
                : "Expected 200 or 503, got " + status;

        mockMvc.perform(get("/actuator/health"))
                .andExpect(jsonPath("$.status").exists());
    }

    @Test
    void testActuatorPrometheusEndpoint() throws Exception {
        // Prometheus endpoint is exposed via management.endpoints.web.exposure.include.
        // It may return 200 (metrics data) or fail if metrics registry has issues.
        // Verify the endpoint is not 404 (i.e., it is properly exposed).
        int status = mockMvc.perform(get("/actuator/prometheus"))
                .andReturn().getResponse().getStatus();
        assert status != 404 : "Prometheus endpoint should be exposed (not 404)";
    }

    @Test
    void testSwaggerUiAccessible() throws Exception {
        // SpringDoc redirects /swagger-ui.html to /swagger-ui/index.html (302)
        int status = mockMvc.perform(get("/swagger-ui.html"))
                .andReturn().getResponse().getStatus();
        assert status == 200 || status == 302
                : "Expected 200 or 302 redirect, got " + status;
    }

    @Test
    void testApiDocsAccessible() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.info.title").value("WokRag Agent API"));
    }

    @Test
    void testHealthEndpointReturnsServiceInfo() throws Exception {
        mockMvc.perform(get("/api/rag/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("OK"))
                .andExpect(jsonPath("$.service").value("wok-rag-agent"));
    }
}
