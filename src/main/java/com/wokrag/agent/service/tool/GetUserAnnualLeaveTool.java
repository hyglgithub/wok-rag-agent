package com.wokrag.agent.service.tool;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.wokrag.agent.model.ToolDefinition;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;

@Slf4j
@Component
@RequiredArgsConstructor
public class GetUserAnnualLeaveTool implements ToolHandler {

    private final ToolRegistry toolRegistry;

    @PostConstruct
    public void init() {
        toolRegistry.register(this);
    }

    @Override
    public ToolDefinition getDefinition() {
        JsonObject parameters = new JsonObject();
        parameters.addProperty("type", "object");
        JsonObject properties = new JsonObject();

        JsonObject employeeId = new JsonObject();
        employeeId.addProperty("type", "string");
        employeeId.addProperty("description", "员工工号");
        properties.add("employeeId", employeeId);

        parameters.add("properties", properties);

        JsonArray required = new JsonArray();
        required.add("employeeId");
        parameters.add("required", required);

        return new ToolDefinition(
                "getUserAnnualLeave",
                "查询员工的年假信息，包括年假总额、已使用天数和剩余天数",
                parameters
        );
    }

    @Override
    public String execute(JsonObject arguments) {
        String employeeId = arguments.get("employeeId").getAsString();
        log.info("Executing getUserAnnualLeave for employee: {}", employeeId);

        JsonObject result = new JsonObject();
        result.addProperty("employeeId", employeeId);
        result.addProperty("totalAnnualLeave", 15);
        result.addProperty("usedDays", 8);
        result.addProperty("remainingDays", 7);
        result.addProperty("year", 2026);

        JsonArray records = new JsonArray();

        JsonObject record1 = new JsonObject();
        record1.addProperty("startDate", "2026-01-20");
        record1.addProperty("endDate", "2026-01-24");
        record1.addProperty("days", 5);
        record1.addProperty("reason", "春节假期");
        records.add(record1);

        JsonObject record2 = new JsonObject();
        record2.addProperty("startDate", "2026-04-01");
        record2.addProperty("endDate", "2026-04-03");
        record2.addProperty("days", 3);
        record2.addProperty("reason", "个人事务");
        records.add(record2);

        result.add("records", records);

        return result.toString();
    }
}
