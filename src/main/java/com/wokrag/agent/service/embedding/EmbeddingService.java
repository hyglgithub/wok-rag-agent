package com.wokrag.agent.service.embedding;

import java.util.List;

public interface EmbeddingService {
    double[] embed(String text);
    List<double[]> embedBatch(List<String> texts);
}
