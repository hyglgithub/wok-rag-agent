package com.wokrag.agent.service.rag;

import com.wokrag.agent.model.RagResponse;
import com.wokrag.agent.model.SearchResult;
import com.wokrag.agent.service.embedding.EmbeddingService;
import com.wokrag.agent.service.generation.LlmService;
import com.wokrag.agent.service.retrieval.HybridSearchService;
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

    private RagPipeline ragPipeline;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        ragPipeline = new RagPipeline(embeddingService, hybridSearchService, llmService);
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

        RagResponse mockResponse = new RagResponse();
        mockResponse.setAnswer("Test answer [1]");
        mockResponse.setCitations(List.of());
        when(llmService.generateWithCitations(any(), anyString()))
                .thenReturn(mockResponse);

        RagResponse result = ragPipeline.execute("Test question");

        assertNotNull(result);
        assertEquals("Test answer [1]", result.getAnswer());
    }
}
