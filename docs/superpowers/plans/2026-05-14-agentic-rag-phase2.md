# Phase 2: Advanced Features Implementation Plan

> **For agentic workers:** Use superpowers:subagent-driven-development to implement this plan task-by-task.

**Goal:** Add session memory, query rewriting, intent recognition, tool calling, and SSE streaming to the existing RAG pipeline.

**Architecture:** Extend existing modular monolith with new service layers.

**Tech Stack:** Java 17, Spring Boot 3.2.5, OkHttp 4.9.1, Gson, SiliconFlow API

---

### Task 1: Data Models for Phase 2

**Files:**
- Create: `src/main/java/com/wokrag/agent/model/ChatMessage.java`
- Create: `src/main/java/com/wokrag/agent/model/ChatSession.java`
- Create: `src/main/java/com/wokrag/agent/model/ToolDefinition.java`
- Create: `src/main/java/com/wokrag/agent/model/ToolCall.java`
- Create: `src/main/java/com/wokrag/agent/model/StreamUsage.java`
- Modify: `src/main/java/com/wokrag/agent/model/RagResponse.java` (add sessionId field)

- [ ] **Step 1: Create ChatMessage**
```java
package com.wokrag.agent.model;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ChatMessage {
    private String role;      // "user", "assistant", "system"
    private String content;
    private long timestamp;

    public ChatMessage(String role, String content) {
        this.role = role;
        this.content = content;
        this.timestamp = System.currentTimeMillis();
    }
}
```

- [ ] **Step 2: Create ChatSession**
```java
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
```

- [ ] **Step 3: Create ToolDefinition**
```java
package com.wokrag.agent.model;

import com.google.gson.JsonObject;
import lombok.Data;
import lombok.AllArgsConstructor;

@Data
@AllArgsConstructor
public class ToolDefinition {
    private String name;
    private String description;
    private JsonObject parameters;

    public JsonObject toJson() {
        JsonObject tool = new JsonObject();
        tool.addProperty("type", "function");
        JsonObject function = new JsonObject();
        function.addProperty("name", name);
        function.addProperty("description", description);
        function.add("parameters", parameters);
        tool.add("function", function);
        return tool;
    }
}
```

- [ ] **Step 4: Create ToolCall**
```java
package com.wokrag.agent.model;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ToolCall {
    private String id;
    private String functionName;
    private String arguments;  // JSON string
}
```

- [ ] **Step 5: Create StreamUsage**
```java
package com.wokrag.agent.model;

import lombok.Data;

@Data
public class StreamUsage {
    private int promptTokens;
    private int completionTokens;
    private int totalTokens;
}
```

- [ ] **Step 6: Add sessionId to RagResponse**
Add field `private String sessionId;` to RagResponse.java.

- [ ] **Step 7: Commit**
```bash
git add src/main/java/com/wokrag/agent/model/
git commit -m "feat: add Phase 2 data models (ChatMessage, ChatSession, ToolDefinition, ToolCall, StreamUsage)"
```

---

### Task 2: Configuration Properties for Phase 2

**Files:**
- Create: `src/main/java/com/wokrag/agent/config/MemoryConfig.java`
- Create: `src/main/java/com/wokrag/agent/config/RewriteConfig.java`
- Create: `src/main/java/com/wokrag/agent/config/ToolConfig.java`
- Modify: `src/main/resources/application.yml`
- Modify: `src/main/resources/application-dev.yml`

- [ ] **Step 1: Create MemoryConfig**
```java
package com.wokrag.agent.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "memory")
public class MemoryConfig {
    private String strategy = "sliding_window";  // sliding_window or summary
    private int maxRounds = 5;
    private int tokenThreshold = 3000;
    private int sessionTimeoutMinutes = 30;
}
```

- [ ] **Step 2: Create RewriteConfig**
```java
package com.wokrag.agent.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "rewrite")
public class RewriteConfig {
    private boolean enabled = true;
    private String model = "Qwen/Qwen2.5-7B-Instruct";
}
```

