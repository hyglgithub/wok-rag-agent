package com.wokrag.agent.service.memory;

import com.wokrag.agent.model.ChatMessage;

import java.util.List;

public interface SessionMemoryService {
    List<ChatMessage> getMessages(String sessionId);
    void addMessage(String sessionId, String role, String content);
    void clearSession(String sessionId);
    boolean sessionExists(String sessionId);
    void cleanupExpiredSessions();
}
