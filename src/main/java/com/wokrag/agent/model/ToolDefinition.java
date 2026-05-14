package com.wokrag.agent.model;

import com.google.gson.JsonObject;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ToolDefinition {
    private String name;
    private String description;
    private JsonObject parameters;

    public JsonObject toJson() {
        JsonObject tool = new JsonObject();
        tool.addProperty("type", "function");
        JsonObject function = new JsonObject();
        function.addProperty("name", name);
        function.addProperty("description", description);
        function.add("parameters", parameters);
        tool.add("function", function);
        return tool;
    }
}
