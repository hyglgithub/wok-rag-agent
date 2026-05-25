# Auto-Token Authentication Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement stateless auto-token authentication so the bundled frontend can authenticate against the backend in production.

**Architecture:** Backend generates a random UUID token on startup, persists it to `data/auth-token.txt`, and validates it on every API request via `ApiKeyAuthFilter`. Frontend presents a login page, stores the token in localStorage, and sends it as `X-API-Key` header on all requests.

**Tech Stack:** Spring Boot 3.2.5, Java 17, React 19, Zustand, TypeScript 6

---

## File Structure

### New Files

| File | Responsibility |
|------|---------------|
| `src/main/java/com/wokrag/agent/service/AuthTokenService.java` | Token generation, persistence to file, constant-time validation |
| `src/main/java/com/wokrag/agent/controller/AuthController.java` | POST /api/auth/login, GET /api/auth/status |
| `src/test/java/com/wokrag/agent/service/AuthTokenServiceTest.java` | Unit tests for token service |
| `src/test/java/com/wokrag/agent/controller/AuthControllerTest.java` | Unit tests for auth controller |
| `frontend/src/stores/authStore.ts` | Zustand store: token, isAuthenticated, setToken, clearToken |
| `frontend/src/api/auth.ts` | login(token), checkAuth() API functions |
| `frontend/src/pages/LoginPage.tsx` | Token input form + login button |

### Modified Files

| File | Change |
|------|--------|
| `src/main/java/com/wokrag/agent/filter/ApiKeyAuthFilter.java` | Inject AuthTokenService, add static/auth path bypass, use token validation |
| `src/test/java/com/wokrag/agent/filter/ApiKeyAuthFilterTest.java` | Update tests for new AuthTokenService dependency |
| `src/main/java/com/wokrag/agent/config/ApiKeyConfig.java` | Remove `key` field |
| `src/main/resources/application.yml` | Remove `security.api-key.key` |
| `src/main/resources/application-prod.yml` | Remove `security.api-key.key` |
| `frontend/src/api/client.ts` | Add X-API-Key header, 401 auto-redirect |
| `frontend/src/api/rag.ts` | Add X-API-Key to streamRag |
| `frontend/src/api/document.ts` | Add X-API-Key to uploadDocument, downloadDocument, getPreviewUrl |
| `frontend/src/App.tsx` | Auth guard wrapping routes |
| `frontend/src/pages/SettingsPage.tsx` | Add logout button |

---

### Task 1: AuthTokenService

**Files:**
- Create: `src/main/java/com/wokrag/agent/service/AuthTokenService.java`
- Test: `src/test/java/com/wokrag/agent/service/AuthTokenServiceTest.java`

- [ ] **Step 1: Write failing test for AuthTokenService**

```java
package com.wokrag.agent.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class AuthTokenServiceTest {

    @TempDir
    Path tempDir;

    @Test
    void testGenerateTokenCreatesFile() throws IOException {
        AuthTokenService service = new AuthTokenService();
        service.setTokenFilePath(tempDir.resolve("auth-token.txt").toString());
        service.init();

        String token = service.getToken();
        assertNotNull(token);
        assertEquals(36, token.length()); // UUID format

        String fileContent = Files.readString(tempDir.resolve("auth-token.txt"));
        assertEquals(token, fileContent.trim());
    }

    @Test
    void testValidateCorrectToken() throws IOException {
        AuthTokenService service = new AuthTokenService();
        service.setTokenFilePath(tempDir.resolve("auth-token.txt").toString());
        service.init();

        assertTrue(service.validate(service.getToken()));
    }

    @Test
    void testValidateWrongToken() throws IOException {
        AuthTokenService service = new AuthTokenService();
        service.setTokenFilePath(tempDir.resolve("auth-token.txt").toString());
        service.init();

        assertFalse(service.validate("wrong-token"));
        assertFalse(service.validate(null));
        assertFalse(service.validate(""));
    }

    @Test
    void testTokenOverwrittenOnRestart() throws IOException {
        AuthTokenService service1 = new AuthTokenService();
        service1.setTokenFilePath(tempDir.resolve("auth-token.txt").toString());
        service1.init();
        String token1 = service1.getToken();

        AuthTokenService service2 = new AuthTokenService();
        service2.setTokenFilePath(tempDir.resolve("auth-token.txt").toString());
        service2.init();
        String token2 = service2.getToken();

        assertNotEquals(token1, token2);
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn test -Dtest=AuthTokenServiceTest -pl . -q`
Expected: FAIL — `AuthTokenService` class does not exist

