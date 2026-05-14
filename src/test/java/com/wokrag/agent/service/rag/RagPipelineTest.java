package com.wokrag.agent.service.rag;

import com.wokrag.agent.client.SiliconFlowClient;
import com.wokrag.agent.model.RagResponse;
import com.wokrag.agent.model.SearchResult;
import com.wokrag.agent.service.embedding.EmbeddingService;
import com.wokrag.agent.service.generation.LlmService;
import com.wokrag.agent.service.generation.PromptService;
import com.wokrag.agent.service.memory.SessionMemoryService;
import com.wokrag.agent.service.retrieval.HybridSearchService;
import com.wokrag.agent.service.rewrite.QueryRewriter;
import com.wokrag.agent.service.tool.FunctionCallService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

class RagPipelineTest {

    @Mock
    private EmbeddingService embeddingService;
    @Mock
    private HybridSearchService hybridSearchService;
    @Mock
    private LlmService llmService;
    @Mock
    private SessionMemoryService sessionMemoryService;
    @Mock
    private QueryRewriter queryRewriter;
    @Mock
    private FunctionCallService functionCallService;
    @Mock
    private SiliconFlowClient siliconFlowClient;
    @Mock
    private PromptService promptService;

    private RagPipeline ragPipeline;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        ragPipeline = new RagPipeline(
                embeddingService, hybridSearchService, llmService,
                sessionMemoryService, queryRewriter, functionCallService,
                siliconFlowClient, promptService);
    }

    @Test
    void testPipelineCreation() {
        assertNotNull(ragPipeline);
    }

    @Test
    void testExecutePipeline() {
        double[] mockVector = new double[]{0.1, 0.2, 0.3};
        when(embeddingService.embed(anyString())).thenReturn(mockVector);

        SearchResult searchResult = new SearchResult();
        searchResult.setChunkId("chunk-1");
        searchResult.setContent("Test content");
        searchResult.setScore(0.95);
        searchResult.setMetadata(Map.of("source", "test.pdf"));
        when(hybridSearchService.hybridSearch(any(), anyString()))
                .thenReturn(List.of(searchResult));

        when(queryRewriter.rewrite(any(), anyString())).thenReturn("Test question");
        when(promptService.getSystemPrompt()).thenReturn("System prompt");
        when(promptService.buildUserPrompt(any(), anyString())).thenReturn("User prompt");
        when(functionCallService.chatWithTools(anyString(), anyString()))
                .thenReturn("Test answer [1]");

        RagResponse result = ragPipeline.execute("Test question");

        assertNotNull(result);
        assertEquals("Test answer [1]", result.getAnswer());
    }

    @Test
    void testExecuteWithSessionId() {
        when(embeddingService.embed(anyString())).thenReturn(new double[]{0.1});
        when(hybridSearchService.hybridSearch(any(), anyString())).thenReturn(List.of());
        when(queryRewriter.rewrite(any(), anyString())).thenReturn("Test question");
        when(functionCallService.chatWithTools(anyString(), anyString()))
                .thenReturn("Direct answer");

        RagResponse result = ragPipeline.execute("Test question", "session-1");

        assertNotNull(result);
        assertEquals("session-1", result.getSessionId());
    }
}
