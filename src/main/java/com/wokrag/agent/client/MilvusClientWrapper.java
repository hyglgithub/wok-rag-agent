package com.wokrag.agent.client;

import com.wokrag.agent.config.MilvusConfig;
import com.wokrag.agent.exception.RagException;
import io.milvus.v2.client.ConnectConfig;
import io.milvus.v2.client.MilvusClientV2;
import io.milvus.v2.common.DataType;
import io.milvus.v2.common.IndexParam;
import io.milvus.v2.service.collection.request.AddFieldReq;
import io.milvus.v2.service.collection.request.CreateCollectionReq;
import io.milvus.v2.service.collection.request.HasCollectionReq;
import io.milvus.v2.service.collection.request.LoadCollectionReq;
import io.milvus.v2.service.index.request.CreateIndexReq;
import io.milvus.v2.service.vector.request.DeleteReq;
import io.milvus.v2.service.vector.request.InsertReq;
import io.milvus.v2.service.vector.request.QueryReq;
import io.milvus.v2.service.vector.request.SearchReq;
import io.milvus.v2.service.vector.request.UpsertReq;
import io.milvus.v2.service.vector.response.InsertResp;
import io.milvus.v2.service.vector.response.QueryResp;
import io.milvus.v2.service.vector.response.SearchResp;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import io.milvus.v2.service.vector.request.data.FloatVec;

import java.util.*;

@Slf4j
@Component
public class MilvusClientWrapper {

    private final MilvusConfig config;
    private MilvusClientV2 client;

    public MilvusClientWrapper(MilvusConfig config) {
        this.config = config;
    }

    @PostConstruct
    public void init() {
        try {
            ConnectConfig connectConfig = ConnectConfig.builder()
                    .uri(config.getUri())
                    .build();
            client = new MilvusClientV2(connectConfig);
            log.info("Connected to Milvus at {}", config.getUri());

            createCollectionIfNotExist();
        } catch (Exception e) {
            log.error("Failed to connect to Milvus", e);
            throw new RagException.RetrievalException("Failed to connect to Milvus", e);
        }
    }

    @PreDestroy
    public void close() {
        if (client != null) {
            client.close();
            log.info("Milvus connection closed");
        }
    }

    private void createCollectionIfNotExist() {
        try {
            Boolean exists = client.hasCollection(
                    HasCollectionReq.builder()
                            .collectionName(config.getCollectionName())
                            .build());

            if (!Boolean.TRUE.equals(exists)) {
                createCollection();
            } else {
                loadCollection();
            }
        } catch (Exception e) {
            log.error("Failed to check/create collection", e);
            throw new RagException.RetrievalException("Collection setup failed", e);
        }
    }

    private void createCollection() {
        CreateCollectionReq.CollectionSchema schema = client.createSchema();

        schema.addField(AddFieldReq.builder()
                .fieldName("id")
                .dataType(DataType.Int64)
                .isPrimaryKey(true)
                .autoID(true)
                .build());

        schema.addField(AddFieldReq.builder()
                .fieldName("chunk_text")
                .dataType(DataType.VarChar)
                .maxLength(8192)
                .enableAnalyzer(true)
                .build());

        schema.addField(AddFieldReq.builder()
                .fieldName("text_dense")
                .dataType(DataType.FloatVector)
                .dimension(config.getVectorDim())
                .build());

        schema.addField(AddFieldReq.builder()
                .fieldName("doc_id")
                .dataType(DataType.VarChar)
                .maxLength(64)
                .build());

        schema.addField(AddFieldReq.builder()
                .fieldName("source")
                .dataType(DataType.VarChar)
                .maxLength(256)
                .build());

        schema.addField(AddFieldReq.builder()
                .fieldName("source_url")
                .dataType(DataType.VarChar)
                .maxLength(512)
                .build());

        client.createCollection(CreateCollectionReq.builder()
                .collectionName(config.getCollectionName())
                .collectionSchema(schema)
                .build());

        IndexParam denseIndex = IndexParam.builder()
                .fieldName("text_dense")
                .indexType(IndexParam.IndexType.HNSW)
                .metricType(IndexParam.MetricType.COSINE)
                .extraParams(Map.of("M", 16, "efConstruction", 256))
                .build();

        client.createIndex(CreateIndexReq.builder()
                .collectionName(config.getCollectionName())
                .indexParams(List.of(denseIndex))
                .build());

        loadCollection();
        log.info("Collection {} created successfully", config.getCollectionName());
    }

    private void loadCollection() {
        client.loadCollection(LoadCollectionReq.builder()
                .collectionName(config.getCollectionName())
                .build());
        log.info("Collection {} loaded", config.getCollectionName());
    }

    private final Gson gson = new Gson();