- [ ] **Step 3: Implement AuthTokenService**

Create `src/main/java/com/wokrag/agent/service/AuthTokenService.java`:

```java
package com.wokrag.agent.service;

import jakarta.annotation.PostConstruct;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;

@Slf4j
@Service
public class AuthTokenService {

    @Setter
    private String tokenFilePath = "data/auth-token.txt";

    private String token;

    @PostConstruct
    public void init() {
        token = UUID.randomUUID().toString();
        try {
            Path path = Path.of(tokenFilePath);
            Files.createDirectories(path.getParent());
            Files.writeString(path, token);
        } catch (IOException e) {
            log.error("Failed to persist auth token to {}: {}", tokenFilePath, e.getMessage());
        }
        log.info("\n========================================\n  API Token: {}\n========================================", token);
    }

    public String getToken() {
        return token;
    }

    public boolean validate(String provided) {
        if (provided == null || provided.isBlank()) {
            return false;
        }
        try {
            return MessageDigest.isEqual(
                    token.getBytes(java.nio.charset.StandardCharsets.UTF_8),
                    provided.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        } catch (Exception e) {
            return false;
        }
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `mvn test -Dtest=AuthTokenServiceTest -pl . -q`
Expected: PASS — all 4 tests pass

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/wokrag/agent/service/AuthTokenService.java src/test/java/com/wokrag/agent/service/AuthTokenServiceTest.java
git commit -m "feat: add AuthTokenService with token generation, persistence, and validation"
```

---

### Task 2: AuthController + LoginRequest

**Files:**
- Create: `src/main/java/com/wokrag/agent/controller/AuthController.java`
- Test: `src/test/java/com/wokrag/agent/controller/AuthControllerTest.java`

- [ ] **Step 1: Write failing test for AuthController**

```java
package com.wokrag.agent.controller;

import com.wokrag.agent.service.AuthTokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AuthControllerTest {

    private MockMvc mockMvc;
    private AuthTokenService tokenService;

    @BeforeEach
    void setUp() throws Exception {
        tokenService = new AuthTokenService();
        // Use a temp file to avoid test pollution
        tokenService.setTokenFilePath("data/test-auth-token.txt");
        tokenService.init();
        AuthController controller = new AuthController(tokenService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void testLoginWithValidToken() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\": \"" + tokenService.getToken() + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("OK"));
    }

    @Test
    void testLoginWithInvalidToken() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\": \"wrong-token\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void testStatusWithValidHeader() throws Exception {
        mockMvc.perform(get("/api/auth/status")
                        .header("X-API-Key", tokenService.getToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.authenticated").value(true));
    }

    @Test
    void testStatusWithInvalidHeader() throws Exception {
        mockMvc.perform(get("/api/auth/status")
                        .header("X-API-Key", "wrong"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void testStatusWithoutHeader() throws Exception {
        mockMvc.perform(get("/api/auth/status"))
                .andExpect(status().isUnauthorized());
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn test -Dtest=AuthControllerTest -pl . -q`
Expected: FAIL — `AuthController` class does not exist

- [ ] **Step 3: Implement AuthController**

Create `src/main/java/com/wokrag/agent/controller/AuthController.java`:

