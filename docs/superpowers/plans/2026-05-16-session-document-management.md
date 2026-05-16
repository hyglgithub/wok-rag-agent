# Session + Document Management Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add backend APIs for session history and document management so the frontend History and Knowledge pages become functional.

**Architecture:** SQLite (via JdbcTemplate) stores session/message/document metadata. Milvus stores document vectors. Two new controllers expose REST endpoints matching the frontend's existing API client functions.

**Tech Stack:** Spring Boot 3.2.5, Spring JDBC, SQLite JDBC 3.45.1.0, JdbcTemplate, Gson, Lombok

---

## File Map

### New Files

| File | Responsibility |
|------|---------------|
| `src/main/resources/schema.sql` | SQLite table definitions |
| `src/main/java/com/wokrag/agent/config/SQLiteConfig.java` | PRAGMA foreign_keys, SQLite dialect workarounds |
| `src/main/java/com/wokrag/agent/repository/SessionRepository.java` | Session + Message CRUD via JdbcTemplate |
| `src/main/java/com/wokrag/agent/repository/DocumentRepository.java` | Document CRUD via JdbcTemplate |
| `src/main/java/com/wokrag/agent/controller/SessionController.java` | REST endpoints for session management |
| `src/main/java/com/wokrag/agent/controller/DocumentController.java` | REST endpoints for document management |

### Modified Files

| File | Change |
|------|--------|
| `pom.xml` | Add sqlite-jdbc + spring-boot-starter-jdbc dependencies |
| `src/main/resources/application.yml` | Add datasource config |
| `src/main/java/com/wokrag/agent/service/rag/RagPipeline.java` | Persist messages to SQLite after chat |
| `src/main/java/com/wokrag/agent/client/MilvusClientWrapper.java` | Add `deleteByDocId()` method |
| `frontend/src/pages/ChatPage.tsx` | Load session messages when navigating to `/chat/:sessionId` |

---

### Task 1: Add SQLite Dependencies

**Files:**
- Modify: `pom.xml`

- [ ] **Step 1: Add sqlite-jdbc and spring-boot-starter-jdbc to pom.xml**

Add these two dependencies inside the `<dependencies>` section of `pom.xml`, after the existing `spring-boot-starter-web` dependency:

```xml
<!-- SQLite JDBC -->
<dependency>
    <groupId>org.xerial</groupId>
    <artifactId>sqlite-jdbc</artifactId>
    <version>3.45.1.0</version>
</dependency>

<!-- Spring JDBC (for JdbcTemplate) -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-jdbc</artifactId>
</dependency>
```

- [ ] **Step 2: Verify dependencies resolve**