- [ ] **Step 3: Create ToolConfig**
```java
package com.wokrag.agent.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "tool")
public class ToolConfig {
    private boolean enabled = true;
    private String model = "Qwen/Qwen2.5-7B-Instruct";
}
```

- [ ] **Step 4: Update application.yml**
Add sections:
```yaml
# Memory Configuration
memory:
  strategy: sliding_window
  max-rounds: 5
  token-threshold: 3000
  session-timeout-minutes: 30

# Query Rewriting Configuration
rewrite:
  enabled: true
  model: Qwen/Qwen2.5-7B-Instruct

# Tool Calling Configuration
tool:
  enabled: true
  model: Qwen/Qwen2.5-7B-Instruct
```

- [ ] **Step 5: Commit**
```bash
git add src/main/java/com/wokrag/agent/config/ src/main/resources/
git commit -m "feat: add Phase 2 configuration properties (memory, rewrite, tool)"
```

---

### Task 3: Session Memory Service

**Files:**
- Create: `src/main/java/com/wokrag/agent/service/memory/SessionMemoryService.java`
- Create: `src/main/java/com/wokrag/agent/service/memory/SlidingWindowMemoryService.java`
- Test: `src/test/java/com/wokrag/agent/service/memory/SessionMemoryServiceTest.java`

- [ ] **Step 1: Write failing test**
```java
package com.wokrag.agent.service.memory;

import com.wokrag.agent.config.MemoryConfig;
import com.wokrag.agent.model.ChatMessage;
import com.wokrag.agent.model.ChatSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SessionMemoryServiceTest {

    private SlidingWindowMemoryService service;

    @BeforeEach
    void setUp() {
        MemoryConfig config = new MemoryConfig();
        config.setMaxRounds(3);
        config.setSessionTimeoutMinutes(30);
        service = new SlidingWindowMemoryService(config);
    }

    @Test
    void testNewSessionReturnsEmpty() {
        List<ChatMessage> messages = service.getMessages("session-1");
        assertTrue(messages.isEmpty());
    }

    @Test
    void testAddAndGetMessages() {
        service.addMessage("session-1", "user", "你好");
        service.addMessage("session-1", "assistant", "你好！有什么可以帮助你的？");

        List<ChatMessage> messages = service.getMessages("session-1");
        assertEquals(2, messages.size());
        assertEquals("user", messages.get(0).getRole());
        assertEquals("assistant", messages.get(1).getRole());
    }

    @Test
    void testSlidingWindowEviction() {
        // Add 5 rounds (10 messages), maxRounds=3 so only last 3 rounds (6 messages) kept
        for (int i = 1; i <= 5; i++) {
            service.addMessage("session-1", "user", "问题" + i);
            service.addMessage("session-1", "assistant", "回答" + i);
        }

        List<ChatMessage> messages = service.getMessages("session-1");
        assertEquals(6, messages.size());
        assertEquals("问题3", messages.get(0).getContent());
    }

    @Test
    void testClearSession() {
        service.addMessage("session-1", "user", "你好");
        service.clearSession("session-1");
        assertTrue(service.getMessages("session-1").isEmpty());
    }

    @Test
    void testSessionExists() {
        assertFalse(service.sessionExists("session-1"));
        service.addMessage("session-1", "user", "你好");
        assertTrue(service.sessionExists("session-1"));
    }
}
```

- [ ] **Step 2: Create interface**
```java
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
```

- [ ] **Step 3: Implement SlidingWindowMemoryService**
```java
package com.wokrag.agent.service.memory;

import com.wokrag.agent.config.MemoryConfig;
import com.wokrag.agent.model.ChatMessage;
import com.wokrag.agent.model.ChatSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
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
    }

    @Override
    public void clearSession(String sessionId) {
        sessionStore.remove(sessionId);
    }

    @Override
    public boolean sessionExists(String sessionId) {
        return sessionStore.containsKey(sessionId);
    }

    @Scheduled(fixedDelay = 300000) // every 5 minutes
    @Override
    public void cleanupExpiredSessions() {
        long timeout = config.getSessionTimeoutMinutes() * 60 * 1000L;
        long now = System.currentTimeMillis();
        sessionStore.entrySet().removeIf(entry -> {
            boolean expired = (now - entry.getValue().getLastActiveAt()) > timeout;
            if (expired) {
                log.info("Cleaning up expired session: {}", entry.getKey());
            }
            return expired;
        });
    }
}
```

