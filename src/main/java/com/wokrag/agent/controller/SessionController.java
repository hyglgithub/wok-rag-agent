package com.wokrag.agent.controller;

import com.wokrag.agent.repository.SessionRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/rag/sessions")
@RequiredArgsConstructor
@Tag(name = "Sessions", description = "Session history management")
public class SessionController {

    private final SessionRepository sessionRepository;

    @GetMapping
    @Operation(summary = "List all sessions")
    public ResponseEntity<Map<String, Object>> listSessions() {
        List<Map<String, Object>> rows = sessionRepository.findAllSessions();

        List<Map<String, Object>> sessions = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            Map<String, Object> session = new LinkedHashMap<>();
            session.put("sessionId", row.get("session_id"));
            session.put("title", row.get("title"));
            session.put("lastMessage", row.get("last_message") != null ? row.get("last_message") : "");
            session.put("lastTime", row.get("last_active_at"));
            session.put("messageCount", row.get("message_count"));
            sessions.add(session);
        }

        return ResponseEntity.ok(Map.of("sessions", sessions));
    }

    @GetMapping("/{sessionId}/messages")
    @Operation(summary = "Get messages for a session")
    public ResponseEntity<Map<String, Object>> getSessionMessages(@PathVariable String sessionId) {
        List<Map<String, Object>> rows = sessionRepository.findMessagesBySessionId(sessionId);

        List<Map<String, Object>> messages = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            Map<String, Object> msg = new LinkedHashMap<>();
            msg.put("role", row.get("role"));
            msg.put("content", row.get("content"));
            msg.put("timestamp", row.get("created_at"));

            String citationsJson = (String) row.get("citations");
            if (citationsJson != null && !citationsJson.isEmpty()) {
                msg.put("citations", new com.google.gson.Gson().fromJson(citationsJson, List.class));
            }
            messages.add(msg);
        }

        return ResponseEntity.ok(Map.of("messages", messages));
    }

    @GetMapping("/search")
    @Operation(summary = "Search sessions by message content")
    public ResponseEntity<Map<String, Object>> searchSessions(@RequestParam("q") String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return ResponseEntity.ok(Map.of("sessions", List.of()));
        }

        List<Map<String, Object>> rows = sessionRepository.searchByKeyword(keyword.trim());

        List<Map<String, Object>> sessions = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            Map<String, Object> session = new LinkedHashMap<>();
            session.put("sessionId", row.get("session_id"));
            session.put("title", row.get("title"));
            session.put("createdAt", row.get("created_at"));
            session.put("matchedPreview", row.get("matched_preview") != null ? row.get("matched_preview") : "");
            sessions.add(session);
        }

        return ResponseEntity.ok(Map.of("sessions", sessions));
    }

    @DeleteMapping("/{sessionId}")
    @Operation(summary = "Delete a session")
    public ResponseEntity<Map<String, String>> deleteSession(@PathVariable String sessionId) {
        sessionRepository.deleteSession(sessionId);
        return ResponseEntity.ok(Map.of("status", "ok"));
    }
}