```java
package com.wokrag.agent.controller;

import com.wokrag.agent.service.AuthTokenService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Auth", description = "Authentication endpoints")
public class AuthController {

    private final AuthTokenService tokenService;

    @PostMapping("/login")
    @Operation(summary = "Login with API token")
    public ResponseEntity<Map<String, String>> login(@RequestBody LoginRequest request) {
        if (tokenService.validate(request.getToken())) {
            return ResponseEntity.ok(Map.of("message", "OK"));
        }
        return ResponseEntity.status(401).body(Map.of("errorMessage", "Invalid token"));
    }

    @GetMapping("/status")
    @Operation(summary = "Check authentication status")
    public ResponseEntity<Map<String, Object>> status(@RequestHeader(value = "X-API-Key", required = false) String apiKey) {
        if (tokenService.validate(apiKey)) {
            return ResponseEntity.ok(Map.of("authenticated", true));
        }
        return ResponseEntity.status(401).body(Map.of("authenticated", false));
    }

    @Data
    public static class LoginRequest {
        private String token;
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `mvn test -Dtest=AuthControllerTest -pl . -q`
Expected: PASS — all 5 tests pass

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/wokrag/agent/controller/AuthController.java src/test/java/com/wokrag/agent/controller/AuthControllerTest.java
git commit -m "feat: add AuthController with login and status endpoints"
```

---

### Task 3: Modify ApiKeyAuthFilter

**Files:**
- Modify: `src/main/java/com/wokrag/agent/filter/ApiKeyAuthFilter.java`
- Modify: `src/test/java/com/wokrag/agent/filter/ApiKeyAuthFilterTest.java`

- [ ] **Step 1: Rewrite ApiKeyAuthFilter**

Replace `src/main/java/com/wokrag/agent/filter/ApiKeyAuthFilter.java` with:

```java
package com.wokrag.agent.filter;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.wokrag.agent.config.ApiKeyConfig;
import com.wokrag.agent.service.AuthTokenService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Set;

@Slf4j
@Component
public class ApiKeyAuthFilter extends OncePerRequestFilter {

    private final ApiKeyConfig apiKeyConfig;
    private final AuthTokenService authTokenService;
    private final Gson gson = new Gson();

    private static final String AUTH_HEADER = "X-API-Key";
    private static final Set<String> EXEMPT_PATHS = Set.of(
            "/actuator/health",
            "/actuator/info",
            "/actuator/prometheus",
            "/swagger-ui.html",
            "/v3/api-docs",
            "/api/auth/login",
            "/api/auth/status"
    );

    public ApiKeyAuthFilter(ApiKeyConfig apiKeyConfig, AuthTokenService authTokenService) {
        this.apiKeyConfig = apiKeyConfig;
        this.authTokenService = authTokenService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                     HttpServletResponse response,
                                     FilterChain filterChain)
            throws ServletException, IOException {

        if (!apiKeyConfig.isEnabled()) {
            filterChain.doFilter(request, response);
            return;
        }

        String path = request.getRequestURI();
        String method = request.getMethod();

        // Static resources and exempt paths bypass auth
        if (isPublicPath(path, method)) {
            filterChain.doFilter(request, response);
            return;
        }

        // Check X-API-Key header against auto-generated token
        String providedKey = request.getHeader(AUTH_HEADER);
        if (authTokenService.validate(providedKey)) {
            filterChain.doFilter(request, response);
            return;
        }

        if (providedKey == null || providedKey.isBlank()) {
            log.warn("Missing API key for path: {} from {}", path, request.getRemoteAddr());
            sendError(response, HttpServletResponse.SC_UNAUTHORIZED, "Missing API key. Provide X-API-Key header.");
        } else {
            log.warn("Invalid API key for path: {} from {}", path, request.getRemoteAddr());
            sendError(response, HttpServletResponse.SC_FORBIDDEN, "Invalid API key.");
        }
    }

    private boolean isPublicPath(String path, String method) {
        // Exact match exempt paths
        if (EXEMPT_PATHS.contains(path)) {
            return true;
        }
        // Swagger prefix
        if (path.startsWith("/swagger-ui") || path.startsWith("/v3/api-docs")) {
            return true;
        }
        // Static resources: root, index.html, assets, favicon
        if (path.equals("/") || path.equals("/index.html")
                || path.startsWith("/assets/") || path.startsWith("/favicon")) {
            return true;
        }
        return false;
    }

    private void sendError(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json;charset=UTF-8");
        JsonObject error = new JsonObject();
        error.addProperty("errorCode", status == 401 ? "UNAUTHORIZED" : "FORBIDDEN");
        error.addProperty("errorMessage", message);
        response.getWriter().write(gson.toJson(error));
    }
}
```

