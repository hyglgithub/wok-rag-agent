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
class EndToEndTest {

    @Autowired
    private RagPipeline ragPipeline;

    @Test
    void testQueryAboutReturnPolicy() {
        String question = "退货政策是什么？";

        System.out.println("=== Query: " + question + " ===");
        RagResponse response = ragPipeline.execute(question);

        System.out.println("\n--- Answer ---");
        System.out.println(response.getAnswer());

        System.out.println("\n--- Citations ---");
        if (response.getCitations() != null) {
            for (RagResponse.CitationInfo citation : response.getCitations()) {
                System.out.printf("[%d] %s | %s%n",
                        citation.getIndex(), citation.getSource(), citation.getChunkContent().substring(0, Math.min(50, citation.getChunkContent().length())));
            }
        }

        assertNotNull(response.getAnswer(), "Answer should not be null");
        assertFalse(response.getAnswer().isEmpty(), "Answer should not be empty");
        System.out.println("\n=== End-to-end test PASSED ===");
    }

    @Test
    void testQueryAboutPayment() {
        String question = "支持哪些支付方式？";

        System.out.println("=== Query: " + question + " ===");
        RagResponse response = ragPipeline.execute(question);

        System.out.println("\n--- Answer ---");
        System.out.println(response.getAnswer());

        assertNotNull(response.getAnswer(), "Answer should not be null");
        System.out.println("\n=== Payment query test PASSED ===");
    }
}
