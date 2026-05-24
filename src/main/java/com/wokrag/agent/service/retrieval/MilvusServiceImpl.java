package com.wokrag.agent.service.retrieval;

import com.wokrag.agent.client.MilvusClientWrapper;
import com.wokrag.agent.model.SearchResult;
import io.milvus.v2.service.vector.response.SearchResp;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class MilvusServiceImpl implements MilvusService {

    private final MilvusClientWrapper milvusClient;

    @Override
    public long insertChunks(List<Map<String, Object>> rows) {
        return milvusClient.insert(rows);
    }

    @Override
    public List<SearchResult> search(double[] queryVector, int topK) {
        List<Float> floatVector = new ArrayList<>();
        for (double d : queryVector) {
            floatVector.add((float) d);
        }

        List<SearchResp.SearchResult> milvusResults = milvusClient.search(floatVector, topK);

        return mapResults(milvusResults);
    }

    @Override
    public List<SearchResult> bm25Search(String queryText, int topK) {
        List<SearchResp.SearchResult> milvusResults = milvusClient.bm25Search(queryText, topK);

        return mapResults(milvusResults);
    }

    private List<SearchResult> mapResults(List<SearchResp.SearchResult> milvusResults) {
        List<SearchResult> results = new ArrayList<>();
        for (SearchResp.SearchResult milvusResult : milvusResults) {
            SearchResult result = new SearchResult();
            result.setChunkId(String.valueOf(milvusResult.getId()));
            result.setContent((String) milvusResult.getEntity().get("chunk_text"));
            result.setScore(milvusResult.getScore());

            Map<String, String> metadata = new HashMap<>();
            metadata.put("doc_id", (String) milvusResult.getEntity().get("doc_id"));
            metadata.put("source", (String) milvusResult.getEntity().get("source"));
            metadata.put("source_url", (String) milvusResult.getEntity().get("source_url"));
            result.setMetadata(metadata);

            results.add(result);
        }

        return results;
    }
}