- [ ] **Step 2: Update ApiKeyAuthFilterTest**

Replace `src/test/java/com/wokrag/agent/filter/ApiKeyAuthFilterTest.java` with:

```java
package com.wokrag.agent.filter;

import com.wokrag.agent.config.ApiKeyConfig;
import com.wokrag.agent.service.AuthTokenService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class ApiKeyAuthFilterTest {

    @TempDir
    Path tempDir;

    private ApiKeyAuthFilter filter;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;
    private FilterChain filterChain;
    private ApiKeyConfig apiKeyConfig;
    private AuthTokenService tokenService;

    @BeforeEach
    void setUp() throws Exception {
        apiKeyConfig = new ApiKeyConfig();
        tokenService = new AuthTokenService();
        tokenService.setTokenFilePath(tempDir.resolve("auth-token.txt").toString());
        tokenService.init();
        filter = new ApiKeyAuthFilter(apiKeyConfig, tokenService);
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        filterChain = (req, res) -> {};
    }

    @Test
    void testDisabledFilterPassesThrough() throws ServletException, IOException {
        apiKeyConfig.setEnabled(false);
        request.setRequestURI("/api/rag/query");
        filter.doFilter(request, response, filterChain);
        assertEquals(200, response.getStatus());
    }

    @Test
    void testHealthEndpointBypassesAuth() throws ServletException, IOException {
        apiKeyConfig.setEnabled(true);
        request.setRequestURI("/actuator/health");
        filter.doFilter(request, response, filterChain);
        assertEquals(200, response.getStatus());
    }

    @Test
    void testSwaggerEndpointBypassesAuth() throws ServletException, IOException {
        apiKeyConfig.setEnabled(true);
        request.setRequestURI("/swagger-ui.html");
        filter.doFilter(request, response, filterChain);
        assertEquals(200, response.getStatus());
    }

    @Test
    void testStaticResourceBypassesAuth() throws ServletException, IOException {
        apiKeyConfig.setEnabled(true);
        request.setRequestURI("/");
        filter.doFilter(request, response, filterChain);
        assertEquals(200, response.getStatus());

        request = new MockHttpServletRequest();
        request.setRequestURI("/index.html");
        filter.doFilter(request, response, filterChain);
        assertEquals(200, response.getStatus());

        request = new MockHttpServletRequest();
        request.setRequestURI("/assets/index-abc123.js");
        filter.doFilter(request, response, filterChain);
        assertEquals(200, response.getStatus());
    }

    @Test
    void testAuthEndpointsBypassAuth() throws ServletException, IOException {
        apiKeyConfig.setEnabled(true);
        request.setRequestURI("/api/auth/login");
        request.setMethod("POST");
        filter.doFilter(request, response, filterChain);
        assertEquals(200, response.getStatus());

        request = new MockHttpServletRequest();
        request.setRequestURI("/api/auth/status");
        filter.doFilter(request, response, filterChain);
        assertEquals(200, response.getStatus());
    }

    @Test
    void testMissingApiKeyReturns401() throws ServletException, IOException {
        apiKeyConfig.setEnabled(true);
        request.setRequestURI("/api/rag/query");
        filter.doFilter(request, response, filterChain);
        assertEquals(401, response.getStatus());
        assertTrue(response.getContentAsString().contains("Missing API key"));
    }

    @Test
    void testWrongApiKeyReturns403() throws ServletException, IOException {
        apiKeyConfig.setEnabled(true);
        request.setRequestURI("/api/rag/query");
        request.addHeader("X-API-Key", "wrong-key");
        filter.doFilter(request, response, filterChain);
        assertEquals(403, response.getStatus());
        assertTrue(response.getContentAsString().contains("Invalid API key"));
    }

    @Test
    void testCorrectTokenPassesThrough() throws ServletException, IOException {
        apiKeyConfig.setEnabled(true);
        request.setRequestURI("/api/rag/query");
        request.addHeader("X-API-Key", tokenService.getToken());
        filter.doFilter(request, response, filterChain);
        assertEquals(200, response.getStatus());
    }
}
```

