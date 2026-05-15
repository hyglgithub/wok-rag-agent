package com.wokrag.agent.client;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.wokrag.agent.config.SiliconFlowConfig;
import com.wokrag.agent.exception.RagException;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
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

    private static final int MAX_RETRIES = 3;
    private static final long BASE_DELAY_MS = 1000;

    private <T> T executeWithRetry(java.util.function.Supplier<T> operation, String operationName) {
        Exception lastException = null;
        for (int attempt = 0; attempt <= MAX_RETRIES; attempt++) {
            try {
                return operation.get();
            } catch (RagException e) {
                lastException = e;
                if (attempt < MAX_RETRIES) {
                    long delay = BASE_DELAY_MS * (long) Math.pow(2, attempt);
                    log.warn("{} failed (attempt {}/{}), retrying in {}ms: {}",
                            operationName, attempt + 1, MAX_RETRIES + 1, delay, e.getMessage());
                    try {
                        Thread.sleep(delay);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw e;
                    }
                }
            }
        }
        throw (RagException) lastException;
    }

    public List<double[]> embed(List<String> texts) {
        return executeWithRetry(() -> {
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
        }, "embed");
    }

    public String chat(String systemPrompt, String userMessage) {
        return executeWithRetry(() -> {
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
        }, "chat");
    }

    public String chat(String systemPrompt, String userMessage, String model) {
        JsonObject requestBody = new JsonObject();
        requestBody.addProperty("model", model != null ? model : config.getChatModel());
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

    public String chatWithMessages(String systemPrompt, List<JsonObject> messages, String model) {
        JsonObject requestBody = new JsonObject();
        requestBody.addProperty("model", model != null ? model : config.getChatModel());
        requestBody.addProperty("temperature", 0.1);
        requestBody.addProperty("max_tokens", 1024);

        JsonArray messagesArray = new JsonArray();
        if (systemPrompt != null && !systemPrompt.isEmpty()) {
            JsonObject sysMsg = new JsonObject();
            sysMsg.addProperty("role", "system");
            sysMsg.addProperty("content", systemPrompt);
            messagesArray.add(sysMsg);
        }
        for (JsonObject msg : messages) {
            messagesArray.add(msg);
        }
        requestBody.add("messages", messagesArray);

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

    public JsonObject chatWithTools(String systemPrompt, String userMessage,
                                     JsonArray tools, String model) {
        JsonObject requestBody = new JsonObject();
        requestBody.addProperty("model", model != null ? model : config.getChatModel());
        requestBody.addProperty("temperature", 0.1);
        requestBody.addProperty("max_tokens", 2048);

        JsonArray messages = new JsonArray();
        if (systemPrompt != null && !systemPrompt.isEmpty()) {
            JsonObject sysMsg = new JsonObject();
            sysMsg.addProperty("role", "system");
            sysMsg.addProperty("content", systemPrompt);
            messages.add(sysMsg);
        }
        JsonObject userMsg = new JsonObject();
        userMsg.addProperty("role", "user");
        userMsg.addProperty("content", userMessage);
        messages.add(userMsg);

        requestBody.add("messages", messages);
        requestBody.add("tools", tools);
        requestBody.addProperty("tool_choice", "auto");

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
            return gson.fromJson(responseBody, JsonObject.class);
        } catch (IOException e) {
            throw new RagException.GenerationException("Chat API call failed", e);
        }
    }

    public interface StreamCallback {
        void onToken(String token);
        void onComplete(String fullContent, int promptTokens, int completionTokens);
        void onError(Exception e, String partialContent);
    }

    public void streamChat(String systemPrompt, String userMessage, StreamCallback callback) {
        JsonObject requestBody = new JsonObject();
        requestBody.addProperty("model", config.getChatModel());
        requestBody.addProperty("temperature", 0.7);
        requestBody.addProperty("max_tokens", 2048);
        requestBody.addProperty("stream", true);

        JsonArray messages = new JsonArray();
        if (systemPrompt != null && !systemPrompt.isEmpty()) {
            JsonObject sysMsg = new JsonObject();
            sysMsg.addProperty("role", "system");
            sysMsg.addProperty("content", systemPrompt);
            messages.add(sysMsg);
        }
        JsonObject userMsg = new JsonObject();
        userMsg.addProperty("role", "user");
        userMsg.addProperty("content", userMessage);
        messages.add(userMsg);
        requestBody.add("messages", messages);

        OkHttpClient streamClient = httpClient.newBuilder()
                .readTimeout(60, TimeUnit.SECONDS)
                .build();

        Request request = new Request.Builder()
                .url(config.getBaseUrl() + "/chat/completions")
                .addHeader("Authorization", "Bearer " + config.getApiKey())
                .addHeader("Content-Type", "application/json")
                .addHeader("Accept", "text/event-stream")
                .post(RequestBody.create(
                        gson.toJson(requestBody),
                        MediaType.parse("application/json")))
                .build();

        StringBuilder fullContent = new StringBuilder();
        int promptTokens = 0;
        int completionTokens = 0;

        try (Response response = streamClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                String errorBody = response.body() != null ? response.body().string() : "no body";
                callback.onError(new RuntimeException("HTTP " + response.code() + ": " + errorBody), "");
                return;
            }

            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(response.body().byteStream(), StandardCharsets.UTF_8));
            String line;
            boolean streamDone = false;

            while ((line = reader.readLine()) != null) {
                if (line.isEmpty() || line.startsWith(":")) continue;
                if (!line.startsWith("data:")) continue;

                String data = line.substring(5);
                if (data.startsWith(" ")) data = data.substring(1);
                if ("[DONE]".equals(data)) {
                    streamDone = true;
                    break;
                }

                try {
                    JsonObject chunk = gson.fromJson(data, JsonObject.class);
                    JsonArray choices = chunk.getAsJsonArray("choices");
                    if (choices != null && !choices.isEmpty()) {
                        JsonObject choice = choices.get(0).getAsJsonObject();
                        JsonObject delta = choice.getAsJsonObject("delta");
                        if (delta != null && delta.has("content")) {
                            var contentElement = delta.get("content");
                            if (!contentElement.isJsonNull()) {
                                String token = contentElement.getAsString();
                                if (!token.isEmpty()) {
                                    fullContent.append(token);
                                    callback.onToken(token);
                                }
                            }
                        }
                    }

                    // Extract usage if present
                    if (chunk.has("usage") && !chunk.get("usage").isJsonNull()) {
                        JsonObject usage = chunk.getAsJsonObject("usage");
                        if (usage.has("prompt_tokens")) {
                            promptTokens = usage.get("prompt_tokens").getAsInt();
                        }
                        if (usage.has("completion_tokens")) {
                            completionTokens = usage.get("completion_tokens").getAsInt();
                        }
                    }
                } catch (Exception e) {
                    log.warn("Failed to parse SSE chunk: {}", data);
                }
            }

            if (streamDone) {
                callback.onComplete(fullContent.toString(), promptTokens, completionTokens);
            } else {
                callback.onError(new RuntimeException("SSE stream ended without [DONE]"),
                        fullContent.toString());
            }
        } catch (Exception e) {
            callback.onError(e, fullContent.toString());
        }
    }

    public void streamChatWithMessages(String systemPrompt, List<JsonObject> messages,
                                        String model, StreamCallback callback) {
        JsonObject requestBody = new JsonObject();
        requestBody.addProperty("model", model != null ? model : config.getChatModel());
        requestBody.addProperty("temperature", 0.7);
        requestBody.addProperty("max_tokens", 2048);
        requestBody.addProperty("stream", true);

        JsonArray messagesArray = new JsonArray();
        if (systemPrompt != null && !systemPrompt.isEmpty()) {
            JsonObject sysMsg = new JsonObject();
            sysMsg.addProperty("role", "system");
            sysMsg.addProperty("content", systemPrompt);
            messagesArray.add(sysMsg);
        }
        for (JsonObject msg : messages) {
            messagesArray.add(msg);
        }
        requestBody.add("messages", messagesArray);

        OkHttpClient streamClient = httpClient.newBuilder()
                .readTimeout(60, TimeUnit.SECONDS)
                .build();

        Request request = new Request.Builder()
                .url(config.getBaseUrl() + "/chat/completions")
                .addHeader("Authorization", "Bearer " + config.getApiKey())
                .addHeader("Content-Type", "application/json")
                .addHeader("Accept", "text/event-stream")
                .post(RequestBody.create(
                        gson.toJson(requestBody),
                        MediaType.parse("application/json")))
                .build();

        StringBuilder fullContent = new StringBuilder();
        int promptTokens = 0;
        int completionTokens = 0;

        try (Response response = streamClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                String errorBody = response.body() != null ? response.body().string() : "no body";
                callback.onError(new RuntimeException("HTTP " + response.code() + ": " + errorBody), "");
                return;
            }

            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(response.body().byteStream(), StandardCharsets.UTF_8));
            String line;
            boolean streamDone = false;

            while ((line = reader.readLine()) != null) {
                if (line.isEmpty() || line.startsWith(":")) continue;
                if (!line.startsWith("data:")) continue;

                String data = line.substring(5);
                if (data.startsWith(" ")) data = data.substring(1);
                if ("[DONE]".equals(data)) {
                    streamDone = true;
                    break;
                }

                try {
                    JsonObject chunk = gson.fromJson(data, JsonObject.class);
                    JsonArray choices = chunk.getAsJsonArray("choices");
                    if (choices != null && !choices.isEmpty()) {
                        JsonObject choice = choices.get(0).getAsJsonObject();
                        JsonObject delta = choice.getAsJsonObject("delta");
                        if (delta != null && delta.has("content")) {
                            var contentElement = delta.get("content");
                            if (!contentElement.isJsonNull()) {
                                String token = contentElement.getAsString();
                                if (!token.isEmpty()) {
                                    fullContent.append(token);
                                    callback.onToken(token);
                                }
                            }
                        }
                    }

                    if (chunk.has("usage") && !chunk.get("usage").isJsonNull()) {
                        JsonObject usage = chunk.getAsJsonObject("usage");
                        if (usage.has("prompt_tokens")) {
                            promptTokens = usage.get("prompt_tokens").getAsInt();
                        }
                        if (usage.has("completion_tokens")) {
                            completionTokens = usage.get("completion_tokens").getAsInt();
                        }
                    }
                } catch (Exception e) {
                    log.warn("Failed to parse SSE chunk: {}", data);
                }
            }

            if (streamDone) {
                callback.onComplete(fullContent.toString(), promptTokens, completionTokens);
            } else {
                callback.onError(new RuntimeException("SSE stream ended without [DONE]"),
                        fullContent.toString());
            }
        } catch (Exception e) {
            callback.onError(e, fullContent.toString());
        }
    }

    public List<RerankResult> rerank(String query, List<String> documents, int topN) {
        return executeWithRetry(() -> {
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
        }, "rerank");
    }

    @lombok.Data
    public static class RerankResult {
        private int index;
        private double score;
        private String text;
    }
}
