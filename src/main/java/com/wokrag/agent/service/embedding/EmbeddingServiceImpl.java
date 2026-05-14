package com.wokrag.agent.service.embedding;

import com.wokrag.agent.client.SiliconFlowClient;
import com.wokrag.agent.config.SiliconFlowConfig;
import com.wokrag.agent.exception.RagException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmbeddingServiceImpl implements EmbeddingService {

    private final SiliconFlowClient client;
    private final SiliconFlowConfig config;

    @Override
    public double[] embed(String text) {
        if (text == null || text.isEmpty()) {
            throw new RagException.EmbeddingException("Text cannot be empty");
        }

        try {
            List<double[]> results = client.embed(List.of(text));
            return results.get(0);
        } catch (Exception e) {
            log.error("Failed to embed text", e);
            throw new RagException.EmbeddingException("Embedding failed", e);
        }
    }

    @Override
    public List<double[]> embedBatch(List<String> texts) {
        if (texts == null || texts.isEmpty()) {
            throw new RagException.EmbeddingException("Texts cannot be empty");
        }

        try {
            return client.embed(texts);
        } catch (Exception e) {
            log.error("Failed to embed batch", e);
            throw new RagException.EmbeddingException("Batch embedding failed", e);
        }
    }
}