- [ ] **Step 3: Run tests to verify they pass**

Run: `mvn test -Dtest=ApiKeyAuthFilterTest -pl . -q`
Expected: PASS — all 8 tests pass

- [ ] **Step 4: Commit**

```bash
git add src/main/java/com/wokrag/agent/filter/ApiKeyAuthFilter.java src/test/java/com/wokrag/agent/filter/ApiKeyAuthFilterTest.java
git commit -m "feat: update ApiKeyAuthFilter with static bypass and AuthTokenService validation"
```

---

### Task 4: Clean up ApiKeyConfig and YAML

**Files:**
- Modify: `src/main/java/com/wokrag/agent/config/ApiKeyConfig.java`
- Modify: `src/main/resources/application.yml`
- Modify: `src/main/resources/application-prod.yml`

- [ ] **Step 1: Remove `key` field from ApiKeyConfig**

Edit `src/main/java/com/wokrag/agent/config/ApiKeyConfig.java`:

```java
package com.wokrag.agent.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "security.api-key")
public class ApiKeyConfig {
    private boolean enabled;
}
```

- [ ] **Step 2: Remove `security.api-key.key` from application.yml**

Edit `src/main/resources/application.yml` — change the security section from:

```yaml
# Security Configuration
security:
  api-key:
    enabled: false
    key: wokrag-default-key-change-me
```

to:

```yaml
# Security Configuration
security:
  api-key:
    enabled: false
```

- [ ] **Step 3: Remove `security.api-key.key` from application-prod.yml**

Edit `src/main/resources/application-prod.yml` — change the security section from:

```yaml
security:
  api-key:
    enabled: true
    key: ${API_KEY}
```

to:

```yaml
security:
  api-key:
    enabled: true
```

- [ ] **Step 4: Compile to verify**

Run: `mvn clean compile -q`
Expected: no errors

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/wokrag/agent/config/ApiKeyConfig.java src/main/resources/application.yml src/main/resources/application-prod.yml
git commit -m "refactor: remove env-var API key, use auto-generated token only"
```

---

### Task 5: Frontend authStore

**Files:**
- Create: `frontend/src/stores/authStore.ts`

- [ ] **Step 1: Create authStore**

Create `frontend/src/stores/authStore.ts`:

```typescript
import { create } from 'zustand'

const TOKEN_KEY = 'wok-rag-token'

interface AuthState {
  token: string | null
  isAuthenticated: boolean
  setToken: (token: string) => void
  clearToken: () => void
  loadToken: () => void
}

export const useAuthStore = create<AuthState>()((set) => ({
  token: null,
  isAuthenticated: false,

  setToken: (token) => {
    localStorage.setItem(TOKEN_KEY, token)
    set({ token, isAuthenticated: true })
  },

  clearToken: () => {
    localStorage.removeItem(TOKEN_KEY)
    set({ token: null, isAuthenticated: false })
  },

  loadToken: () => {
    const stored = localStorage.getItem(TOKEN_KEY)
    if (stored) {
      set({ token: stored, isAuthenticated: true })
    }
  },
}))
```

- [ ] **Step 2: Verify TypeScript compiles**

Run: `cd frontend && npx tsc --noEmit --pretty 2>&1 | head -20`
Expected: no errors (or only pre-existing errors unrelated to this file)

- [ ] **Step 3: Commit**

```bash
git add frontend/src/stores/authStore.ts
git commit -m "feat: add authStore for token management"
```

---

### Task 6: Frontend auth API

**Files:**
- Create: `frontend/src/api/auth.ts`

- [ ] **Step 1: Create auth.ts**

Create `frontend/src/api/auth.ts`:

```typescript
import { apiFetch } from './client'

