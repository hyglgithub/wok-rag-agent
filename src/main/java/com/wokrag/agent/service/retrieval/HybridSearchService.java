package com.wokrag.agent.service.retrieval;

import com.wokrag.agent.config.RagConfig;
import com.wokrag.agent.model.SearchResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class HybridSearchService {

    private final MilvusService milvusService;
    private final RerankerService rerankerService;
    private final RagConfig ragConfig;

    public List<SearchResult> hybridSearch(double[] queryVector, String queryText) {
        List<SearchResult> vectorResults = milvusService.search(
                queryVector, ragConfig.getDenseRecallTopK());

        List<SearchResult> rerankedResults = rerankerService.rerank(
                queryText, vectorResults, ragConfig.getTopK());

        return rerankedResults;
    }

    public List<SearchResult> rrfFusion(List<List<SearchResult>> resultLists, int k) {
        Map<String, Double> scoreMap = new HashMap<>();
        Map<String, SearchResult> resultMap = new HashMap<>();

        for (List<SearchResult> results : resultLists) {
            for (int rank = 0; rank < results.size(); rank++) {
                SearchResult result = results.get(rank);
                String key = result.getChunkId();

                double rrfScore = 1.0 / (k + rank + 1);
                scoreMap.merge(key, rrfScore, Double::sum);

                resultMap.putIfAbsent(key, result);
            }
        }

        return scoreMap.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .limit(ragConfig.getTopK())
                .map(entry -> {
                    SearchResult result = resultMap.get(entry.getKey());
                    result.setScore(entry.getValue());
                    return result;
                })
                .collect(Collectors.toList());
    }
}
