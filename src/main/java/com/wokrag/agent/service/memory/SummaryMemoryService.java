package com.wokrag.agent.service.memory;

import com.wokrag.agent.client.SiliconFlowClient;
import com.wokrag.agent.config.MemoryConfig;
import com.wokrag.agent.model.ChatMessage;
import com.wokrag.agent.model.ChatSession;
import com.wokrag.agent.util.TextUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@EnableScheduling
@RequiredArgsConstructor
public class SummaryMemoryService implements SessionMemoryService {

    private final MemoryConfig config;
    private final SiliconFlowClient siliconFlowClient;
    private final Map<String, ChatSession> sessionStore = new ConcurrentHashMap<>();

    @Override
    public List<ChatMessage> getMessages(String sessionId) {
        ChatSession session = sessionStore.get(sessionId);
        if (session == null) {
            return List.of();
        }

        List<ChatMessage> all = session.getMessages();
        return new ArrayList<>(all);
    }

    @Override
    public void addMessage(String sessionId, String role, String content) {
        ChatSession session = sessionStore.computeIfAbsent(sessionId, ChatSession::new);
        synchronized (session) {
            session.addMessage(new ChatMessage(role, content));
            log.debug("Added message to session {}, total: {}",
                    sessionId, session.getMessages().size());

            int totalTokens = estimateTotalTokens(session);
            if (totalTokens > config.getTokenThreshold()) {
                try {
                    compress(session);
                } catch (Exception e) {
                    log.warn("Summary compression failed for session {}: {}",
                            sessionId, e.getMessage());
                }
            }
        }
    }

    @Override
    public void clearSession(String sessionId) {
        sessionStore.remove(sessionId);
        log.info("Cleared session: {}", sessionId);
    }

    @Override
    public boolean sessionExists(String sessionId) {
        return sessionStore.containsKey(sessionId);
    }

    @Scheduled(fixedDelay = 300000)
    @Override
    public void cleanupExpiredSessions() {
        long timeout = config.getSessionTimeoutMinutes() * 60 * 1000L;
        long now = System.currentTimeMillis();
        int before = sessionStore.size();
        sessionStore.entrySet().removeIf(entry -> {
            boolean expired = (now - entry.getValue().getLastActiveAt()) > timeout;
            if (expired) {
                log.info("Cleaning up expired session: {}", entry.getKey());
            }
            return expired;
        });
        int cleaned = before - sessionStore.size();
        if (cleaned > 0) {
            log.info("Cleaned up {} expired sessions, {} remaining", cleaned, sessionStore.size());
        }
    }

    @Override
    public String getSummary(String sessionId) {
        ChatSession session = sessionStore.get(sessionId);
        if (session == null) {
            return null;
        }
        return session.getSummary();
    }

    private void compress(ChatSession session) {
        List<ChatMessage> allMessages = session.getMessages();
        int keepCount = config.getMaxRounds() * 2;

        if (allMessages.size() <= keepCount) {
            return;
        }

        // Split: early messages (to compress) + recent messages (to keep)
        List<ChatMessage> earlyMessages = new ArrayList<>(
                allMessages.subList(0, allMessages.size() - keepCount));
        List<ChatMessage> recentMessages = new ArrayList<>(
                allMessages.subList(allMessages.size() - keepCount, allMessages.size()));

        // Build conversation text from early messages
        StringBuilder conversationText = new StringBuilder();
        for (ChatMessage msg : earlyMessages) {
            String roleName = "user".equals(msg.getRole()) ? "用户" : "助手";
            conversationText.append(roleName).append("：").append(msg.getContent()).append("\n");
        }

        // Build summarization prompt
        String existingSummary = session.getSummary();
        StringBuilder summaryPrompt = new StringBuilder();
        summaryPrompt.append("请将以下对话历史压缩为一段简洁的摘要，要求：\n");
        summaryPrompt.append("1. 保留用户的核心意图和关注点\n");
        summaryPrompt.append("2. 保留所有关键实体（产品名、订单号、日期、金额等）\n");
        summaryPrompt.append("3. 保留已经确认的结论和决定\n");
        summaryPrompt.append("4. 保留尚未解决的问题\n");
        summaryPrompt.append("5. 省略寒暄、重复确认、无关细节\n");
        summaryPrompt.append("6. 摘要以第三人称描述，控制在 200 字以内\n");

        if (existingSummary != null && !existingSummary.isEmpty()) {
            summaryPrompt.append("\n已有的历史摘要：\n").append(existingSummary).append("\n");
        }
        summaryPrompt.append("\n需要压缩的新对话：\n").append(conversationText);

        // Call LLM to generate summary
        String summary = siliconFlowClient.chat(
                "你是一个对话摘要助手，负责将对话历史压缩为简洁的摘要。",
                summaryPrompt.toString(),
                config.getSummaryModel());

        // Update session: set summary and keep only recent messages
        session.setSummary(summary);
        session.getMessages().clear();
        session.getMessages().addAll(recentMessages);

        log.info("[摘要压缩] session={}, 压缩 {} 条早期消息, 摘要: {}",
                session.getSessionId(), earlyMessages.size(), summary);
    }

    private int estimateTotalTokens(ChatSession session) {
        int total = 0;
        String summary = session.getSummary();
        if (summary != null && !summary.isEmpty()) {
            total += TextUtil.estimateTokens(summary);
        }
        for (ChatMessage msg : session.getMessages()) {
            total += TextUtil.estimateTokens(msg.getContent());
        }
        return total;
    }
}