export async function login(token: string): Promise<void> {
  await apiFetch('/api/auth/login', {
    method: 'POST',
    body: JSON.stringify({ token }),
  })
}

export async function checkAuth(token: string): Promise<boolean> {
  try {
    const res = await apiFetch<{ authenticated: boolean }>('/api/auth/status', {
      headers: { 'X-API-Key': token },
    })
    return res.authenticated
  } catch {
    return false
  }
}
```

- [ ] **Step 2: Verify TypeScript compiles**

Run: `cd frontend && npx tsc --noEmit --pretty 2>&1 | head -20`
Expected: no errors from this file

- [ ] **Step 3: Commit**

```bash
git add frontend/src/api/auth.ts
git commit -m "feat: add auth API functions"
```

---

### Task 7: Modify API layer to send token

**Files:**
- Modify: `frontend/src/api/client.ts`
- Modify: `frontend/src/api/rag.ts`
- Modify: `frontend/src/api/document.ts`

- [ ] **Step 1: Modify client.ts — add token header + 401 handling**

Replace `frontend/src/api/client.ts` with:

```typescript
import { useSettingsStore } from '@/stores/settingsStore'
import { useAuthStore } from '@/stores/authStore'

function getBaseUrl(): string {
  return useSettingsStore.getState().settings.apiUrl
}

