package com.wokrag.agent.service.rag;

import com.wokrag.agent.client.SiliconFlowClient;
import com.wokrag.agent.model.RagResponse;
import com.wokrag.agent.model.SearchResult;
import com.wokrag.agent.service.embedding.EmbeddingService;
import com.wokrag.agent.service.generation.LlmService;
import com.wokrag.agent.service.generation.PromptService;
import com.wokrag.agent.service.intent.IntentClassifier;
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
import static org.mockito.Mockito.*;

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
    @Mock
    private IntentClassifier intentClassifier;

    private RagPipeline ragPipeline;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        ragPipeline = new RagPipeline(
                embeddingService, hybridSearchService, llmService,
                sessionMemoryService, queryRewriter, functionCallService,
                siliconFlowClient, promptService, intentClassifier);

        // Default: classify as knowledge intent
        when(intentClassifier.classify(any(), anyString()))
                .thenReturn(new IntentClassifier.IntentResult(IntentClassifier.INTENT_KNOWLEDGE, 0.95));
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

    @Test
    void testToolIntentSkipsRewriteAndEmbedding() {
        when(intentClassifier.classify(any(), anyString()))
                .thenReturn(new IntentClassifier.IntentResult(IntentClassifier.INTENT_TOOL, 0.95));
        when(promptService.getSystemPrompt()).thenReturn("System prompt");
        when(functionCallService.chatWithTools(anyString(), anyString()))
                .thenReturn("Order status result");

        RagResponse result = ragPipeline.execute("查一下我的年假", "session-1");

        assertNotNull(result);
        assertEquals("Order status result", result.getAnswer());
        verify(queryRewriter, never()).rewrite(any(), anyString());
        verify(embeddingService, never()).embed(anyString());
    }

    @Test
    void testChitchatIntentUsesReply() {
        when(intentClassifier.classify(any(), anyString()))
                .thenReturn(new IntentClassifier.IntentResult(IntentClassifier.INTENT_CHITCHAT, 0.95, "你好！今天有什么可以帮您的？"));

        RagResponse result = ragPipeline.execute("今天心情不好", "session-1");

        assertNotNull(result);
        assertEquals("你好！今天有什么可以帮您的？", result.getAnswer());
        verify(siliconFlowClient, never()).chat(anyString(), anyString());
        verify(queryRewriter, never()).rewrite(any(), anyString());
        verify(embeddingService, never()).embed(anyString());
    }

    @Test
    void testChitchatIntentNullReplyUsesHardcoded() {
        when(intentClassifier.classify(any(), anyString()))
                .thenReturn(new IntentClassifier.IntentResult(IntentClassifier.INTENT_CHITCHAT, 0.99));

        RagResponse result = ragPipeline.execute("你好", "session-1");

        assertNotNull(result);
        assertEquals("您好！请问有什么可以帮您的？", result.getAnswer());
        verify(siliconFlowClient, never()).chat(anyString(), anyString());
    }

    @Test
    void testClarificationIntentUsesReply() {
        when(intentClassifier.classify(any(), anyString()))
                .thenReturn(new IntentClassifier.IntentResult(IntentClassifier.INTENT_CLARIFICATION, 0.90, "您想了解哪方面的信息呢？产品、订单还是售后？"));

        RagResponse result = ragPipeline.execute("有什么推荐的", "session-1");

        assertNotNull(result);
        assertEquals("您想了解哪方面的信息呢？产品、订单还是售后？", result.getAnswer());
        verify(siliconFlowClient, never()).chat(anyString(), anyString());
        verify(functionCallService, never()).chatWithTools(anyString(), anyString());
    }
}
