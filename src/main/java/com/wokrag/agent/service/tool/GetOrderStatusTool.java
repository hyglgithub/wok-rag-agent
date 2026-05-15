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
public class GetOrderStatusTool implements ToolHandler {

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

        JsonObject orderId = new JsonObject();
        orderId.addProperty("type", "string");
        orderId.addProperty("description", "订单编号");
        properties.add("orderId", orderId);

        parameters.add("properties", properties);

        JsonArray required = new JsonArray();
        required.add("orderId");
        parameters.add("required", required);

        return new ToolDefinition(
                "getOrderStatus",
                "查询订单的物流状态和配送信息",
                parameters
        );
    }

    @Override
    public String execute(JsonObject arguments) {
        String orderId = arguments.get("orderId").getAsString();
        log.info("Executing getOrderStatus for order: {}", orderId);

        JsonObject result = new JsonObject();
        result.addProperty("orderId", orderId);
        result.addProperty("status", "已发货");
        result.addProperty("statusCode", "SHIPPED");

        JsonObject logistics = new JsonObject();
        logistics.addProperty("carrier", "顺丰速运");
        String tracking = orderId.replace("-", "");
        logistics.addProperty("trackingNumber", "SF" + tracking.substring(0, Math.min(12, tracking.length())));
        logistics.addProperty("estimatedDelivery", "2026-05-18");
        logistics.addProperty("currentLocation", "北京转运中心");
        result.add("logistics", logistics);

        JsonArray timeline = new JsonArray();

        JsonObject step1 = new JsonObject();
        step1.addProperty("time", "2026-05-12 10:00:00");
        step1.addProperty("status", "订单已创建");
        timeline.add(step1);

        JsonObject step2 = new JsonObject();
        step2.addProperty("time", "2026-05-12 14:30:00");
        step2.addProperty("status", "商家已发货");
        timeline.add(step2);

        JsonObject step3 = new JsonObject();
        step3.addProperty("time", "2026-05-13 08:00:00");
        step3.addProperty("status", "已到达北京转运中心");
        timeline.add(step3);

        JsonObject step4 = new JsonObject();
        step4.addProperty("time", "2026-05-14 16:00:00");
        step4.addProperty("status", "正在派送中");
        timeline.add(step4);

        result.add("timeline", timeline);

        return result.toString();
    }
}