export async function apiFetch<T>(
  path: string,
  options: RequestInit = {}
): Promise<T> {
  const url = `${getBaseUrl()}${path}`
  const token = useAuthStore.getState().token

  const headers: Record<string, string> = {
    'Content-Type': 'application/json',
    ...(options.headers as Record<string, string>),
  }
  if (token) {
    headers['X-API-Key'] = token
  }

  const response = await fetch(url, {
    ...options,
    headers,
  })

  if (response.status === 401) {
    useAuthStore.getState().clearToken()
    throw { errorCode: 'UNAUTHORIZED', errorMessage: 'Authentication required' }
  }

  if (!response.ok) {
    const error = await response.json().catch(() => ({
      errorCode: 'UNKNOWN_ERROR',
      errorMessage: `HTTP ${response.status}`,
    }))
    throw error
  }

  return response.json()
}
```

- [ ] **Step 2: Modify rag.ts — add token to streamRag**

Edit `frontend/src/api/rag.ts` — change the `streamRag` function to include the token header. Replace the function with:

```typescript
export async function streamRag(request: QueryRequest, signal?: AbortSignal): Promise<Response> {
  const url = `${useSettingsStore.getState().settings.apiUrl}/api/rag/stream`
  const token = useAuthStore.getState().token
  const headers: Record<string, string> = { 'Content-Type': 'application/json' }
  if (token) {
    headers['X-API-Key'] = token
  }

  const response = await fetch(url, {
    method: 'POST',
    headers,
    body: JSON.stringify(request),
    signal,
  })

  if (response.status === 401) {
    useAuthStore.getState().clearToken()
    throw { errorCode: 'UNAUTHORIZED', errorMessage: 'Authentication required' }
  }

  if (!response.ok) {
    const error = await response.json().catch(() => ({
      message: `HTTP ${response.status}`,
    }))
    throw error
  }
  return response
}
```

Also add the import at the top of `rag.ts`:

```typescript
import { useAuthStore } from '@/stores/authStore'
```

- [ ] **Step 3: Modify document.ts — add token to direct fetch calls**

Edit `frontend/src/api/document.ts` — the `uploadDocument`, `downloadDocument`, and `getPreviewUrl` functions bypass `apiFetch` and call `fetch` directly. They need the token header too.

Add import at top:
```typescript
import { useAuthStore } from '@/stores/authStore'
```

Modify `uploadDocument` — add token to headers:
```typescript
export async function uploadDocument(file: File, source?: string): Promise<DocumentInfo> {
  const formData = new FormData()
  formData.append('file', file)
  if (source) formData.append('source', source)

  const baseUrl = useSettingsStore.getState().settings.apiUrl
  const token = useAuthStore.getState().token
  const headers: Record<string, string> = {}
  if (token) {
    headers['X-API-Key'] = token
  }

  const response = await fetch(`${baseUrl}/api/documents/upload`, {
    method: 'POST',
    headers,
    body: formData,
  })
  // ... rest unchanged
```

Modify `downloadDocument` — add token:
```typescript
export async function downloadDocument(docId: string, filename: string) {
  const baseUrl = useSettingsStore.getState().settings.apiUrl
  const token = useAuthStore.getState().token
  const headers: Record<string, string> = {}
  if (token) {
    headers['X-API-Key'] = token
  }
  const response = await fetch(`${baseUrl}/api/documents/${docId}/download`, { headers })
  // ... rest unchanged
```

- [ ] **Step 4: Verify TypeScript compiles**

Run: `cd frontend && npx tsc --noEmit --pretty 2>&1 | head -20`
Expected: no errors from modified files

- [ ] **Step 5: Commit**

```bash
git add frontend/src/api/client.ts frontend/src/api/rag.ts frontend/src/api/document.ts
git commit -m "feat: add X-API-Key header to all frontend API calls"
```

---

### Task 8: LoginPage component

**Files:**
- Create: `frontend/src/pages/LoginPage.tsx`

- [ ] **Step 1: Create LoginPage**

Create `frontend/src/pages/LoginPage.tsx`:

```tsx
import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useAuthStore } from '@/stores/authStore'
import { login } from '@/api/auth'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'

export default function LoginPage() {
  const [token, setToken] = useState('')
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)
  const setAuthToken = useAuthStore((s) => s.setToken)
  const navigate = useNavigate()

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault()
    setError('')
    setLoading(true)

    try {
      await login(token)
      setAuthToken(token)
      navigate('/', { replace: true })
    } catch {
      setError('Token 无效，请重新输入')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="flex items-center justify-center min-h-screen bg-background">
      <form onSubmit={handleSubmit} className="w-full max-w-sm p-6 space-y-4">
        <h1 className="text-xl font-semibold text-center text-foreground">
          Wok RAG Agent
        </h1>
        <p className="text-sm text-muted-foreground text-center">
          请输入 API Token 登录
        </p>
        <Input
          type="text"
          placeholder="API Token"
          value={token}
          onChange={(e) => setToken(e.target.value)}
          autoFocus
        />
        {error && (
          <p className="text-sm text-destructive text-center">{error}</p>
        )}
        <Button type="submit" className="w-full" disabled={loading || !token.trim()}>
          {loading ? '登录中...' : '登录'}
        </Button>
      </form>
    </div>
  )
}
```

- [ ] **Step 2: Verify TypeScript compiles**

Run: `cd frontend && npx tsc --noEmit --pretty 2>&1 | head -20`
Expected: no errors from this file

- [ ] **Step 3: Commit**

```bash
git add frontend/src/pages/LoginPage.tsx
git commit -m "feat: add LoginPage component"
```

---

### Task 9: Auth guard in App.tsx

**Files:**
- Modify: `frontend/src/App.tsx`

- [ ] **Step 1: Modify App.tsx with auth guard**

Replace `frontend/src/App.tsx` with:

```tsx
import { useEffect, useState } from 'react'
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom'
import { ConfirmProvider } from '@/components/ui/confirm-dialog'
import { useAuthStore } from '@/stores/authStore'
import { checkAuth } from '@/api/auth'
import AppLayout from '@/components/layout/AppLayout'
import ChatPage from '@/pages/ChatPage'
import KnowledgePage from '@/pages/KnowledgePage'
import HistoryPage from '@/pages/HistoryPage'
import StatusPage from '@/pages/StatusPage'
import SettingsPage from '@/pages/SettingsPage'
import ChunkPage from '@/pages/ChunkPage'
import LoginPage from '@/pages/LoginPage'

