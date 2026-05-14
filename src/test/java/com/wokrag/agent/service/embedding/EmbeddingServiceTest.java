package com.wokrag.agent.service.embedding;

import com.wokrag.agent.client.SiliconFlowClient;
import com.wokrag.agent.config.SiliconFlowConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

class EmbeddingServiceTest {

    @Mock
    private SiliconFlowClient client;

    @Mock
    private SiliconFlowConfig config;

    private EmbeddingService embeddingService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        embeddingService = new EmbeddingServiceImpl(client, config);
    }

    @Test
    void testEmbedSingleText() {
        double[] mockVector = new double[]{0.1, 0.2, 0.3};
        when(client.embed(List.of("test text"))).thenReturn(List.of(mockVector));

        double[] result = embeddingService.embed("test text");

        assertNotNull(result);
        assertEquals(3, result.length);
    }

    @Test
    void testEmbedBatch() {
        List<double[]> mockVectors = List.of(
                new double[]{0.1, 0.2, 0.3},
                new double[]{0.4, 0.5, 0.6}
        );
        when(client.embed(List.of("text1", "text2"))).thenReturn(mockVectors);

        List<double[]> results = embeddingService.embedBatch(List.of("text1", "text2"));

        assertEquals(2, results.size());
    }
}