- [ ] **Step 4: Run test**
Run: `mvn test -pl . -Dtest=SessionMemoryServiceTest -q`
Expected: PASS

- [ ] **Step 5: Commit**
```bash
git add src/main/java/com/wokrag/agent/service/memory/ src/test/java/com/wokrag/agent/service/memory/
git commit -m "feat: add session memory service with sliding window strategy"
```

---

### Task 4: Query Rewriter Service

**Files:**
- Create: `src/main/java/com/wokrag/agent/service/rewrite/QueryRewriter.java`
- Test: `src/test/java/com/wokrag/agent/service/rewrite/QueryRewriterTest.java`

- [ ] **Step 1: Write failing test**
```java
package com.wokrag.agent.service.rewrite;

import com.wokrag.agent.client.SiliconFlowClient;
import com.wokrag.agent.config.RewriteConfig;
import com.wokrag.agent.model.ChatMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class QueryRewriterTest {

    private QueryRewriter rewriter;
    private SiliconFlowClient client;

    @BeforeEach
    void setUp() {
        client = mock(SiliconFlowClient.class);
        RewriteConfig config = new RewriteConfig();
        config.setEnabled(true);
        config.setModel("Qwen/Qwen2.5-7B-Instruct");
        rewriter = new QueryRewriter(client, config);
    }

    @Test
    void testRewriteWithHistory() {
        List<ChatMessage> history = List.of(
                new ChatMessage("user", "iPhone 16 Pro 的退货政策是什么？"),
                new ChatMessage("assistant", "iPhone 16 Pro 支持7天无理由退货。")
        );
        when(client.chatWithMessages(any(), any())).thenReturn("iPhone 16 Pro 的保修期是多久？");

        String result = rewriter.rewrite(history, "那它的保修期呢？");
        assertEquals("iPhone 16 Pro 的保修期是多久？", result);
    }

    @Test
    void testRewriteFallsBackOnError() {
        when(client.chatWithMessages(any(), any())).thenThrow(new RuntimeException("API error"));

        String result = rewriter.rewrite(List.of(), "那它的保修期呢？");
        assertEquals("那它的保修期呢？", result); // fallback to original
    }

    @Test
    void testRewriteReturnsOriginalWhenDisabled() {
        RewriteConfig config = new RewriteConfig();
        config.setEnabled(false);
        QueryRewriter disabledRewriter = new QueryRewriter(client, config);

        String result = disabledRewriter.rewrite(List.of(), "那它的保修期呢？");
        assertEquals("那它的保修期呢？", result);
    }
}
```

- [ ] **Step 2: Implement QueryRewriter**
```java
package com.wokrag.agent.service.rewrite;

import com.wokrag.agent.client.SiliconFlowClient;
import com.wokrag.agent.config.RewriteConfig;
import com.wokrag.agent.model.ChatMessage;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
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
```

- [ ] **Step 3: Add chatWithMessages method to SiliconFlowClient**
Need to add a method that accepts a specific model override and message list.

- [ ] **Step 4: Run test**
Run: `mvn test -pl . -Dtest=QueryRewriterTest -q`
Expected: PASS

- [ ] **Step 5: Commit**
```bash
git add src/main/java/com/wokrag/agent/service/rewrite/ src/test/java/com/wokrag/agent/service/rewrite/
git commit -m "feat: add query rewriter service with fallback on failure"
```

---

### Task 5: Tool Calling Infrastructure

**Files:**
- Create: `src/main/java/com/wokrag/agent/service/tool/ToolHandler.java`
- Create: `src/main/java/com/wokrag/agent/service/tool/ToolRegistry.java`
- Create: `src/main/java/com/wokrag/agent/service/tool/FunctionCallService.java`
- Test: `src/test/java/com/wokrag/agent/service/tool/FunctionCallServiceTest.java`