    public long insert(List<Map<String, Object>> rows) {
        try {
            List<JsonObject> jsonRows = new ArrayList<>();
            for (Map<String, Object> row : rows) {
                jsonRows.add(gson.toJsonTree(row).getAsJsonObject());
            }

            InsertResp resp = client.insert(InsertReq.builder()
                    .collectionName(config.getCollectionName())
                    .data(jsonRows)
                    .build());
            return resp.getInsertCnt();
        } catch (Exception e) {
            throw new RagException.RetrievalException("Failed to insert data", e);
        }
    }

    public List<SearchResp.SearchResult> search(List<Float> queryVector, int topK) {
        try {
            SearchReq searchReq = SearchReq.builder()
                    .collectionName(config.getCollectionName())
                    .data(List.of(new FloatVec(queryVector)))
                    .topK(topK)
                    .outputFields(List.of("chunk_text", "doc_id", "source", "source_url"))
                    .annsField("text_dense")
                    .searchParams(Map.of("ef", 128))
                    .build();

            SearchResp resp = client.search(searchReq);
            return resp.getSearchResults().get(0);
        } catch (Exception e) {
            throw new RagException.RetrievalException("Search failed", e);
        }
    }

    public void deleteByDocId(String docId) {
        try {
            String expr = "doc_id == \"" + docId + "\"";
            client.delete(DeleteReq.builder()
                    .collectionName(config.getCollectionName())
                    .filter(expr)
                    .build());
            log.info("Deleted Milvus chunks for doc_id: {}", docId);
        } catch (Exception e) {
            throw new RagException.RetrievalException("Failed to delete chunks for doc_id: " + docId, e);
        }
    }

    public List<QueryResp.QueryResult> queryByDocId(String docId) {
        try {
            String expr = "doc_id == \"" + docId + "\"";
            QueryResp resp = client.query(QueryReq.builder()
                    .collectionName(config.getCollectionName())
                    .filter(expr)
                    .outputFields(List.of("id", "chunk_text", "doc_id", "source"))
                    .build());
            return resp.getQueryResults();
        } catch (Exception e) {
            throw new RagException.RetrievalException("Failed to query chunks for doc_id: " + docId, e);
        }
    }

    public void deleteByPrimaryKey(long id) {
        try {
            String expr = "id == " + id;
            client.delete(DeleteReq.builder()
                    .collectionName(config.getCollectionName())
                    .filter(expr)
                    .build());
            log.info("Deleted Milvus chunk id={}", id);
        } catch (Exception e) {
            throw new RagException.RetrievalException("Failed to delete chunk id=" + id, e);
        }
    }

    public Map<String, Object> getByPrimaryKey(long id) {
        try {
            String expr = "id == " + id;
            QueryResp resp = client.query(QueryReq.builder()
                    .collectionName(config.getCollectionName())
                    .filter(expr)
                    .outputFields(List.of("chunk_text", "text_dense", "doc_id", "source", "source_url"))
                    .build());
            
            List<QueryResp.QueryResult> results = resp.getQueryResults();
            if (results.isEmpty()) {
                throw new RagException.RetrievalException("Chunk not found with id=" + id);
            }
            
            return results.get(0).getEntity();
        } catch (Exception e) {
            throw new RagException.RetrievalException("Failed to query chunk id=" + id, e);
        }
    }

    public void updateByPrimaryKey(long id, String chunkText, float[] vector) {
        try {
            // 将 float[] 转换为 List<Float>
            List<Float> vectorList = new ArrayList<>(vector.length);
            for (float v : vector) {
                vectorList.add(v);
            }
            
            // 构建完整的新记录
            Map<String, Object> newRow = new HashMap<>();
            newRow.put("id", id);
            newRow.put("chunk_text", chunkText);
            newRow.put("text_dense", vectorList);
            
            // 直接 upsert (Milvus 内部会处理 delete+insert)
            JsonObject jsonRow = gson.toJsonTree(newRow).getAsJsonObject();
            client.upsert(UpsertReq.builder()
                    .collectionName(config.getCollectionName())
                    .data(List.of(jsonRow))
                    .build());
            log.info("Updated Milvus chunk id={}", id);
        } catch (Exception e) {
            throw new RagException.RetrievalException("Failed to update chunk id=" + id, e);
        }
    }

    public MilvusClientV2 getClient() {
        return client;
    }

    public boolean isHealthy() {
        try {
            Boolean exists = client.hasCollection(
                    HasCollectionReq.builder()
                            .collectionName(config.getCollectionName())
                            .build());
            return Boolean.TRUE.equals(exists);
        } catch (Exception e) {
            log.warn("Milvus health check failed: {}", e.getMessage());
            return false;
        }
    }
}