function AuthGuard({ children }: { children: React.ReactNode }) {
  const { isAuthenticated, token, clearToken, setToken, loadToken } = useAuthStore()
  const [checking, setChecking] = useState(true)

  useEffect(() => {
    loadToken()
  }, [loadToken])

  useEffect(() => {
    const stored = useAuthStore.getState().token
    if (!stored) {
      setChecking(false)
      return
    }

    checkAuth(stored).then((valid) => {
      if (valid) {
        setToken(stored)
      } else {
        clearToken()
      }
      setChecking(false)
    })
  }, []) // eslint-disable-line react-hooks/exhaustive-deps

  if (checking) {
    return (
      <div className="flex items-center justify-center min-h-screen">
        <p className="text-muted-foreground">加载中...</p>
      </div>
    )
  }

  if (!isAuthenticated) {
    return <LoginPage />
  }

  return <>{children}</>
}

export default function App() {
  return (
    <ConfirmProvider>
    <BrowserRouter>
      <AuthGuard>
        <Routes>
          <Route element={<AppLayout />}>
            <Route path="/" element={<Navigate to="/chat" replace />} />
            <Route path="/chat" element={<ChatPage />} />
            <Route path="/chat/:sessionId" element={<ChatPage />} />
            <Route path="/knowledge" element={<KnowledgePage />} />
            <Route path="/knowledge/:docId/chunks" element={<ChunkPage />} />
            <Route path="/history" element={<HistoryPage />} />
            <Route path="/status" element={<StatusPage />} />
            <Route path="/settings" element={<SettingsPage />} />
          </Route>
        </Routes>
      </AuthGuard>
    </BrowserRouter>
    </ConfirmProvider>
  )
}
```

- [ ] **Step 2: Verify TypeScript compiles**

Run: `cd frontend && npx tsc --noEmit --pretty 2>&1 | head -20`
Expected: no errors

- [ ] **Step 3: Commit**

```bash
git add frontend/src/App.tsx
git commit -m "feat: add auth guard to App.tsx"
```

---

### Task 10: Logout button in Settings

**Files:**
- Modify: `frontend/src/pages/SettingsPage.tsx`

- [ ] **Step 1: Add logout button to SettingsPage**

Edit `frontend/src/pages/SettingsPage.tsx` — add import and logout section:

Add import at top:
```typescript
import { useNavigate } from 'react-router-dom'
import { useAuthStore } from '@/stores/authStore'
import { LogOut } from 'lucide-react'
```

Add inside the component, before the return:
```typescript
const clearToken = useAuthStore((s) => s.clearToken)
const navigate = useNavigate()

function handleLogout() {
  clearToken()
  navigate('/', { replace: true })
}
```

Add after the "Sidebar Title" section, inside the `<div className="space-y-6">`:
```tsx
{/* Logout */}
<div>
  <label className="block text-sm font-medium text-foreground mb-2">账号</label>
  <Button variant="destructive" onClick={handleLogout}>
    <LogOut size={16} className="mr-2" />
    退出登录
  </Button>
  <p className="text-xs text-muted-foreground mt-1">清除本地 Token，返回登录页</p>
</div>
```

- [ ] **Step 2: Verify TypeScript compiles**

Run: `cd frontend && npx tsc --noEmit --pretty 2>&1 | head -20`
Expected: no errors

- [ ] **Step 3: Commit**

```bash
git add frontend/src/pages/SettingsPage.tsx
git commit -m "feat: add logout button to SettingsPage"
```

---

### Task 11: Full verification

- [ ] **Step 1: Backend compile + unit tests**

Run: `mvn clean test -Dtest="AuthTokenServiceTest,AuthControllerTest,ApiKeyAuthFilterTest,RagControllerTest" -pl . -q`
Expected: all tests pass

- [ ] **Step 2: Frontend TypeScript check**

Run: `cd frontend && npx tsc --noEmit --pretty`
Expected: no errors

- [ ] **Step 3: Full backend test suite**

Run: `mvn test -q 2>&1 | grep -E "Tests run:|BUILD"`
Expected: unit tests pass (integration tests may fail without Milvus/API key — that's expected)

- [ ] **Step 4: Final commit (if any fixups needed)**

```bash
git add -A
git commit -m "fix: address verification issues"
```
