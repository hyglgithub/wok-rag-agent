package com.wokrag.agent.service.tool;

import com.google.gson.JsonArray;
import com.wokrag.agent.model.ToolDefinition;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@Component
public class ToolRegistry {

    private final Map<String, ToolHandler> handlers = new LinkedHashMap<>();

    public void register(ToolHandler handler) {
        handlers.put(handler.getDefinition().getName(), handler);
        log.info("Registered tool: {}", handler.getDefinition().getName());
    }

    public ToolHandler getHandler(String name) {
        return handlers.get(name);
    }

    public Collection<ToolHandler> getAllHandlers() {
        return handlers.values();
    }

    public JsonArray getToolDefinitions() {
        JsonArray tools = new JsonArray();
        for (ToolHandler handler : handlers.values()) {
            tools.add(handler.getDefinition().toJson());
        }
        return tools;
    }
}