- [ ] **Step 1: Create ToolHandler interface**
```java
package com.wokrag.agent.service.tool;

import com.google.gson.JsonObject;
import com.wokrag.agent.model.ToolDefinition;

public interface ToolHandler {
    ToolDefinition getDefinition();
    String execute(JsonObject arguments);
}
```

- [ ] **Step 2: Create ToolRegistry**
```java
package com.wokrag.agent.service.tool;

import com.wokrag.agent.model.ToolDefinition;
import com.google.gson.JsonArray;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;

@Slf4j
@Component
public class ToolRegistry {

    private final Map<String, ToolHandler> handlers = new LinkedHashMap<>();

    public void register(ToolHandler handler) {
        handlers.put(handler.getDefinition().getName(), handler);
        log.info("Registered tool: {}", handler.getDefinition().getName());
    }

    public ToolHandler getHandler(String name) {
        return handlers.get(name);
    }

    public Collection<ToolHandler> getAllHandlers() {
        return handlers.values();
    }

    public JsonArray getToolDefinitions() {
        JsonArray tools = new JsonArray();
        for (ToolHandler handler : handlers.values()) {
            tools.add(handler.getDefinition().toJson());
        }
        return tools;
    }
}
```

- [ ] **Step 3: Implement FunctionCallService**
```java
package com.wokrag.agent.service.tool;

import com.google.gson.*;
import com.wokrag.agent.client.SiliconFlowClient;
import com.wokrag.agent.config.ToolConfig;
import com.wokrag.agent.model.ToolCall;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class FunctionCallService {

    private final SiliconFlowClient client;
    private final ToolRegistry toolRegistry;
    private final ToolConfig config;

    /**
     * Execute a chat with tool calling support.
     * Returns the final answer string after potential tool invocations.
     */
    public String chatWithTools(String systemPrompt, String userMessage) {
        if (!config.isEnabled()) {
            return client.chat(systemPrompt, userMessage);
        }

        // Round 1: send with tools
        JsonObject firstResponse = client.chatWithTools(
                systemPrompt, userMessage,
                toolRegistry.getToolDefinitions(),
                config.getModel());

        // Check for tool_calls
        JsonArray toolCalls = extractToolCalls(firstResponse);

        if (toolCalls == null || toolCalls.isEmpty()) {
            // No tool calls, return direct answer
            return extractContent(firstResponse);
        }

        // Execute tools and build second round messages
        List<JsonObject> messages = new ArrayList<>();

        // User message
        JsonObject userMsg = new JsonObject();
        userMsg.addProperty("role", "user");
        userMsg.addProperty("content", userMessage);
        messages.add(userMsg);

        // Assistant message with tool_calls
        JsonObject assistantMsg = new JsonObject();
        assistantMsg.addProperty("role", "assistant");
        assistantMsg.add("content", JsonNull.INSTANCE);
        assistantMsg.add("tool_calls", toolCalls);
        messages.add(assistantMsg);

        // Execute each tool and add results
        for (int i = 0; i < toolCalls.size(); i++) {
            JsonObject toolCall = toolCalls.get(i).getAsJsonObject();
            String functionName = toolCall.getAsJsonObject("function")
                    .get("name").getAsString();
            String arguments = toolCall.getAsJsonObject("function")
                    .get("arguments").getAsString();
            String toolCallId = toolCall.get("id").getAsString();

            String result = executeTool(functionName, arguments);

            JsonObject toolMsg = new JsonObject();
            toolMsg.addProperty("role", "tool");
            toolMsg.addProperty("tool_call_id", toolCallId);
            toolMsg.addProperty("content", result);
            messages.add(toolMsg);

            log.info("Executed tool: {} with result length: {}",
                    functionName, result.length());
        }

        // Round 2: send with tool results
        return client.chatWithMessages(systemPrompt, messages, config.getModel());
    }

    private String executeTool(String functionName, String arguments) {
        ToolHandler handler = toolRegistry.getHandler(functionName);
        if (handler == null) {
            return "{\"error\": \"未知的工具: " + functionName + "\"}";
        }

        try {
            JsonObject args = JsonParser.parseString(arguments).getAsJsonObject();
            return handler.execute(args);
        } catch (Exception e) {
            log.error("Tool execution failed: {}", functionName, e);
            return "{\"error\": \"工具执行失败: " + e.getMessage() + "\"}";
        }
    }

    private JsonArray extractToolCalls(JsonObject response) {
        try {
            JsonObject message = response.getAsJsonArray("choices")
                    .get(0).getAsJsonObject()
                    .getAsJsonObject("message");
            if (message.has("tool_calls") && !message.get("tool_calls").isJsonNull()) {
                return message.getAsJsonArray("tool_calls");
            }
        } catch (Exception e) {
            log.debug("No tool_calls in response");
        }
        return null;
    }

    private String extractContent(JsonObject response) {
        try {
            return response.getAsJsonArray("choices")
                    .get(0).getAsJsonObject()
                    .getAsJsonObject("message")
                    .get("content").getAsString();
        } catch (Exception e) {
            return "";
        }
    }
}
```