Run: `cd E:/idea_workspace/wok-rag-agent && mvn dependency:resolve -pl . -DincludeArtifactIds=sqlite-jdbc,spring-boot-starter-jdbc`
Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add pom.xml
git commit -m "deps: add SQLite JDBC and Spring JDBC dependencies"
```

---

### Task 2: SQLite Configuration and Schema

**Files:**
- Create: `src/main/resources/schema.sql`
- Create: `src/main/java/com/wokrag/agent/config/SQLiteConfig.java`
- Modify: `src/main/resources/application.yml`

- [ ] **Step 1: Create schema.sql**

Create file `src/main/resources/schema.sql`:

```sql
CREATE TABLE IF NOT EXISTS sessions (
    session_id TEXT PRIMARY KEY,
    title TEXT NOT NULL,
    created_at TEXT NOT NULL DEFAULT (datetime('now')),
    last_active_at TEXT NOT NULL DEFAULT (datetime('now')),
    message_count INTEGER NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS messages (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    session_id TEXT NOT NULL,
    role TEXT NOT NULL,
    content TEXT NOT NULL,
    citations TEXT,
    created_at TEXT NOT NULL DEFAULT (datetime('now')),
    FOREIGN KEY (session_id) REFERENCES sessions(session_id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS documents (
    doc_id TEXT PRIMARY KEY,
    name TEXT NOT NULL,
    source TEXT,
    upload_time TEXT NOT NULL DEFAULT (datetime('now')),
    chunk_count INTEGER NOT NULL DEFAULT 0
);
```

- [ ] **Step 2: Create SQLiteConfig.java**

Create file `src/main/java/com/wokrag/agent/config/SQLiteConfig.java`:

```java
package com.wokrag.agent.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

@Slf4j
@Configuration
public class SQLiteConfig {

    @Bean
    public JdbcTemplate jdbcTemplate(DataSource dataSource) throws SQLException {
        // Enable foreign keys for SQLite
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("PRAGMA foreign_keys = ON");
            log.info("SQLite foreign keys enabled");
        }
        return new JdbcTemplate(dataSource);
    }
}
```

- [ ] **Step 3: Add datasource config to application.yml**

Add the following block at the top of `src/main/resources/application.yml`, right after the `spring.application.name` line and before `spring.servlet.multipart`:

```yaml
  datasource:
    url: jdbc:sqlite:data/wok-rag.db
    driver-class-name: org.sqlite.JDBC
  sql:
    init:
      mode: always
      schema-locations: classpath:schema.sql
```

The resulting `spring:` section should look like:

```yaml
spring:
  application:
    name: wok-rag-agent
  datasource:
    url: jdbc:sqlite:data/wok-rag.db
    driver-class-name: org.sqlite.JDBC
  sql:
    init:
      mode: always
      schema-locations: classpath:schema.sql
  servlet:
    multipart:
      max-file-size: 50MB
      max-request-size: 50MB
```

- [ ] **Step 4: Add .gitignore entry for data directory**

Add this line to `.gitignore` (create the file if it doesn't exist):

```
data/
```

- [ ] **Step 5: Commit**

```bash
git add src/main/resources/schema.sql src/main/java/com/wokrag/agent/config/SQLiteConfig.java src/main/resources/application.yml .gitignore
git commit -m "feat: add SQLite configuration and schema"
```

---

### Task 3: SessionRepository

**Files:**
- Create: `src/main/java/com/wokrag/agent/repository/SessionRepository.java`

- [ ] **Step 1: Create SessionRepository**

Create file `src/main/java/com/wokrag/agent/repository/SessionRepository.java`:

```java
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

    public boolean sessionExists(String sessionId) {
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM sessions WHERE session_id = ?",
                Integer.class, sessionId
        );
        return count != null && count > 0;
    }
}
```

- [ ] **Step 2: Verify compilation**

Run: `cd E:/idea_workspace/wok-rag-agent && mvn compile -q`
Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add src/main/java/com/wokrag/agent/repository/SessionRepository.java
git commit -m "feat: add SessionRepository for session/message CRUD"
```

---

### Task 4: SessionController

**Files:**
- Create: `src/main/java/com/wokrag/agent/controller/SessionController.java`

- [ ] **Step 1: Create SessionController**

Create file `src/main/java/com/wokrag/agent/controller/SessionController.java`:

```java
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

    @DeleteMapping("/{sessionId}")
    @Operation(summary = "Delete a session")
    public ResponseEntity<Map<String, String>> deleteSession(@PathVariable String sessionId) {
        sessionRepository.deleteSession(sessionId);
        return ResponseEntity.ok(Map.of("status", "ok"));
    }
}
```

- [ ] **Step 2: Verify compilation**

