package com.wokrag.agent.service.intent;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import com.wokrag.agent.client.SiliconFlowClient;
import com.wokrag.agent.config.IntentConfig;
import com.wokrag.agent.model.ChatMessage;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class IntentClassifier {

    private final SiliconFlowClient client;
    private final IntentConfig config;
    private final Gson gson = new Gson();

    public static final String INTENT_KNOWLEDGE = "knowledge";
    public static final String INTENT_TOOL = "tool";
    public static final String INTENT_CHITCHAT = "chitchat";
    public static final String INTENT_CLARIFICATION = "clarification";

    private static final Set<String> VALID_INTENTS = Set.of(
            INTENT_KNOWLEDGE, INTENT_TOOL, INTENT_CHITCHAT, INTENT_CLARIFICATION);

    private static final Set<String> CHITCHAT_KEYWORDS = Set.of(
            "你好", "您好", "谢谢", "感谢", "再见", "拜拜",
            "哈哈", "嗯嗯", "好的", "收到", "明白了", "ok");

    private static final String CLASSIFY_PROMPT = """
            你是一个意图分类助手。根据对话历史和用户的最新消息，判断用户的意图类别。

            意图类别定义：
            1. knowledge - 知识检索：用户在询问产品信息、政策规定、操作指南等通用知识。
               示例："退货政策是什么""保修期多久""配送范围覆盖哪些城市"
            2. tool - 工具调用：用户想查询个人数据、实时信息，或执行某个操作。
               示例："查一下我的订单状态""帮我申请退货""我还剩几天年假"
            3. chitchat - 闲聊对话：用户在打招呼、感谢、闲聊，不涉及具体业务问题。
               示例："你好""谢谢""你是AI吗""今天心情不好"
            4. clarification - 引导澄清：用户的问题太模糊，缺少关键信息，无法确定意图。
               示例："有什么推荐的""怎么办""帮我看看"

            判断规则：
            - 结合对话历史判断，相同的话在不同上下文中意图可能不同
            - 如果用户的问题涉及"我的""查一下"等个人化表述，通常是工具调用
            - 如果问题在问通用的规则、政策、产品信息，通常是知识检索
            - 在无法判断意图时分类为 clarification
            - 以 JSON 格式输出：
              - 如果意图是 chitchat 或 clarification，同时生成回复内容：{"intent": "分类结果", "confidence": 置信度, "reply": "回复内容"}
              - 如果意图是 knowledge 或 tool：{"intent": "分类结果", "confidence": 置信度}
            - 不要输出 JSON 以外的任何内容

            对话历史：
            %s

            用户最新消息：%s
            """;

    public IntentResult classify(List<ChatMessage> history, String query) {
        if (!config.isEnabled()) {
            return new IntentResult(INTENT_KNOWLEDGE, 1.0);
        }

        if (query != null && query.length() <= 6 && CHITCHAT_KEYWORDS.contains(query.trim())) {
            log.debug("Intent classified by rules: chitchat (query='{}')", query);
            return new IntentResult(INTENT_CHITCHAT, 0.99);
        }

        try {
            IntentResult result = classifyByLLM(history, query);

            if (result.getConfidence() < config.getConfidenceThreshold()) {
                log.warn("Low confidence intent: {}, falling back to knowledge",
                        result.getConfidence());
                return new IntentResult(INTENT_KNOWLEDGE, 0.5);
            }

            log.info("Intent classified: {} (confidence={})", result.getIntent(), result.getConfidence());
            return result;
        } catch (Exception e) {
            log.warn("Intent classification failed, defaulting to knowledge: {}", e.getMessage());
            return new IntentResult(INTENT_KNOWLEDGE, 0.5);
        }
    }

    private IntentResult classifyByLLM(List<ChatMessage> history, String query) {
        StringBuilder historyText = new StringBuilder();
        if (history == null || history.isEmpty()) {
            historyText.append("（无历史对话）");
        } else {
            for (ChatMessage msg : history) {
                String roleName = "user".equals(msg.getRole()) ? "用户" : "助手";
                historyText.append(roleName).append("：")
                        .append(msg.getContent()).append("\n");
            }
        }

        String prompt = String.format(CLASSIFY_PROMPT, historyText.toString(), query);
        String response = client.chat(null, prompt, config.getModel());

        return parseIntentResult(response);
    }

    private IntentResult parseIntentResult(String content) {
        try {
            JsonObject result = gson.fromJson(content.trim(), JsonObject.class);
            String intent = result.get("intent").getAsString();
            double confidence = result.has("confidence")
                    ? result.get("confidence").getAsDouble() : 0.8;
            String reply = result.has("reply") && !result.get("reply").isJsonNull()
                    ? result.get("reply").getAsString() : null;

            if (!VALID_INTENTS.contains(intent)) {
                log.warn("Invalid intent from LLM: '{}', falling back to knowledge", intent);
                return new IntentResult(INTENT_KNOWLEDGE, 0.5);
            }

            return new IntentResult(intent, confidence, reply);
        } catch (JsonSyntaxException | NullPointerException | IllegalStateException e) {
            log.warn("Failed to parse intent JSON: '{}', falling back to knowledge", content);
            return new IntentResult(INTENT_KNOWLEDGE, 0.5);
        }
    }

    @Data
    @AllArgsConstructor
    public static class IntentResult {
        private String intent;
        private double confidence;
        private String reply;

        public IntentResult(String intent, double confidence) {
            this(intent, confidence, null);
        }
    }
}
