package com.wokrag.agent.service.memory;

import com.wokrag.agent.config.MemoryConfig;
import com.wokrag.agent.model.ChatMessage;
import com.wokrag.agent.model.ChatSession;
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
public class SlidingWindowMemoryService implements SessionMemoryService {

    private final MemoryConfig config;
    private final Map<String, ChatSession> sessionStore = new ConcurrentHashMap<>();

    @Override
    public List<ChatMessage> getMessages(String sessionId) {
        ChatSession session = sessionStore.get(sessionId);
        if (session == null) {
            return List.of();
        }

        List<ChatMessage> all = session.getMessages();
        int keepCount = config.getMaxRounds() * 2;
        if (all.size() <= keepCount) {
            return new ArrayList<>(all);
        }
        return new ArrayList<>(all.subList(all.size() - keepCount, all.size()));
    }

    @Override
    public void addMessage(String sessionId, String role, String content) {
        ChatSession session = sessionStore.computeIfAbsent(sessionId,
                ChatSession::new);
        session.addMessage(new ChatMessage(role, content));
        log.debug("Added message to session {}, total: {}",
                sessionId, session.getMessages().size());
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
}