Run: `cd E:/idea_workspace/wok-rag-agent && mvn compile -q`
Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add src/main/java/com/wokrag/agent/controller/SessionController.java
git commit -m "feat: add SessionController with list, messages, delete endpoints"
```

---

### Task 5: Integrate Session Persistence into RagPipeline

**Files:**
- Modify: `src/main/java/com/wokrag/agent/service/rag/RagPipeline.java`

The RagPipeline currently calls `sessionMemoryService.addMessage()` to store messages in memory. We need to also persist to SQLite via SessionRepository. The approach: inject SessionRepository and call it alongside the existing in-memory storage.

- [ ] **Step 1: Add SessionRepository import and field**

In `RagPipeline.java`, add import at the top:

```java
import com.wokrag.agent.repository.SessionRepository;
```

Add the field to the constructor-injected dependencies (after the existing `intentClassifier` field):

```java
private final SessionRepository sessionRepository;
```

Since the class uses `@RequiredArgsConstructor`, Lombok will automatically include it in the constructor.

- [ ] **Step 2: Add persistence helper method**

Add this private method at the bottom of RagPipeline.java, before the `StreamCallback` interface:

```java
private void persistToSqlite(String sessionId, String question, String answer,
                              java.util.List<RagResponse.CitationInfo> citations) {
    if (sessionId == null) return;
    try {
        // Save or update session (title = first user message, truncated to 50 chars)
        String title = question.length() > 50 ? question.substring(0, 50) + "..." : question;
        int messageCount = sessionMemoryService.getMessages(sessionId).size();
        sessionRepository.updateSessionOnMessage(sessionId, title, messageCount);

        // Save user message
        sessionRepository.saveMessage(sessionId, "user", question, null);

        // Save assistant message with citations
        sessionRepository.saveMessage(sessionId, "assistant", answer, citations);
    } catch (Exception e) {
        log.warn("Failed to persist session to SQLite: {}", e.getMessage());
    }
}
```

- [ ] **Step 3: Add persistence calls to each intent handler**

In each of the four handler methods, add a call to `persistToSqlite` after the existing `sessionMemoryService.addMessage()` calls. The call should be placed right after the RagResponse is built, before the return/log statement.

**In `handleKnowledgeIntent`** — add after line 117 (`response.setCitations(...)`), before `log.info("Knowledge intent completed successfully")`:

```java
persistToSqlite(sessionId, question, answer, response.getCitations());
```

**In `handleToolIntent`** — add after line 141 (`response.setCitations(new ArrayList<>())`), before `log.info("Tool intent completed successfully")`:

```java
persistToSqlite(sessionId, question, answer, response.getCitations());
```

**In `handleChitchatIntent`** — add after line 173 (`response.setCitations(new ArrayList<>())`), before `log.info("Chitchat intent completed")`:

```java
persistToSqlite(sessionId, question, answer, response.getCitations());
```

**In `handleClarificationIntent`** — add after line 192 (`response.setCitations(new ArrayList<>())`), before `log.info("Clarification intent, returned guiding prompt")`:

```java
persistToSqlite(sessionId, question, answer, response.getCitations());
```

- [ ] **Step 4: Add persistence to streaming handlers**

For streaming handlers, persistence happens in the `onComplete` callback. Add the same `persistToSqlite` call inside each `onComplete` callback, right after the existing `sessionMemoryService.addMessage()` calls.

**In `handleKnowledgeIntentStreaming`** — inside the `onComplete` callback, after the `sessionMemoryService.addMessage` block (around line 307), add:

```java
persistToSqlite(sessionId, question, fullContent, response.getCitations());
```

**In `handleToolIntentStreaming`** — inside the `onComplete` callback, after the `sessionMemoryService.addMessage` block (around line 349), add:

```java
persistToSqlite(sessionId, question, fullContent, response.getCitations());
```

**In `handleChitchatIntentStreaming`** — after the existing `sessionMemoryService.addMessage` block (around line 386), add:

```java
persistToSqlite(sessionId, question, answer, response.getCitations());
```

Note: `handleClarificationIntentStreaming` does NOT save to session memory (per design), so no persistence call needed there.

- [ ] **Step 5: Verify compilation**

Run: `cd E:/idea_workspace/wok-rag-agent && mvn compile -q`
Expected: BUILD SUCCESS

- [ ] **Step 6: Commit**

```bash
git add src/main/java/com/wokrag/agent/service/rag/RagPipeline.java
git commit -m "feat: persist session messages to SQLite via SessionRepository"
```

---

### Task 6: DocumentRepository

**Files:**
- Create: `src/main/java/com/wokrag/agent/repository/DocumentRepository.java`

- [ ] **Step 1: Create DocumentRepository**

Create file `src/main/java/com/wokrag/agent/repository/DocumentRepository.java`:

```java
package com.wokrag.agent.repository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

@Slf4j
@Repository
@RequiredArgsConstructor
public class DocumentRepository {

    private final JdbcTemplate jdbc;