- [ ] **Step 4: Add required methods to SiliconFlowClient**
Add `chatWithTools()` and `chatWithMessages()` methods.

- [ ] **Step 5: Run test**
Expected: PASS

- [ ] **Step 6: Commit**
```bash
git add src/main/java/com/wokrag/agent/service/tool/
git commit -m "feat: add tool calling infrastructure (ToolRegistry, FunctionCallService)"
```

---

### Task 6: SiliconFlowClient Enhancements

**Files:**
- Modify: `src/main/java/com/wokrag/agent/client/SiliconFlowClient.java`
- Test: `src/test/java/com/wokrag/agent/client/SiliconFlowClientTest.java`

- [ ] **Step 1: Add chatWithTools method**
```java
public JsonObject chatWithTools(String systemPrompt, String userMessage,
                                 JsonArray tools, String model) {
    JsonObject requestBody = new JsonObject();
    requestBody.addProperty("model", model != null ? model : config.getChatModel());
    requestBody.addProperty("temperature", 0.1);
    requestBody.addProperty("max_tokens", 1024);

    JsonArray messages = new JsonArray();
    if (systemPrompt != null && !systemPrompt.isEmpty()) {
        JsonObject sysMsg = new JsonObject();
        sysMsg.addProperty("role", "system");
        sysMsg.addProperty("content", systemPrompt);
        messages.add(sysMsg);
    }
    JsonObject userMsg = new JsonObject();
    userMsg.addProperty("role", "user");
    userMsg.addProperty("content", userMessage);
    messages.add(userMsg);

    requestBody.add("messages", messages);
    requestBody.add("tools", tools);
    requestBody.addProperty("tool_choice", "auto");

    // ... HTTP call and return full JsonObject response
}
```

- [ ] **Step 2: Add chatWithMessages method**
```java
public String chatWithMessages(String systemPrompt, List<JsonObject> messages, String model) {
    JsonObject requestBody = new JsonObject();
    requestBody.addProperty("model", model != null ? model : config.getChatModel());
    requestBody.addProperty("temperature", 0.1);
    requestBody.addProperty("max_tokens", 1024);

    JsonArray messagesArray = new JsonArray();
    if (systemPrompt != null && !systemPrompt.isEmpty()) {
        JsonObject sysMsg = new JsonObject();
        sysMsg.addProperty("role", "system");
        sysMsg.addProperty("content", systemPrompt);
        messagesArray.add(sysMsg);
    }
    for (JsonObject msg : messages) {
        messagesArray.add(msg);
    }
    requestBody.add("messages", messagesArray);

    // ... HTTP call and return content string
}
```

- [ ] **Step 3: Add chat overload with model parameter**
```java
public String chat(String systemPrompt, String userMessage, String model) {
    // Same as chat() but uses specified model
}
```

- [ ] **Step 4: Add streamChat method for SSE**
```java
public void streamChat(String systemPrompt, String userMessage,
                       StreamCallback callback) {
    // Stream implementation with SSE parsing
}

public interface StreamCallback {
    void onToken(String token);
    void onComplete(String fullContent, int promptTokens, int completionTokens);
    void onError(Exception e, String partialContent);
}
```

