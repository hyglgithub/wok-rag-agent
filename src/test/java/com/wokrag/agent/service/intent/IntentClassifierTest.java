package com.wokrag.agent.service.intent;

import com.wokrag.agent.client.SiliconFlowClient;
import com.wokrag.agent.config.IntentConfig;
import com.wokrag.agent.model.ChatMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

class IntentClassifierTest {

    @Mock
    private SiliconFlowClient client;
    @Mock
    private IntentConfig config;

    private IntentClassifier classifier;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(config.isEnabled()).thenReturn(true);
        when(config.getModel()).thenReturn("Qwen/Qwen2.5-7B-Instruct");
        when(config.getConfidenceThreshold()).thenReturn(0.5);
        classifier = new IntentClassifier(client, config);
    }

    @Test
    void testDisabledReturnsDefault() {
        when(config.isEnabled()).thenReturn(false);
        IntentClassifier.IntentResult result = classifier.classify(List.of(), "任何问题");
        assertEquals(IntentClassifier.INTENT_KNOWLEDGE, result.getIntent());
        assertEquals(1.0, result.getConfidence());
        assertNull(result.getReply());
    }

    @Test
    void testRuleBasedChitchatShortQuery() {
        IntentClassifier.IntentResult result = classifier.classify(List.of(), "你好");
        assertEquals(IntentClassifier.INTENT_CHITCHAT, result.getIntent());
        assertEquals(0.99, result.getConfidence());
        assertNull(result.getReply());
    }

    @Test
    void testRuleBasedChitchatKeywords() {
        IntentClassifier.IntentResult result = classifier.classify(List.of(), "谢谢");
        assertEquals(IntentClassifier.INTENT_CHITCHAT, result.getIntent());
        assertNull(result.getReply());
    }

    @Test
    void testLLMClassifyKnowledge() {
        when(client.chat(isNull(), anyString(), anyString()))
                .thenReturn("{\"intent\": \"knowledge\", \"confidence\": 0.95}");
        IntentClassifier.IntentResult result = classifier.classify(
                List.of(), "退货政策是什么");
        assertEquals(IntentClassifier.INTENT_KNOWLEDGE, result.getIntent());
        assertEquals(0.95, result.getConfidence());
        assertNull(result.getReply());
    }

    @Test
    void testLLMClassifyTool() {
        when(client.chat(isNull(), anyString(), anyString()))
                .thenReturn("{\"intent\": \"tool\", \"confidence\": 0.92}");
        IntentClassifier.IntentResult result = classifier.classify(
                List.of(), "查一下我的订单状态");
        assertEquals(IntentClassifier.INTENT_TOOL, result.getIntent());
        assertNull(result.getReply());
    }

    @Test
    void testLLMClassifyChitchatWithReply() {
        when(client.chat(isNull(), anyString(), anyString()))
                .thenReturn("{\"intent\": \"chitchat\", \"confidence\": 0.95, \"reply\": \"你好！有什么可以帮您的？\"}");
        IntentClassifier.IntentResult result = classifier.classify(List.of(), "今天心情不好");
        assertEquals(IntentClassifier.INTENT_CHITCHAT, result.getIntent());
        assertEquals(0.95, result.getConfidence());
        assertEquals("你好！有什么可以帮您的？", result.getReply());
    }

    @Test
    void testLLMClassifyClarificationWithReply() {
        when(client.chat(isNull(), anyString(), anyString()))
                .thenReturn("{\"intent\": \"clarification\", \"confidence\": 0.90, \"reply\": \"您想了解哪方面的信息呢？产品、订单还是售后？\"}");
        IntentClassifier.IntentResult result = classifier.classify(List.of(), "有什么推荐的");
        assertEquals(IntentClassifier.INTENT_CLARIFICATION, result.getIntent());
        assertEquals("您想了解哪方面的信息呢？产品、订单还是售后？", result.getReply());
    }

    @Test
    void testLLMClassifyWithHistory() {
        List<ChatMessage> history = List.of(
                new ChatMessage("user", "iPhone退货政策"),
                new ChatMessage("assistant", "支持七天无理由退货"));
        when(client.chat(isNull(), anyString(), anyString()))
                .thenReturn("{\"intent\": \"tool\", \"confidence\": 0.93}");
        IntentClassifier.IntentResult result = classifier.classify(history, "帮我退了吧");
        assertEquals(IntentClassifier.INTENT_TOOL, result.getIntent());
    }

    @Test
    void testLowConfidenceFallsBackToKnowledge() {
        when(client.chat(isNull(), anyString(), anyString()))
                .thenReturn("{\"intent\": \"tool\", \"confidence\": 0.3}");
        IntentClassifier.IntentResult result = classifier.classify(List.of(), "模糊的问题");
        assertEquals(IntentClassifier.INTENT_KNOWLEDGE, result.getIntent());
    }

    @Test
    void testInvalidIntentFallsBackToKnowledge() {
        when(client.chat(isNull(), anyString(), anyString()))
                .thenReturn("{\"intent\": \"complaint\", \"confidence\": 0.9}");
        IntentClassifier.IntentResult result = classifier.classify(List.of(), "投诉");
        assertEquals(IntentClassifier.INTENT_KNOWLEDGE, result.getIntent());
    }

    @Test
    void testMalformedJsonFallsBackToKnowledge() {
        when(client.chat(isNull(), anyString(), anyString()))
                .thenReturn("这不是JSON");
        IntentClassifier.IntentResult result = classifier.classify(List.of(), "问题");
        assertEquals(IntentClassifier.INTENT_KNOWLEDGE, result.getIntent());
    }

    @Test
    void testLLMExceptionFallsBackToKnowledge() {
        when(client.chat(isNull(), anyString(), anyString()))
                .thenThrow(new RuntimeException("API error"));
        IntentClassifier.IntentResult result = classifier.classify(List.of(), "问题");
        assertEquals(IntentClassifier.INTENT_KNOWLEDGE, result.getIntent());
    }
}