    public void save(String docId, String name, String source, int chunkCount) {
        jdbc.update(
                "INSERT INTO documents (doc_id, name, source, chunk_count) VALUES (?, ?, ?, ?)",
                docId, name, source, chunkCount
        );
    }

    public List<Map<String, Object>> findAll() {
        return jdbc.queryForList(
                "SELECT doc_id, name, source, upload_time, chunk_count FROM documents ORDER BY upload_time DESC"
        );
    }

    public void delete(String docId) {
        jdbc.update("DELETE FROM documents WHERE doc_id = ?", docId);
    }

    public boolean exists(String docId) {
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM documents WHERE doc_id = ?",
                Integer.class, docId
        );
        return count != null && count > 0;
    }
}
```

- [ ] **Step 2: Verify compilation**

Run: `cd E:/idea_workspace/wok-rag-agent && mvn compile -q`
Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add src/main/java/com/wokrag/agent/repository/DocumentRepository.java
git commit -m "feat: add DocumentRepository for document metadata CRUD"
```

---

### Task 7: Extend MilvusClientWrapper with deleteByDocId

**Files:**
- Modify: `src/main/java/com/wokrag/agent/client/MilvusClientWrapper.java`

- [ ] **Step 1: Add deleteByDocId method**

Add this method to `MilvusClientWrapper.java`, after the `search` method (after line 186):

```java
public void deleteByDocId(String docId) {
    try {
        String expr = "doc_id == \"" + docId + "\"";
        client.delete(io.milvus.v2.service.vector.request.DeleteReq.builder()
                .collectionName(config.getCollectionName())
                .filter(expr)
                .build());
        log.info("Deleted Milvus chunks for doc_id: {}", docId);
    } catch (Exception e) {
        throw new RagException.RetrievalException("Failed to delete chunks for doc_id: " + docId, e);
    }
}
```

- [ ] **Step 2: Verify compilation**

Run: `cd E:/idea_workspace/wok-rag-agent && mvn compile -q`
Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add src/main/java/com/wokrag/agent/client/MilvusClientWrapper.java
git commit -m "feat: add deleteByDocId to MilvusClientWrapper"
```

---

### Task 8: DocumentController

**Files:**
- Create: `src/main/java/com/wokrag/agent/controller/DocumentController.java`

- [ ] **Step 1: Create DocumentController**

Create file `src/main/java/com/wokrag/agent/controller/DocumentController.java`:

```java
package com.wokrag.agent.controller;

import com.wokrag.agent.client.MilvusClientWrapper;
import com.wokrag.agent.config.RagConfig;
import com.wokrag.agent.model.Chunk;
import com.wokrag.agent.model.ParseResult;
import com.wokrag.agent.repository.DocumentRepository;
import com.wokrag.agent.service.document.ChunkService;
import com.wokrag.agent.service.document.DocumentService;
import com.wokrag.agent.service.embedding.EmbeddingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;

@Slf4j
@RestController
@RequestMapping("/api/documents")
@RequiredArgsConstructor
@Tag(name = "Documents", description = "Knowledge base document management")
public class DocumentController {

    private final DocumentService documentService;
    private final ChunkService chunkService;
    private final EmbeddingService embeddingService;
    private final MilvusClientWrapper milvusClient;
    private final DocumentRepository documentRepository;
    private final RagConfig ragConfig;

    @GetMapping
    @Operation(summary = "List all documents")
    public ResponseEntity<Map<String, Object>> listDocuments() {
        List<Map<String, Object>> rows = documentRepository.findAll();

        List<Map<String, Object>> documents = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            Map<String, Object> doc = new LinkedHashMap<>();
            doc.put("id", row.get("doc_id"));
            doc.put("name", row.get("name"));
            doc.put("source", row.get("source") != null ? row.get("source") : "");
            doc.put("uploadTime", row.get("upload_time"));
            doc.put("chunkCount", row.get("chunk_count"));
            documents.add(doc);
        }

