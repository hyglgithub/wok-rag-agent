package com.wokrag.agent.model;

import lombok.Data;
import java.util.ArrayList;
import java.util.List;

@Data
public class ChatSession {
    private String sessionId;
    private List<ChatMessage> messages = new ArrayList<>();
    private String summary;
    private long createdAt;
    private long lastActiveAt;

    public ChatSession(String sessionId) {
        this.sessionId = sessionId;
        this.createdAt = System.currentTimeMillis();
        this.lastActiveAt = System.currentTimeMillis();
    }

    public void addMessage(ChatMessage message) {
        messages.add(message);
        lastActiveAt = System.currentTimeMillis();
    }
}