- [ ] **Step 5: Commit**
```bash
git add src/main/java/com/wokrag/agent/client/SiliconFlowClient.java
git commit -m "feat: add SiliconFlowClient methods for tool calling and streaming"
```

---

### Task 7: SSE Streaming Endpoint

**Files:**
- Create: `src/main/java/com/wokrag/agent/controller/StreamController.java`
- Modify: `src/main/java/com/wokrag/agent/service/rag/RagPipeline.java` (add streaming execute method)

- [ ] **Step 1: Create StreamController**
```java
package com.wokrag.agent.controller;

import com.wokrag.agent.model.RagResponse;
import com.wokrag.agent.service.rag.RagPipeline;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
@RestController
@RequestMapping("/api/rag")
@RequiredArgsConstructor
public class StreamController {

    private final RagPipeline ragPipeline;
    private final ExecutorService executor = Executors.newCachedThreadPool();

    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(@RequestBody StreamRequest request) {
        SseEmitter emitter = new SseEmitter(120_000L); // 2 minute timeout

        executor.execute(() -> {
            try {
                ragPipeline.executeStreaming(
                        request.getQuestion(),
                        request.getSessionId(),
                        new RagPipeline.StreamCallback() {
                            @Override
                            public void onToken(String token) {
                                try {
                                    emitter.send(SseEmitter.event()
                                            .name("token")
                                            .data(token));
                                } catch (IOException e) {
                                    log.warn("SSE send failed", e);
                                }
                            }

                            @Override
                            public void onComplete(RagResponse response) {
                                try {
                                    emitter.send(SseEmitter.event()
                                            .name("done")
                                            .data(response));
                                    emitter.complete();
                                } catch (IOException e) {
                                    emitter.completeWithError(e);
                                }
                            }

                            @Override
                            public void onError(Exception e) {
                                try {
                                    emitter.send(SseEmitter.event()
                                            .name("error")
                                            .data("{\"message\":\"" + e.getMessage() + "\"}"));
                                } catch (IOException ex) {
                                    // ignore
                                }
                                emitter.completeWithError(e);
                            }
                        });
            } catch (Exception e) {
                emitter.completeWithError(e);
            }
        });

        emitter.onTimeout(() -> log.warn("SSE timeout for request: {}", request.getQuestion()));
        emitter.onError(e -> log.warn("SSE error", e));

        return emitter;
    }

    @Data
    public static class StreamRequest {
        private String question;
        private String sessionId;
    }
}
```

- [ ] **Step 2: Commit**
```bash
git add src/main/java/com/wokrag/agent/controller/StreamController.java
git commit -m "feat: add SSE streaming endpoint POST /api/rag/stream"
```

---

### Task 8: Update RagPipeline with Phase 2 Features

**Files:**
- Modify: `src/main/java/com/wokrag/agent/service/rag/RagPipeline.java`
- Test: `src/test/java/com/wokrag/agent/service/rag/RagPipelineTest.java`

