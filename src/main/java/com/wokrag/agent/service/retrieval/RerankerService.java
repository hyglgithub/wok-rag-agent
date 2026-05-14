package com.wokrag.agent.service.retrieval;

import com.wokrag.agent.client.SiliconFlowClient;
import com.wokrag.agent.model.SearchResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class RerankerService {

    private final SiliconFlowClient client;

    public List<SearchResult> rerank(String query, List<SearchResult> candidates, int topN) {
        if (candidates == null || candidates.isEmpty()) {
            return new ArrayList<>();
        }

        List<String> documents = candidates.stream()
                .map(SearchResult::getContent)
                .toList();

        List<SiliconFlowClient.RerankResult> rerankResults = client.rerank(query, documents, topN);

        List<SearchResult> results = new ArrayList<>();
        for (SiliconFlowClient.RerankResult rerankResult : rerankResults) {
            int originalIndex = rerankResult.getIndex();
            if (originalIndex >= 0 && originalIndex < candidates.size()) {
                SearchResult original = candidates.get(originalIndex);
                SearchResult result = new SearchResult();
                result.setChunkId(original.getChunkId());
                result.setContent(original.getContent());
                result.setScore(rerankResult.getScore());
                result.setMetadata(original.getMetadata());
                results.add(result);
            }
        }

        return results;
    }
}
