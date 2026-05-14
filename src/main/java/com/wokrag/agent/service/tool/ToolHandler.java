package com.wokrag.agent.service.tool;

import com.google.gson.JsonObject;
import com.wokrag.agent.model.ToolDefinition;

public interface ToolHandler {
    ToolDefinition getDefinition();
    String execute(JsonObject arguments);
}
