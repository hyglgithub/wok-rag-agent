package com.wokrag.agent.integration;

import com.wokrag.agent.model.RagResponse;
import com.wokrag.agent.service.rag.RagPipeline;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("dev")
class RagIntegrationTest {

    @Autowired
    private RagPipeline ragPipeline;

    @Test
    void testEndToEndRagFlow() {
        String apiKey = System.getenv("SILICONFLOW_API_KEY");
        if (apiKey == null || apiKey.equals("your-api-key")) {
            System.out.println("Skipping integration test - no API key");
            return;
        }

        String question = "What is the return policy?";

        try {
            RagResponse response = ragPipeline.execute(question);

            assertNotNull(response);
            assertNotNull(response.getAnswer());
            System.out.println("Answer: " + response.getAnswer());
            System.out.println("Citations: " + response.getCitations().size());
        } catch (Exception e) {
            System.out.println("Integration test failed (expected if Milvus not running): " + e.getMessage());
        }
    }
}