        return ResponseEntity.ok(Map.of("documents", documents));
    }

    @PostMapping("/upload")
    @Operation(summary = "Upload a document to the knowledge base")
    public ResponseEntity<Map<String, Object>> uploadDocument(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "source", required = false) String source) {

        String docId = UUID.randomUUID().toString();
        String fileName = file.getOriginalFilename();
        if (source == null || source.isEmpty()) {
            source = fileName;
        }

        log.info("Uploading document: {} (docId={})", fileName, docId);

        // Step 1: Parse file
        ParseResult parseResult = documentService.parseFile(file);
        if (!parseResult.isSuccess()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "PARSE_ERROR",
                    "message", parseResult.getErrorMessage()
            ));
        }

        // Step 2: Chunk text
        List<Chunk> chunks = chunkService.chunkText(
                parseResult.getContent(),
                ragConfig.getChunkSize(),
                ragConfig.getChunkOverlap(),
                source
        );

        if (chunks.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "EMPTY_DOCUMENT",
                    "message", "Document produced no usable content"
            ));
        }

        // Step 3: Generate embeddings
        List<String> texts = chunks.stream().map(Chunk::getContent).toList();
        List<double[]> embeddings = embeddingService.embedBatch(texts);

        // Step 4: Insert into Milvus
        List<Map<String, Object>> rows = new ArrayList<>();
        for (int i = 0; i < chunks.size(); i++) {
            Chunk chunk = chunks.get(i);
            Map<String, Object> row = new HashMap<>();
            row.put("chunk_text", chunk.getContent());
            row.put("text_dense", embeddings.get(i));
            row.put("doc_id", docId);
            row.put("source", source);
            row.put("source_url", "");
            rows.add(row);
        }
        milvusClient.insert(rows);

        // Step 5: Save metadata to SQLite
        documentRepository.save(docId, fileName, source, chunks.size());

        log.info("Document uploaded successfully: {} ({} chunks)", fileName, chunks.size());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", docId);
        result.put("name", fileName);
        result.put("source", source);
        result.put("uploadTime", java.time.LocalDateTime.now().toString());
        result.put("chunkCount", chunks.size());

        return ResponseEntity.ok(result);
    }

    @DeleteMapping("/{docId}")
    @Operation(summary = "Delete a document and its chunks")
    public ResponseEntity<Map<String, String>> deleteDocument(@PathVariable String docId) {
        log.info("Deleting document: {}", docId);

        // Delete from Milvus
        milvusClient.deleteByDocId(docId);

        // Delete from SQLite
        documentRepository.delete(docId);

        log.info("Document deleted: {}", docId);
        return ResponseEntity.ok(Map.of("status", "ok"));
    }
}
```

- [ ] **Step 2: Verify compilation**

Run: `cd E:/idea_workspace/wok-rag-agent && mvn compile -q`
Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add src/main/java/com/wokrag/agent/controller/DocumentController.java
git commit -m "feat: add DocumentController with upload, list, delete endpoints"
```

---

### Task 9: Frontend ChatPage — Load Session Messages

**Files:**
- Modify: `frontend/src/pages/ChatPage.tsx`

- [ ] **Step 1: Add getSessionMessages import and loading logic**

Replace the entire content of `frontend/src/pages/ChatPage.tsx` with:

