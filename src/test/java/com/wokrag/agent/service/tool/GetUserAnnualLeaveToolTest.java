package com.wokrag.agent.service.tool;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.wokrag.agent.model.ToolDefinition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.*;

class GetUserAnnualLeaveToolTest {

    @Mock
    private ToolRegistry toolRegistry;

    private GetUserAnnualLeaveTool tool;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        tool = new GetUserAnnualLeaveTool(toolRegistry);
    }

    @Test
    void testGetDefinition() {
        ToolDefinition def = tool.getDefinition();
        assertEquals("getUserAnnualLeave", def.getName());
        assertNotNull(def.getDescription());
        assertNotNull(def.getParameters());
    }

    @Test
    void testExecuteReturnsMockData() {
        JsonObject args = new JsonObject();
        args.addProperty("employeeId", "EMP-001");
        String result = tool.execute(args);

        JsonObject json = JsonParser.parseString(result).getAsJsonObject();
        assertEquals("EMP-001", json.get("employeeId").getAsString());
        assertEquals(15, json.get("totalAnnualLeave").getAsInt());
        assertEquals(8, json.get("usedDays").getAsInt());
        assertEquals(7, json.get("remainingDays").getAsInt());
        assertTrue(json.has("records"));
    }
}
