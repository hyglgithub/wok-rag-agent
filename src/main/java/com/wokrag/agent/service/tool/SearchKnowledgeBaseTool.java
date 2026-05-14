package com.wokrag.agent.service.tool;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.wokrag.agent.model.RagResponse;
import com.wokrag.agent.model.ToolDefinition;
import com.wokrag.agent.service.rag.RagPipeline;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;

@Slf4j
@Component
@RequiredArgsConstructor
public class SearchKnowledgeBaseTool implements ToolHandler {

    private final ToolRegistry toolRegistry;
    private final RagPipeline ragPipeline;

    @PostConstruct
    public void init() {
        toolRegistry.register(this);
    }

    @Override
    public ToolDefinition getDefinition() {
        JsonObject parameters = new JsonObject();
        parameters.addProperty("type", "object");
        JsonObject properties = new JsonObject();
        JsonObject query = new JsonObject();
        query.addProperty("type", "string");
        query.addProperty("description", "搜索关键词");
        properties.add("query", query);
        parameters.add("properties", properties);
        JsonArray required = new JsonArray();
        required.add("query");
        parameters.add("required", required);

        return new ToolDefinition(
                "searchKnowledgeBase",
                "在企业知识库中搜索相关文档，适用于查询公司制度、产品文档、操作指南等静态知识",
                parameters
        );
    }

    @Override
    public String execute(JsonObject arguments) {
        String query = arguments.get("query").getAsString();
        log.info("Executing knowledge base search: {}", query);

        try {
            RagResponse response = ragPipeline.executeWithoutTools(query);
            JsonObject result = new JsonObject();
            result.addProperty("answer", response.getAnswer());
            if (response.getCitations() != null) {
                JsonArray citations = new JsonArray();
                for (RagResponse.CitationInfo citation : response.getCitations()) {
                    JsonObject c = new JsonObject();
                    c.addProperty("source", citation.getSource());
                    c.addProperty("content", citation.getChunkContent());
                    citations.add(c);
                }
                result.add("citations", citations);
            }
            return result.toString();
        } catch (Exception e) {
            log.error("Knowledge base search failed", e);
            return "{\"error\": \"知识库检索失败: " + e.getMessage() + "\"}";
        }
    }
}