```tsx
import { useEffect, useRef } from 'react'
import { useParams } from 'react-router-dom'
import { useChatStore } from '@/stores/chatStore'
import { useSessionStore } from '@/stores/sessionStore'
import { getSessionMessages } from '@/api/rag'
import MessageBubble from '@/components/chat/MessageBubble'
import ChatInput from '@/components/chat/ChatInput'
import { MessageSquare, Loader2 } from 'lucide-react'
import type { Message } from '@/types'

export default function ChatPage() {
  const { sessionId } = useParams()
  const { messages, isStreaming, streamingContent, sendMessage, loadSession } = useChatStore()
  const { addLocalSession } = useSessionStore()
  const messagesEndRef = useRef<HTMLDivElement>(null)

  // Load session messages from backend when navigating to /chat/:sessionId
  useEffect(() => {
    if (sessionId) {
      useChatStore.setState({ currentSessionId: sessionId })
      getSessionMessages(sessionId).then((sessionMessages) => {
        if (sessionMessages.length > 0) {
          const messages: Message[] = sessionMessages.map((m, i) => ({
            id: `hist-${sessionId}-${i}`,
            role: m.role as 'user' | 'assistant',
            content: m.content,
            citations: m.citations || [],
            timestamp: new Date(m.timestamp).getTime(),
          }))
          loadSession(sessionId, messages)
        }
      })
    }
  }, [sessionId, loadSession])

  // Scroll to bottom on new messages or streaming
  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' })
  }, [messages.length, streamingContent])

  async function handleSend(question: string) {
    await sendMessage(question)
    const currentId = useChatStore.getState().currentSessionId
    if (currentId) {
      addLocalSession(currentId, question.slice(0, 30) + (question.length > 30 ? '...' : ''))
    }
  }

  return (
    <div className="flex flex-col h-full">
      {/* Messages area */}
      <div className="flex-1 overflow-y-auto">
        {messages.length === 0 ? (
          <div className="flex flex-col items-center justify-center h-full text-muted-foreground">
            <MessageSquare size={48} className="mb-4 opacity-30" />
            <p className="text-lg font-medium">有什么可以帮你的？</p>
            <p className="text-sm mt-1">基于知识库的智能问答助手</p>
          </div>
        ) : (
          <div className="max-w-3xl mx-auto py-4">
            {messages.map((msg) => (
              <MessageBubble key={msg.id} message={msg} />
            ))}

            {/* Streaming indicator */}
            {isStreaming && !streamingContent && (
              <div className="flex items-center gap-2 px-4 py-2 text-muted-foreground text-sm">
                <Loader2 size={16} className="animate-spin" />
                <span>思考中...</span>
              </div>
            )}

            <div ref={messagesEndRef} />
          </div>
        )}
      </div>

      {/* Input area */}
      <ChatInput disabled={isStreaming} onSend={handleSend} />
    </div>
  )
}
```

- [ ] **Step 2: Verify frontend builds**

Run: `cd E:/idea_workspace/wok-rag-agent/frontend && npm run build`
Expected: Build succeeds with no errors

- [ ] **Step 3: Commit**

```bash
git add frontend/src/pages/ChatPage.tsx
git commit -m "feat: load session messages from backend when navigating to /chat/:sessionId"
```

---

### Task 10: End-to-End Verification

- [ ] **Step 1: Start the backend**

Run: `cd E:/idea_workspace/wok-rag-agent && mvn spring-boot:run`
Expected: Application starts, SQLite database `data/wok-rag.db` is created, logs show "SQLite foreign keys enabled"

- [ ] **Step 2: Test session endpoints**

```bash
# List sessions (should be empty initially)
curl http://localhost:8080/api/rag/sessions

# Send a chat message to create a session
curl -X POST http://localhost:8080/api/rag/query \
  -H "Content-Type: application/json" \
  -d '{"question": "你好", "sessionId": "test-session-1"}'

# List sessions again (should have one session)
curl http://localhost:8080/api/rag/sessions

# Get session messages
curl http://localhost:8080/api/rag/sessions/test-session-1/messages

# Delete session
curl -X DELETE http://localhost:8080/api/rag/sessions/test-session-1
```

- [ ] **Step 3: Test document endpoints**

```bash
# List documents (should be empty)
curl http://localhost:8080/api/documents

# Upload a test file
curl -X POST http://localhost:8080/api/documents/upload \
  -F "file=@test.txt" \
  -F "source=test-doc"

# List documents again (should have one)
curl http://localhost:8080/api/documents

# Delete document (use the doc_id from the upload response)
curl -X DELETE http://localhost:8080/api/documents/{doc_id}
```

- [ ] **Step 4: Test frontend integration**

1. Start frontend: `cd frontend && npm run dev`
2. Open `http://localhost:5173`
3. Send a chat message
4. Navigate to History page — session should appear
5. Click the session — should navigate to `/chat/:sessionId` and load messages
6. Navigate to Knowledge page — should show empty document list
7. Upload a document — should appear in the list
8. Delete the document — should disappear from the list

- [ ] **Step 5: Commit any fixes**

If any issues were found and fixed during verification, commit them.
