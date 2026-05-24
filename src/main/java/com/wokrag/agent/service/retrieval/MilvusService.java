package com.wokrag.agent.service.retrieval;

import com.wokrag.agent.model.SearchResult;

import java.util.List;
import java.util.Map;

public interface MilvusService {
    long insertChunks(List<Map<String, Object>> rows);
    List<SearchResult> search(double[] queryVector, int topK);
    List<SearchResult> bm25Search(String queryText, int topK);
}
