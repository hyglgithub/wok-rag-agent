package com.wokrag.agent.service.retrieval;

import com.wokrag.agent.client.MilvusClientWrapper;
import com.wokrag.agent.client.SiliconFlowClient;
import com.wokrag.agent.model.SearchResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;

class RetrievalServiceTest {

    @Mock
    private MilvusClientWrapper milvusClient;

    @Mock
    private SiliconFlowClient siliconFlowClient;

    private MilvusService milvusService;
    private RerankerService rerankerService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        milvusService = new MilvusServiceImpl(milvusClient);
        rerankerService = new RerankerService(siliconFlowClient);
    }

    @Test
    void testMilvusServiceCreation() {
        assertNotNull(milvusService);
    }

    @Test
    void testRerankerServiceCreation() {
        assertNotNull(rerankerService);
    }
}
