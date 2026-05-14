package com.wokrag.agent.service.rewrite;

import com.wokrag.agent.client.SiliconFlowClient;
import com.wokrag.agent.config.RewriteConfig;
import com.wokrag.agent.model.ChatMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class QueryRewriter {

    private final SiliconFlowClient client;
    private final RewriteConfig config;

    private static final String REWRITE_PROMPT = """
            你是一个查询改写助手。根据对话历史和用户的最新问题，
            将问题改写为一个独立的完整的检索查询。

            要求：
            1. 如果最新问题中包含代词（它、这个、那个等）或省略了关键信息，请结合对话历史补全
            2. 如果问题已经足够完整清晰，请原样输出，不要画蛇添足
            3. 不要添加用户没有提到的信息
            4. 只输出改写后的查询，不要输出任何解释、前缀或多余内容
            5. 改写后的查询应该是一个独立的句子，脱离对话历史也能理解

            对话历史：
            %s

            用户最新问题：%s

            改写后的查询：""";

    public String rewrite(List<ChatMessage> history, String currentQuery) {
        if (!config.isEnabled()) {
            return currentQuery;
        }

        try {
            StringBuilder historyText = new StringBuilder();
            if (history.isEmpty()) {
                historyText.append("（无历史对话）");
            } else {
                for (ChatMessage msg : history) {
                    String roleName = "user".equals(msg.getRole()) ? "用户" : "助手";
                    historyText.append(roleName).append("：")
                            .append(msg.getContent()).append("\n");
                }
            }

            String prompt = String.format(REWRITE_PROMPT,
                    historyText.toString(), currentQuery);

            String rewritten = client.chat(null, prompt, config.getModel());

            if (rewritten != null && !rewritten.isEmpty() && rewritten.length() < 500) {
                log.debug("Query rewritten: '{}' -> '{}'", currentQuery, rewritten);
                return rewritten.trim();
            }
        } catch (Exception e) {
            log.warn("Query rewrite failed, using original: {}", e.getMessage());
        }

        return currentQuery;
    }
}