- [ ] **Step 1: Update RagPipeline to integrate session memory, query rewriting, and tool calling**
```java
@Slf4j
@Service
@RequiredArgsConstructor
public class RagPipeline {

    private final EmbeddingService embeddingService;
    private final HybridSearchService hybridSearchService;
    private final LlmService llmService;
    private final SessionMemoryService sessionMemoryService;
    private final QueryRewriter queryRewriter;
    private final FunctionCallService functionCallService;
    private final SiliconFlowClient siliconFlowClient;

    public RagResponse execute(String question) {
        return execute(question, null);
    }

    public RagResponse execute(String question, String sessionId) {
        log.info("Executing RAG pipeline for question: {}, sessionId: {}", question, sessionId);

        try {
            // Step 1: Load session history
            List<ChatMessage> history = sessionId != null
                    ? sessionMemoryService.getMessages(sessionId)
                    : List.of();

            // Step 2: Rewrite query
            String rewrittenQuery = queryRewriter.rewrite(history, question);
            log.debug("Rewritten query: {}", rewrittenQuery);

            // Step 3: Intent recognition + tool calling
            String systemPrompt = buildSystemPrompt(history);
            String answer = functionCallService.chatWithTools(systemPrompt, rewrittenQuery);

            // Step 4: Save to session memory
            if (sessionId != null) {
                sessionMemoryService.addMessage(sessionId, "user", question);
                sessionMemoryService.addMessage(sessionId, "assistant", answer);
            }

            RagResponse response = new RagResponse();
            response.setAnswer(answer);
            response.setSessionId(sessionId);
            response.setCitations(new ArrayList<>());
            return response;

        } catch (RagException e) {
            throw e;
        } catch (Exception e) {
            throw new RagException.GenerationException("RAG pipeline failed", e);
        }
    }

    // Streaming version
    public void executeStreaming(String question, String sessionId, StreamCallback callback) {
        // Similar to execute() but uses streaming for generation
    }

    private String buildSystemPrompt(List<ChatMessage> history) {
        // Build system prompt with conversation history context
    }

    public interface StreamCallback {
        void onToken(String token);
        void onComplete(RagResponse response);
        void onError(Exception e);
    }
}
```

- [ ] **Step 2: Commit**
```bash
git add src/main/java/com/wokrag/agent/service/rag/RagPipeline.java
git commit -m "feat: integrate session memory, query rewriting, and tool calling into RAG pipeline"
```

---

### Task 9: Update RagController for Phase 2

**Files:**
- Modify: `src/main/java/com/wokrag/agent/controller/RagController.java`
- Test: `src/test/java/com/wokrag/agent/controller/RagControllerTest.java`

- [ ] **Step 1: Update QueryRequest to include sessionId**
```java
@Data
public static class QueryRequest {
    private String question;
    private String sessionId;  // optional
}
```

- [ ] **Step 2: Update query endpoint**
```java
@PostMapping("/query")
public ResponseEntity<RagResponse> query(@RequestBody QueryRequest request) {
    RagResponse response = ragPipeline.execute(
            request.getQuestion(), request.getSessionId());
    return ResponseEntity.ok(response);
}
```

- [ ] **Step 3: Commit**
```bash
git add src/main/java/com/wokrag/agent/controller/RagController.java
git commit -m "feat: add sessionId support to query endpoint"
```

---

### Task 10: Integration Test for Phase 2

**Files:**
- Create: `src/test/java/com/wokrag/agent/integration/Phase2IntegrationTest.java`

- [ ] **Step 1: Write integration test**
```java
@SpringBootTest
@ActiveProfiles("dev")
class Phase2IntegrationTest {

    @Autowired
    private RagPipeline ragPipeline;

    @Autowired
    private SessionMemoryService sessionMemoryService;

    @Test
    void testMultiTurnConversation() {
        String sessionId = "test-session-" + System.currentTimeMillis();

        // Round 1
        RagResponse r1 = ragPipeline.execute("退货政策是什么？", sessionId);
        assertNotNull(r1.getAnswer());

        // Round 2 - should have context
        RagResponse r2 = ragPipeline.execute("那它的保修期呢？", sessionId);
        assertNotNull(r2.getAnswer());

        // Verify session has messages
        assertTrue(sessionMemoryService.getMessages(sessionId).size() >= 4);
    }

    @Test
    void testQueryRewriting() {
        RagResponse response = ragPipeline.execute("东西坏了咋整？");
        assertNotNull(response.getAnswer());
    }
}
```

- [ ] **Step 2: Commit**
```bash
git add src/test/java/com/wokrag/agent/integration/Phase2IntegrationTest.java
git commit -m "test: add Phase 2 integration tests"
```

---

### Task 11: Final Verification

- [ ] **Step 1: Compile**
Run: `mvn compile -q`
Expected: BUILD SUCCESS

- [ ] **Step 2: Run all tests**
Run: `mvn test -q`
Expected: All tests pass

- [ ] **Step 3: Verify new file structure**
Check all new files exist in correct packages.

- [ ] **Step 4: Commit**
```bash
git add .
git commit -m "chore: Phase 2 final verification and cleanup"
```
