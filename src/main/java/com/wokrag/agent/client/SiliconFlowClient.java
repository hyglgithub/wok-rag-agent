package com.wokrag.agent.client;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.wokrag.agent.config.SiliconFlowConfig;
import com.wokrag.agent.exception.RagException;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class SiliconFlowClient {

    private final SiliconFlowConfig config;
    private final OkHttpClient httpClient;
    private final Gson gson;

    public SiliconFlowClient(SiliconFlowConfig config) {
        this.config = config;
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();
        this.gson = new Gson();
    }

    public List<double[]> embed(List<String> texts) {
        JsonObject requestBody = new JsonObject();
        requestBody.addProperty("model", config.getEmbeddingModel());
        requestBody.add("input", gson.toJsonTree(texts));
        requestBody.addProperty("encoding_format", "float");

        Request request = new Request.Builder()
                .url(config.getBaseUrl() + "/embeddings")
                .addHeader("Authorization", "Bearer " + config.getApiKey())
                .addHeader("Content-Type", "application/json")
                .post(RequestBody.create(
                        gson.toJson(requestBody),
                        MediaType.parse("application/json")))
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                String errorBody = response.body() != null ? response.body().string() : "no body";
                throw new RagException.EmbeddingException(
                        "Embedding API failed: HTTP " + response.code() + " - " + errorBody);
            }

            String responseBody = response.body().string();
            JsonObject json = gson.fromJson(responseBody, JsonObject.class);
            JsonArray dataArray = json.getAsJsonArray("data");

            List<double[]> embeddings = new ArrayList<>();
            for (int i = 0; i < dataArray.size(); i++) {
                JsonArray embeddingArray = dataArray.get(i)
                        .getAsJsonObject()
                        .getAsJsonArray("embedding");
                double[] vector = new double[embeddingArray.size()];
                for (int j = 0; j < embeddingArray.size(); j++) {
                    vector[j] = embeddingArray.get(j).getAsDouble();
                }
                embeddings.add(vector);
            }

            return embeddings;
        } catch (IOException e) {
            throw new RagException.EmbeddingException("Embedding API call failed", e);
        }
    }

    public String chat(String systemPrompt, String userMessage) {
        JsonObject requestBody = new JsonObject();
        requestBody.addProperty("model", config.getChatModel());
        requestBody.addProperty("temperature", 0.1);
        requestBody.addProperty("max_tokens", 1024);
        requestBody.addProperty("stream", false);

        JsonArray messages = new JsonArray();

        if (systemPrompt != null && !systemPrompt.isEmpty()) {
            JsonObject systemMsg = new JsonObject();
            systemMsg.addProperty("role", "system");
            systemMsg.addProperty("content", systemPrompt);
            messages.add(systemMsg);
        }

        JsonObject userMsg = new JsonObject();
        userMsg.addProperty("role", "user");
        userMsg.addProperty("content", userMessage);
        messages.add(userMsg);

        requestBody.add("messages", messages);

        Request request = new Request.Builder()
                .url(config.getBaseUrl() + "/chat/completions")
                .addHeader("Authorization", "Bearer " + config.getApiKey())
                .addHeader("Content-Type", "application/json")
                .post(RequestBody.create(
                        gson.toJson(requestBody),
                        MediaType.parse("application/json")))
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                String errorBody = response.body() != null ? response.body().string() : "no body";
                throw new RagException.GenerationException(
                        "Chat API failed: HTTP " + response.code() + " - " + errorBody);
            }

            String responseBody = response.body().string();
            JsonObject json = gson.fromJson(responseBody, JsonObject.class);
            return json.getAsJsonArray("choices")
                    .get(0).getAsJsonObject()
                    .getAsJsonObject("message")
                    .get("content").getAsString();
        } catch (IOException e) {
            throw new RagException.GenerationException("Chat API call failed", e);
        }
    }

    public List<RerankResult> rerank(String query, List<String> documents, int topN) {
        JsonObject requestBody = new JsonObject();
        requestBody.addProperty("model", config.getRerankerModel());
        requestBody.addProperty("query", query);
        requestBody.add("documents", gson.toJsonTree(documents));
        requestBody.addProperty("top_n", topN);
        requestBody.addProperty("return_documents", true);

        Request request = new Request.Builder()
                .url(config.getBaseUrl() + "/rerank")
                .addHeader("Authorization", "Bearer " + config.getApiKey())
                .addHeader("Content-Type", "application/json")
                .post(RequestBody.create(
                        gson.toJson(requestBody),
                        MediaType.parse("application/json")))
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                String errorBody = response.body() != null ? response.body().string() : "no body";
                throw new RagException.RetrievalException(
                        "Rerank API failed: HTTP " + response.code() + " - " + errorBody);
            }

            String responseBody = response.body().string();
            JsonObject json = gson.fromJson(responseBody, JsonObject.class);
            JsonArray results = json.getAsJsonArray("results");

            List<RerankResult> rerankResults = new ArrayList<>();
            for (int i = 0; i < results.size(); i++) {
                JsonObject item = results.get(i).getAsJsonObject();
                RerankResult result = new RerankResult();
                result.setIndex(item.get("index").getAsInt());
                result.setScore(item.get("relevance_score").getAsDouble());

                if (item.has("document") && item.get("document").isJsonObject()) {
                    JsonObject doc = item.getAsJsonObject("document");
                    result.setText(doc.has("text") ? doc.get("text").getAsString() : "");
                }

                rerankResults.add(result);
            }

            return rerankResults;
        } catch (IOException e) {
            throw new RagException.RetrievalException("Rerank API call failed", e);
        }
    }

    @lombok.Data
    public static class RerankResult {
        private int index;
        private double score;
        private String text;
    }
}
