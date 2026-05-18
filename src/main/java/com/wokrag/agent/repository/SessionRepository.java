package com.wokrag.agent.repository;

import com.wokrag.agent.model.ChatMessage;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.lang.reflect.Type;
import java.util.*;

@Slf4j
@Repository
@RequiredArgsConstructor
public class SessionRepository {

    private final JdbcTemplate jdbc;
    private final Gson gson = new Gson();
    private static final Type CITATIONS_TYPE = new TypeToken<List<Map<String, Object>>>(){}.getType();

    public void saveSession(String sessionId, String title) {
        jdbc.update(
                "INSERT OR IGNORE INTO sessions (session_id, title) VALUES (?, ?)",
                sessionId, title
        );
    }

    public void updateSessionOnMessage(String sessionId, String title, int messageCount) {
        jdbc.update(
                "INSERT INTO sessions (session_id, title, message_count, last_active_at) " +
                "VALUES (?, ?, ?, datetime('now')) " +
                "ON CONFLICT(session_id) DO UPDATE SET " +
                "title = CASE WHEN excluded.title != '' THEN excluded.title ELSE sessions.title END, " +
                "message_count = ?, " +
                "last_active_at = datetime('now')",
                sessionId, title, messageCount, messageCount
        );
    }

    public void saveMessage(String sessionId, String role, String content, List<?> citations) {
        String citationsJson = (citations != null && !citations.isEmpty())
                ? gson.toJson(citations) : null;
        jdbc.update(
                "INSERT INTO messages (session_id, role, content, citations) VALUES (?, ?, ?, ?)",
                sessionId, role, content, citationsJson
        );
    }

    public List<Map<String, Object>> findAllSessions() {
        return jdbc.queryForList(
                "SELECT s.session_id, s.title, s.message_count, s.created_at, s.last_active_at, " +
                "(SELECT content FROM messages WHERE session_id = s.session_id ORDER BY id DESC LIMIT 1) as last_message " +
                "FROM sessions s ORDER BY s.last_active_at DESC"
        );
    }

    public List<Map<String, Object>> findMessagesBySessionId(String sessionId) {
        return jdbc.queryForList(
                "SELECT role, content, citations, created_at FROM messages " +
                "WHERE session_id = ? ORDER BY id ASC",
                sessionId
        );
    }

    public void deleteSession(String sessionId) {
        jdbc.update("DELETE FROM sessions WHERE session_id = ?", sessionId);
    }

    public List<Map<String, Object>> searchByKeyword(String keyword) {
        return jdbc.queryForList(
                "SELECT DISTINCT s.session_id, s.title, m.created_at, " +
                "SUBSTR(m.content, MAX(1, INSTR(m.content, ?) - 20), 60) AS matched_preview " +
                "FROM sessions s " +
                "JOIN messages m ON s.session_id = m.session_id " +
                "WHERE m.content LIKE '%' || ? || '%' " +
                "ORDER BY m.created_at DESC " +
                "LIMIT 20",
                keyword, keyword
        );
    }

    public boolean sessionExists(String sessionId) {
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM sessions WHERE session_id = ?",
                Integer.class, sessionId
        );
        return count != null && count > 0;
    }
}
