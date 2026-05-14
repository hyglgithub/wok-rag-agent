package com.wokrag.agent.service.generation;

import com.wokrag.agent.client.SiliconFlowClient;
import com.wokrag.agent.model.Chunk;
import com.wokrag.agent.model.RagResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

class GenerationServiceTest {

    @Mock
    private SiliconFlowClient client;

    private LlmService llmService;
    private PromptService promptService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        llmService = new LlmServiceImpl(client);
        promptService = new PromptService();
    }

    @Test
    void testPromptBuilding() {
        Chunk chunk = new Chunk();
        chunk.setId("1");
        chunk.setContent("Test content");
        chunk.setSource("test.pdf");
        chunk.setUpdateTime("2026-05-14");

        String prompt = promptService.buildUserPrompt(List.of(chunk), "Test question");

        assertTrue(prompt.contains("Test content"));
        assertTrue(prompt.contains("Test question"));
        assertTrue(prompt.contains("[1]"));
    }

    @Test
    void testLlmServiceCreation() {
        assertNotNull(llmService);
    }
}
