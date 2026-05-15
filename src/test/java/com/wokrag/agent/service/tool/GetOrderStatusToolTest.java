package com.wokrag.agent.service.tool;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.wokrag.agent.model.ToolDefinition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.*;

class GetOrderStatusToolTest {

    @Mock
    private ToolRegistry toolRegistry;

    private GetOrderStatusTool tool;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        tool = new GetOrderStatusTool(toolRegistry);
    }

    @Test
    void testGetDefinition() {
        ToolDefinition def = tool.getDefinition();
        assertEquals("getOrderStatus", def.getName());
        assertNotNull(def.getDescription());
        assertNotNull(def.getParameters());
    }

    @Test
    void testExecuteReturnsMockData() {
        JsonObject args = new JsonObject();
        args.addProperty("orderId", "ORD-20260512-001");
        String result = tool.execute(args);

        JsonObject json = JsonParser.parseString(result).getAsJsonObject();
        assertEquals("ORD-20260512-001", json.get("orderId").getAsString());
        assertEquals("已发货", json.get("status").getAsString());
        assertTrue(json.has("logistics"));
        assertTrue(json.has("timeline"));
    }
}
