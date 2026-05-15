package com.wokrag.agent.service.tool;

import com.google.gson.*;
import com.wokrag.agent.client.SiliconFlowClient;
import com.wokrag.agent.config.ToolConfig;
import com.wokrag.agent.exception.RagException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class FunctionCallService {

    private final SiliconFlowClient client;
    private final ToolRegistry toolRegistry;
    private final ToolConfig config;

    /**
     * Chat with tool calling support. Returns the final answer after potential tool invocations.
     */
    public String chatWithTools(String systemPrompt, String userMessage) {
        if (!config.isEnabled() || toolRegistry.getAllHandlers().isEmpty()) {
            return client.chat(systemPrompt, userMessage);
        }

        // Round 1: send with tools
        JsonObject firstResponse = client.chatWithTools(
                systemPrompt, userMessage,
                toolRegistry.getToolDefinitions(),
                config.getModel());

        // Check for tool_calls
        JsonArray toolCalls = extractToolCalls(firstResponse);

        if (toolCalls == null || toolCalls.isEmpty()) {
            return extractContent(firstResponse);
        }

        // Build messages for round 2
        List<JsonObject> messages = new ArrayList<>();

        JsonObject userMsg = new JsonObject();
        userMsg.addProperty("role", "user");
        userMsg.addProperty("content", userMessage);
        messages.add(userMsg);

        JsonObject assistantMsg = new JsonObject();
        assistantMsg.addProperty("role", "assistant");
        assistantMsg.add("content", JsonNull.INSTANCE);
        assistantMsg.add("tool_calls", toolCalls);
        messages.add(assistantMsg);

        // Execute each tool
        for (int i = 0; i < toolCalls.size(); i++) {
            JsonObject toolCall = toolCalls.get(i).getAsJsonObject();
            String functionName = toolCall.getAsJsonObject("function")
                    .get("name").getAsString();
            String arguments = toolCall.getAsJsonObject("function")
                    .get("arguments").getAsString();
            String toolCallId = toolCall.get("id").getAsString();

            String result = executeTool(functionName, arguments);

            JsonObject toolMsg = new JsonObject();
            toolMsg.addProperty("role", "tool");
            toolMsg.addProperty("tool_call_id", toolCallId);
            toolMsg.addProperty("content", result);
            messages.add(toolMsg);

            log.info("Executed tool: {} (id={})", functionName, toolCallId);
        }

        // Round 2: send with tool results
        try {
            return client.chatWithMessages(systemPrompt, messages, config.getModel());
        } catch (Exception e) {
            log.error("Round 2 chat failed", e);
            throw new RagException.GenerationException("Tool calling failed", e);
        }
    }

    public void chatWithToolsStreaming(String systemPrompt, String userMessage,
                                       SiliconFlowClient.StreamCallback callback) {
        if (!config.isEnabled() || toolRegistry.getAllHandlers().isEmpty()) {
            client.streamChat(systemPrompt, userMessage, callback);
            return;
        }

        // Round 1: synchronous tool call
        JsonObject firstResponse = client.chatWithTools(
                systemPrompt, userMessage,
                toolRegistry.getToolDefinitions(),
                config.getModel());

        JsonArray toolCalls = extractToolCalls(firstResponse);

        if (toolCalls == null || toolCalls.isEmpty()) {
            String content = extractContent(firstResponse);
            callback.onComplete(content, 0, 0);
            return;
        }

        // Build messages for round 2
        List<JsonObject> messages = new ArrayList<>();

        JsonObject userMsg = new JsonObject();
        userMsg.addProperty("role", "user");
        userMsg.addProperty("content", userMessage);
        messages.add(userMsg);

        JsonObject assistantMsg = new JsonObject();
        assistantMsg.addProperty("role", "assistant");
        assistantMsg.add("content", JsonNull.INSTANCE);
        assistantMsg.add("tool_calls", toolCalls);
        messages.add(assistantMsg);

        for (int i = 0; i < toolCalls.size(); i++) {
            JsonObject toolCall = toolCalls.get(i).getAsJsonObject();
            String functionName = toolCall.getAsJsonObject("function")
                    .get("name").getAsString();
            String arguments = toolCall.getAsJsonObject("function")
                    .get("arguments").getAsString();
            String toolCallId = toolCall.get("id").getAsString();

            String result = executeTool(functionName, arguments);

            JsonObject toolMsg = new JsonObject();
            toolMsg.addProperty("role", "tool");
            toolMsg.addProperty("tool_call_id", toolCallId);
            toolMsg.addProperty("content", result);
            messages.add(toolMsg);

            log.info("Executed tool: {} (id={})", functionName, toolCallId);
        }

        // Round 2: stream the final answer
        client.streamChatWithMessages(systemPrompt, messages, config.getModel(), callback);
    }

    private String executeTool(String functionName, String arguments) {
        ToolHandler handler = toolRegistry.getHandler(functionName);
        if (handler == null) {
            return "{\"error\": \"未知的工具: " + functionName + "\"}";
        }
        try {
            JsonObject args = JsonParser.parseString(arguments).getAsJsonObject();
            return handler.execute(args);
        } catch (Exception e) {
            log.error("Tool execution failed: {}", functionName, e);
            return "{\"error\": \"工具执行失败: " + e.getMessage() + "\"}";
        }
    }

    private JsonArray extractToolCalls(JsonObject response) {
        try {
            JsonObject message = response.getAsJsonArray("choices")
                    .get(0).getAsJsonObject()
                    .getAsJsonObject("message");
            if (message.has("tool_calls") && !message.get("tool_calls").isJsonNull()) {
                return message.getAsJsonArray("tool_calls");
            }
        } catch (Exception e) {
            log.debug("No tool_calls in response");
        }
        return null;
    }

    private String extractContent(JsonObject response) {
        try {
            return response.getAsJsonArray("choices")
                    .get(0).getAsJsonObject()
                    .getAsJsonObject("message")
                    .get("content").getAsString();
        } catch (Exception e) {
            return "";
        }
    }
}
