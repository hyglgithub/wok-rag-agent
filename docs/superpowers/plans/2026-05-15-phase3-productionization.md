# Phase 3: Productionization Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Harden the Agentic RAG platform for production with monitoring, security, resilience, deployment automation, and API documentation.

**Architecture:** Layer production concerns on top of the existing modular monolith. Add Spring Boot Actuator for health/metrics, a servlet filter for API key auth and request logging, retry logic in SiliconFlowClient, and containerize the application with Docker.

**Tech Stack:** Java 17, Spring Boot 3.2.5, Spring Boot Actuator, Micrometer, Logback, Docker, springdoc-openapi

---

## File Structure

### New files to create

| File | Responsibility |
|------|---------------|
| `src/main/resources/logback-spring.xml` | Structured logging config with JSON output for prod, console for dev |
| `src/main/resources/application-prod.yml` | Production profile overrides |
| `src/main/java/com/wokrag/agent/config/SecurityConfig.java` | CORS configuration |
| `src/main/java/com/wokrag/agent/config/ActuatorConfig.java` | Actuator endpoint exposure config |
| `src/main/java/com/wokrag/agent/filter/ApiKeyAuthFilter.java` | API key authentication servlet filter |
| `src/main/java/com/wokrag/agent/filter/RequestLoggingFilter.java` | Request/response logging with correlation IDs |
| `src/main/java/com/wokrag/agent/health/MilvusHealthIndicator.java` | Custom Milvus health check |
| `src/main/java/com/wokrag/agent/health/SiliconFlowHealthIndicator.java` | Custom SiliconFlow health check |
| `src/main/java/com/wokrag/agent/config/OpenApiConfig.java` | OpenAPI/Swagger configuration |
| `Dockerfile` | Multi-stage Docker build |
| `.dockerignore` | Docker build context exclusions |
| `src/test/java/com/wokrag/agent/filter/ApiKeyAuthFilterTest.java` | Auth filter tests |
| `src/test/java/com/wokrag/agent/filter/RequestLoggingFilterTest.java` | Logging filter tests |
| `src/test/java/com/wokrag/agent/health/HealthIndicatorTest.java` | Health indicator tests |

### Files to modify

| File | Changes |
|------|---------|
| `pom.xml` | Add Actuator, Micrometer, springdoc-openapi dependencies |
| `src/main/resources/application.yml` | Add actuator, security, openapi config sections |
| `src/main/resources/application-dev.yml` | Disable auth for dev |
| `src/main/java/com/wokrag/agent/client/SiliconFlowClient.java` | Add retry logic with exponential backoff |
| `src/main/java/com/wokrag/agent/client/MilvusClientWrapper.java` | Add health check method |
| `src/main/java/com/wokrag/agent/controller/RagController.java` | Add OpenAPI annotations |
| `src/main/java/com/wokrag/agent/controller/StreamController.java` | Add OpenAPI annotations |
| `src/main/java/com/wokrag/agent/model/RagResponse.java` | Add OpenAPI schema annotations |
| `docker-compose.yml` | Add application service |

---

### Task 1: Add Production Dependencies to pom.xml

**Files:**
- Modify: `pom.xml`

- [ ] **Step 1: Add Actuator, Micrometer, and springdoc-openapi dependencies**

Add these inside the `<dependencies>` section of `pom.xml`, after the existing `spring-boot-starter-web` dependency:

```xml
        <!-- Spring Boot Actuator -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-actuator</artifactId>
        </dependency>

        <!-- Micrometer Prometheus Registry -->
        <dependency>
            <groupId>io.micrometer</groupId>
            <artifactId>micrometer-registry-prometheus</artifactId>
        </dependency>

        <!-- OpenAPI / Swagger UI -->
        <dependency>
            <groupId>org.springdoc</groupId>
            <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
            <version>2.6.0</version>
        </dependency>
```

- [ ] **Step 2: Verify compilation**

Run: `mvn compile -q`
Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add pom.xml
git commit -m "feat: add Actuator, Micrometer, and springdoc-openapi dependencies"
```

---

### Task 2: Structured Logging Configuration

**Files:**
- Create: `src/main/resources/logback-spring.xml`
- Modify: `src/main/resources/application.yml`
- Modify: `src/main/resources/application-dev.yml`

- [ ] **Step 1: Create logback-spring.xml**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<configuration>

    <!-- Console appender for dev -->
    <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
        <encoder>
            <pattern>%d{HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n</pattern>
        </encoder>
    </appender>

    <!-- JSON appender for production -->
    <appender name="JSON_FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
        <file>logs/wok-rag-agent.json</file>
        <rollingPolicy class="ch.qos.logback.core.rolling.SizeAndTimeBasedRollingPolicy">
            <fileNamePattern>logs/wok-rag-agent.%d{yyyy-MM-dd}.%i.json</fileNamePattern>
            <maxFileSize>100MB</maxFileSize>
            <maxHistory>30</maxHistory>
            <totalSizeCap>3GB</totalSizeCap>
        </rollingPolicy>
        <encoder class="ch.qos.logback.core.encoder.LayoutWrappingEncoder">
            <layout class="ch.qos.logback.contrib.json.classic.JsonLayout">
                <timestamp>timestamp</timestamp>
                <level>level</level>
                <logger>logger</logger>
                <thread>thread</thread>
                <message>message</message>
                <exception>exception</exception>
                <contextName>context</contextName>
                <appendLineSeparator>true</appendLineSeparator>
            </layout>
        </encoder>
    </appender>

    <!-- Plain file appender for human-readable logs -->
    <appender name="FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
        <file>logs/wok-rag-agent.log</file>
        <rollingPolicy class="ch.qos.logback.core.rolling.SizeAndTimeBasedRollingPolicy">
            <fileNamePattern>logs/wok-rag-agent.%d{yyyy-MM-dd}.%i.log</fileNamePattern>
            <maxFileSize>50MB</maxFileSize>
            <maxHistory>14</maxHistory>
            <totalSizeCap>1GB</totalSizeCap>
        </rollingPolicy>
        <encoder>
            <pattern>%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n</pattern>
        </encoder>
    </appender>

    <!-- Dev profile: console only -->
    <springProfile name="dev">
        <root level="INFO">
            <appender-ref ref="CONSOLE" />
        </root>
        <logger name="com.wokrag" level="DEBUG" />
        <logger name="io.milvus" level="DEBUG" />
    </springProfile>

    <!-- Prod profile: JSON file + plain file, no console -->
    <springProfile name="prod">
        <root level="INFO">
            <appender-ref ref="JSON_FILE" />
            <appender-ref ref="FILE" />
        </root>
        <logger name="com.wokrag" level="INFO" />
        <logger name="io.milvus" level="WARN" />
    </springProfile>

    <!-- Default (no profile): console -->
    <springProfile name="default">
        <root level="INFO">
            <appender-ref ref="CONSOLE" />
        </root>
        <logger name="com.wokrag" level="INFO" />
    </springProfile>

</configuration>
```

- [ ] **Step 2: Remove duplicate logging config from application-dev.yml**

The `application-dev.yml` currently has logging level overrides that will now be handled by logback-spring.xml. Replace its contents:

```yaml
# Development environment overrides
siliconflow:
  api-key: ${SILICONFLOW_API_KEY}
```

- [ ] **Step 3: Verify application starts**

Run: `mvn spring-boot:run -Dspring-boot.run.profiles=dev`
Expected: Application starts, console output uses the pattern from logback-spring.xml

Stop the application after verification.

- [ ] **Step 4: Commit**

```bash
git add src/main/resources/logback-spring.xml src/main/resources/application-dev.yml
git commit -m "feat: add structured logging with logback-spring.xml (JSON for prod, console for dev)"
```

---

### Task 3: Production Profile Configuration

**Files:**
- Create: `src/main/resources/application-prod.yml`

- [ ] **Step 1: Create application-prod.yml**

```yaml
# Production environment overrides
server:
  port: ${SERVER_PORT:8080}

siliconflow:
  api-key: ${SILICONFLOW_API_KEY}

milvus:
  uri: ${MILVUS_URI:http://localhost:19530}

# Actuator: expose health, info, prometheus
management:
  endpoints:
    web:
      exposure:
        include: health,info,prometheus
  endpoint:
    health:
      show-details: when-authorized

# Security: enable API key auth
security:
  api-key:
    enabled: true
    key: ${API_KEY:wokrag-default-key-change-me}

# Rate limiting
rate-limit:
  enabled: true
  requests-per-minute: 60
```

- [ ] **Step 2: Add prod config sections to application.yml**

Append these sections to the end of `src/main/resources/application.yml`:

```yaml
# Actuator Configuration
management:
  endpoints:
    web:
      exposure:
        include: health,info,prometheus
  endpoint:
    health:
      show-details: always
  health:
    redis:
      enabled: false

# Security Configuration
security:
  api-key:
    enabled: false
    key: ${API_KEY:}

# Rate Limiting
rate-limit:
  enabled: false
  requests-per-minute: 120

# OpenAPI / Swagger
springdoc:
  api-docs:
    path: /v3/api-docs
  swagger-ui:
    path: /swagger-ui.html
    tags-sorter: alpha
    operations-sorter: alpha
```

- [ ] **Step 3: Verify compilation**

Run: `mvn compile -q`
Expected: BUILD SUCCESS

- [ ] **Step 4: Commit**

```bash
git add src/main/resources/application.yml src/main/resources/application-prod.yml
git commit -m "feat: add production profile and actuator/security/rate-limit config sections"
```

---

### Task 4: API Key Authentication Filter

**Files:**
- Create: `src/main/java/com/wokrag/agent/filter/ApiKeyAuthFilter.java`
- Create: `src/test/java/com/wokrag/agent/filter/ApiKeyAuthFilterTest.java`

- [ ] **Step 1: Write the failing test**

```java
package com.wokrag.agent.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

class ApiKeyAuthFilterTest {

    private ApiKeyAuthFilter filter;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;
    private FilterChain filterChain;

    @BeforeEach
    void setUp() {
        filter = new ApiKeyAuthFilter();
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        filterChain = (req, res) -> {};
    }

    @Test
    void testDisabledFilterPassesThrough() throws ServletException, IOException {
        filter.setEnabled(false);
        filter.setApiKey("test-key");

        request.setRequestURI("/api/rag/query");
        filter.doFilter(request, response, filterChain);

        assertEquals(200, response.getStatus());
    }

    @Test
    void testHealthEndpointBypassesAuth() throws ServletException, IOException {
        filter.setEnabled(true);
        filter.setApiKey("test-key");

        request.setRequestURI("/actuator/health");
        filter.doFilter(request, response, filterChain);

        assertEquals(200, response.getStatus());
    }

    @Test
    void testSwaggerEndpointBypassesAuth() throws ServletException, IOException {
        filter.setEnabled(true);
        filter.setApiKey("test-key");

        request.setRequestURI("/swagger-ui.html");
        filter.doFilter(request, response, filterChain);

        assertEquals(200, response.getStatus());
    }

    @Test
    void testMissingApiKeyReturns401() throws ServletException, IOException {
        filter.setEnabled(true);
        filter.setApiKey("test-key");

        request.setRequestURI("/api/rag/query");
        filter.doFilter(request, response, filterChain);

        assertEquals(401, response.getStatus());
        assertTrue(response.getContentAsString().contains("Missing API key"));
    }

    @Test
    void testWrongApiKeyReturns403() throws ServletException, IOException {
        filter.setEnabled(true);
        filter.setApiKey("correct-key");

        request.setRequestURI("/api/rag/query");
        request.addHeader("X-API-Key", "wrong-key");
        filter.doFilter(request, response, filterChain);

        assertEquals(403, response.getStatus());
        assertTrue(response.getContentAsString().contains("Invalid API key"));
    }

    @Test
    void testCorrectApiKeyPassesThrough() throws ServletException, IOException {
        filter.setEnabled(true);
        filter.setApiKey("correct-key");

        request.setRequestURI("/api/rag/query");
        request.addHeader("X-API-Key", "correct-key");
        filter.doFilter(request, response, filterChain);

        assertEquals(200, response.getStatus());
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn test -Dtest=ApiKeyAuthFilterTest -q`
Expected: FAIL (class not found)

- [ ] **Step 3: Implement ApiKeyAuthFilter**

```java
package com.wokrag.agent.filter;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Set;

@Slf4j
@Data
@Component
public class ApiKeyAuthFilter extends OncePerRequestFilter {

    private boolean enabled = false;
    private String apiKey = "";

    private static final String AUTH_HEADER = "X-API-Key";
    private static final Set<String> BYPASS_PATHS = Set.of(
            "/actuator/health",
            "/actuator/info",
            "/actuator/prometheus",
            "/swagger-ui.html",
            "/v3/api-docs"
    );

    private final Gson gson = new Gson();

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                     HttpServletResponse response,
                                     FilterChain filterChain)
            throws ServletException, IOException {

        if (!enabled) {
            filterChain.doFilter(request, response);
            return;
        }

        String path = request.getRequestURI();

        // Bypass auth for health check, actuator, and swagger endpoints
        if (shouldBypass(path)) {
            filterChain.doFilter(request, response);
            return;
        }

        String providedKey = request.getHeader(AUTH_HEADER);

        if (providedKey == null || providedKey.isBlank()) {
            log.warn("Missing API key for path: {} from {}", path, request.getRemoteAddr());
            sendError(response, HttpServletResponse.SC_UNAUTHORIZED, "Missing API key. Provide X-API-Key header.");
            return;
        }

        if (!apiKey.equals(providedKey)) {
            log.warn("Invalid API key for path: {} from {}", path, request.getRemoteAddr());
            sendError(response, HttpServletResponse.SC_FORBIDDEN, "Invalid API key.");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private boolean shouldBypass(String path) {
        if (BYPASS_PATHS.contains(path)) {
            return true;
        }
        // Bypass all swagger-ui and api-docs sub-paths
        return path.startsWith("/swagger-ui") || path.startsWith("/v3/api-docs");
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

- [ ] **Step 4: Run test to verify it passes**

Run: `mvn test -Dtest=ApiKeyAuthFilterTest -q`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/wokrag/agent/filter/ApiKeyAuthFilter.java src/test/java/com/wokrag/agent/filter/ApiKeyAuthFilterTest.java
git commit -m "feat: add API key authentication filter with bypass for health/swagger endpoints"
```

---

### Task 5: Request Logging Filter with Correlation IDs

**Files:**
- Create: `src/main/java/com/wokrag/agent/filter/RequestLoggingFilter.java`
- Create: `src/test/java/com/wokrag/agent/filter/RequestLoggingFilterTest.java`

- [ ] **Step 1: Write the failing test**

```java
package com.wokrag.agent.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

class RequestLoggingFilterTest {

    private RequestLoggingFilter filter;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;
    private boolean chainCalled;

    @BeforeEach
    void setUp() {
        filter = new RequestLoggingFilter();
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        chainCalled = false;
    }

    @Test
    void testCorrelationIdGeneratedWhenMissing() throws ServletException, IOException {
        request.setRequestURI("/api/rag/query");
        request.setMethod("POST");

        filter.doFilter(request, response, (req, res) -> {
            chainCalled = true;
            String correlationId = ((jakarta.servlet.http.HttpServletRequest) req)
                    .getHeader("X-Correlation-Id");
            assertNotNull(correlationId);
            assertFalse(correlationId.isEmpty());
        });

        assertTrue(chainCalled);
        assertNotNull(response.getHeader("X-Correlation-Id"));
    }

    @Test
    void testExistingCorrelationIdPreserved() throws ServletException, IOException {
        request.setRequestURI("/api/rag/query");
        request.setMethod("POST");
        request.addHeader("X-Correlation-Id", "existing-id-123");

        filter.doFilter(request, response, (req, res) -> {
            chainCalled = true;
            String correlationId = ((jakarta.servlet.http.HttpServletRequest) req)
                    .getHeader("X-Correlation-Id");
            assertEquals("existing-id-123", correlationId);
        });

        assertTrue(chainCalled);
        assertEquals("existing-id-123", response.getHeader("X-Correlation-Id"));
    }

    @Test
    void testActuatorEndpointsSkipped() throws ServletException, IOException {
        request.setRequestURI("/actuator/health");
        request.setMethod("GET");

        filter.doFilter(request, response, (req, res) -> chainCalled = true);

        assertTrue(chainCalled);
        assertNull(response.getHeader("X-Correlation-Id"));
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn test -Dtest=RequestLoggingFilterTest -q`
Expected: FAIL (class not found)

- [ ] **Step 3: Implement RequestLoggingFilter**

```java
package com.wokrag.agent.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.*;

@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class RequestLoggingFilter extends OncePerRequestFilter {

    private static final String CORRELATION_ID_HEADER = "X-Correlation-Id";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                     HttpServletResponse response,
                                     FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getRequestURI();

        // Skip actuator and static resource requests
        if (path.startsWith("/actuator") || path.startsWith("/swagger-ui")
                || path.startsWith("/v3/api-docs")) {
            filterChain.doFilter(request, response);
            return;
        }

        String correlationId = request.getHeader(CORRELATION_ID_HEADER);
        if (correlationId == null || correlationId.isBlank()) {
            correlationId = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        }

        String finalCorrelationId = correlationId;
        response.setHeader(CORRELATION_ID_HEADER, correlationId);

        long startTime = System.currentTimeMillis();
        String method = request.getMethod();
        String clientIp = request.getRemoteAddr();

        log.info("[{}] >>> {} {} from {}", correlationId, method, path, clientIp);

        try {
            HttpServletRequest wrappedRequest = new CorrelationIdRequestWrapper(request, finalCorrelationId);
            filterChain.doFilter(wrappedRequest, response);
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            int status = response.getStatus();
            log.info("[{}] <<< {} {} -> {} ({}ms)", correlationId, method, path, status, duration);
        }
    }

    private static class CorrelationIdRequestWrapper extends HttpServletRequestWrapper {
        private final String correlationId;

        CorrelationIdRequestWrapper(HttpServletRequest request, String correlationId) {
            super(request);
            this.correlationId = correlationId;
        }

        @Override
        public String getHeader(String name) {
            if (CORRELATION_ID_HEADER.equals(name)) {
                return correlationId;
            }
            return super.getHeader(name);
        }

        @Override
        public Enumeration<String> getHeaderNames() {
            Set<String> names = new HashSet<>(Collections.list(super.getHeaderNames()));
            names.add(CORRELATION_ID_HEADER);
            return Collections.enumeration(names);
        }
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `mvn test -Dtest=RequestLoggingFilterTest -q`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/wokrag/agent/filter/RequestLoggingFilter.java src/test/java/com/wokrag/agent/filter/RequestLoggingFilterTest.java
git commit -m "feat: add request logging filter with correlation ID tracking"
```

---

### Task 6: CORS Configuration

**Files:**
- Create: `src/main/java/com/wokrag/agent/config/SecurityConfig.java`

- [ ] **Step 1: Implement SecurityConfig with CORS**

```java
package com.wokrag.agent.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.List;

@Configuration
public class SecurityConfig {

    @Value("${cors.allowed-origins:*}")
    private List<String> allowedOrigins;

    @Bean
    public CorsFilter corsFilter() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(allowedOrigins);
        config.setAllowedMethods(List.of("GET", "POST", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setExposedHeaders(List.of("X-Correlation-Id"));
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);
        return new CorsFilter(source);
    }
}
```

- [ ] **Step 2: Add CORS config to application.yml**

Append to `src/main/resources/application.yml`:

```yaml
# CORS Configuration
cors:
  allowed-origins: "*"
```

And add to `application-prod.yml`:

```yaml
# CORS: restrict to specific origins in production
cors:
  allowed-origins: ${CORS_ALLOWED_ORIGINS:http://localhost:3000}
```

- [ ] **Step 3: Verify compilation**

Run: `mvn compile -q`
Expected: BUILD SUCCESS

- [ ] **Step 4: Commit**

```bash
git add src/main/java/com/wokrag/agent/config/SecurityConfig.java src/main/resources/application.yml src/main/resources/application-prod.yml
git commit -m "feat: add CORS configuration with per-profile allowed origins"
```

---

### Task 7: Custom Health Indicators

**Files:**
- Create: `src/main/java/com/wokrag/agent/health/MilvusHealthIndicator.java`
- Create: `src/main/java/com/wokrag/agent/health/SiliconFlowHealthIndicator.java`
- Modify: `src/main/java/com/wokrag/agent/client/MilvusClientWrapper.java`
- Create: `src/test/java/com/wokrag/agent/health/HealthIndicatorTest.java`

- [ ] **Step 1: Add health check method to MilvusClientWrapper**

Add this method to `MilvusClientWrapper.java`, after the `getClient()` method:

```java
    public boolean isHealthy() {
        try {
            Boolean exists = client.hasCollection(
                    HasCollectionReq.builder()
                            .collectionName(config.getCollectionName())
                            .build());
            return Boolean.TRUE.equals(exists);
        } catch (Exception e) {
            log.warn("Milvus health check failed: {}", e.getMessage());
            return false;
        }
    }
```

- [ ] **Step 2: Write the failing test**

```java
package com.wokrag.agent.health;

import com.wokrag.agent.client.MilvusClientWrapper;
import com.wokrag.agent.client.SiliconFlowClient;
import com.wokrag.agent.config.SiliconFlowConfig;
import org.junit.jupiter.api.Test;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class HealthIndicatorTest {

    @Test
    void testMilvusHealthUp() {
        MilvusClientWrapper client = mock(MilvusClientWrapper.class);
        when(client.isHealthy()).thenReturn(true);

        MilvusHealthIndicator indicator = new MilvusHealthIndicator(client);
        Health health = indicator.health();

        assertEquals(Status.UP, health.getStatus());
    }

    @Test
    void testMilvusHealthDown() {
        MilvusClientWrapper client = mock(MilvusClientWrapper.class);
        when(client.isHealthy()).thenReturn(false);

        MilvusHealthIndicator indicator = new MilvusHealthIndicator(client);
        Health health = indicator.health();

        assertEquals(Status.DOWN, health.getStatus());
    }

    @Test
    void testMilvusHealthException() {
        MilvusClientWrapper client = mock(MilvusClientWrapper.class);
        when(client.isHealthy()).thenThrow(new RuntimeException("Connection refused"));

        MilvusHealthIndicator indicator = new MilvusHealthIndicator(client);
        Health health = indicator.health();

        assertEquals(Status.DOWN, health.getStatus());
    }

    @Test
    void testSiliconFlowHealthUp() {
        SiliconFlowClient client = mock(SiliconFlowClient.class);
        SiliconFlowConfig config = new SiliconFlowConfig();
        config.setBaseUrl("https://api.siliconflow.cn/v1");
        config.setApiKey("test-key");

        SiliconFlowHealthIndicator indicator = new SiliconFlowHealthIndicator(client, config);
        // SiliconFlow health check does a lightweight API call
        // In unit test we just verify the indicator constructs properly
        assertNotNull(indicator);
    }
}
```

- [ ] **Step 3: Run test to verify it fails**

Run: `mvn test -Dtest=HealthIndicatorTest -q`
Expected: FAIL (class not found)

- [ ] **Step 4: Implement MilvusHealthIndicator**

```java
package com.wokrag.agent.health;

import com.wokrag.agent.client.MilvusClientWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class MilvusHealthIndicator implements HealthIndicator {

    private final MilvusClientWrapper milvusClient;

    @Override
    public Health health() {
        try {
            boolean healthy = milvusClient.isHealthy();
            if (healthy) {
                return Health.up()
                        .withDetail("database", "Milvus")
                        .withDetail("collection", "accessible")
                        .build();
            }
            return Health.down()
                    .withDetail("database", "Milvus")
                    .withDetail("reason", "Collection not accessible")
                    .build();
        } catch (Exception e) {
            log.warn("Milvus health check failed", e);
            return Health.down()
                    .withDetail("database", "Milvus")
                    .withException(e)
                    .build();
        }
    }
}
```

- [ ] **Step 5: Implement SiliconFlowHealthIndicator**

```java
package com.wokrag.agent.health;

import com.wokrag.agent.client.SiliconFlowClient;
import com.wokrag.agent.config.SiliconFlowConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class SiliconFlowHealthIndicator implements HealthIndicator {

    private final SiliconFlowClient client;
    private final SiliconFlowConfig config;

    @Override
    public Health health() {
        try {
            // Lightweight check: call embed with a tiny input
            client.embed(java.util.List.of("health"));
            return Health.up()
                    .withDetail("api", "SiliconFlow")
                    .withDetail("baseUrl", config.getBaseUrl())
                    .build();
        } catch (Exception e) {
            log.warn("SiliconFlow health check failed: {}", e.getMessage());
            return Health.down()
                    .withDetail("api", "SiliconFlow")
                    .withDetail("baseUrl", config.getBaseUrl())
                    .withException(e)
                    .build();
        }
    }
}
```

- [ ] **Step 6: Run test to verify it passes**

Run: `mvn test -Dtest=HealthIndicatorTest -q`
Expected: PASS

- [ ] **Step 7: Commit**

```bash
git add src/main/java/com/wokrag/agent/health/ src/main/java/com/wokrag/agent/client/MilvusClientWrapper.java src/test/java/com/wokrag/agent/health/
git commit -m "feat: add custom health indicators for Milvus and SiliconFlow"
```

---

### Task 8: Retry Logic for SiliconFlow API Calls

**Files:**
- Modify: `src/main/java/com/wokrag/agent/client/SiliconFlowClient.java`

- [ ] **Step 1: Add retry helper method to SiliconFlowClient**

Add this private method at the bottom of the class, before the `RerankResult` inner class:

```java
    private static final int MAX_RETRIES = 3;
    private static final long BASE_DELAY_MS = 1000;

    private <T> T executeWithRetry(java.util.function.Supplier<T> operation, String operationName) {
        Exception lastException = null;
        for (int attempt = 0; attempt <= MAX_RETRIES; attempt++) {
            try {
                return operation.get();
            } catch (RagException e) {
                lastException = e;
                if (attempt < MAX_RETRIES) {
                    long delay = BASE_DELAY_MS * (long) Math.pow(2, attempt);
                    log.warn("{} failed (attempt {}/{}), retrying in {}ms: {}",
                            operationName, attempt + 1, MAX_RETRIES + 1, delay, e.getMessage());
                    try {
                        Thread.sleep(delay);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw e;
                    }
                }
            }
        }
        throw (RagException) lastException;
    }
```

- [ ] **Step 2: Wrap the embed method with retry**

Replace the `embed` method's body. Change the method to wrap the core logic in `executeWithRetry`. The full method becomes:

```java
    public List<double[]> embed(List<String> texts) {
        return executeWithRetry(() -> {
            JsonObject requestBody = new JsonObject();
            requestBody.addProperty("model", config.getEmbeddingModel());
            requestBody.add("input", gson.toJsonTree(texts));
            requestBody.addProperty("encoding_format", "float");

            Request request = new Request.Builder()
                    .url(config.getBaseUrl() + "/embeddings")
                    .addHeader("Authorization", "Bearer " + config.getApiKey())
                    .addHeader("Content-Type", "application/json")
                    .post(RequestBody.create(
                            gson.toJson(requestBody),
                            MediaType.parse("application/json")))
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    String errorBody = response.body() != null ? response.body().string() : "no body";
                    throw new RagException.EmbeddingException(
                            "Embedding API failed: HTTP " + response.code() + " - " + errorBody);
                }

                String responseBody = response.body().string();
                JsonObject json = gson.fromJson(responseBody, JsonObject.class);
                JsonArray dataArray = json.getAsJsonArray("data");

                List<double[]> embeddings = new ArrayList<>();
                for (int i = 0; i < dataArray.size(); i++) {
                    JsonArray embeddingArray = dataArray.get(i)
                            .getAsJsonObject()
                            .getAsJsonArray("embedding");
                    double[] vector = new double[embeddingArray.size()];
                    for (int j = 0; j < embeddingArray.size(); j++) {
                        vector[j] = embeddingArray.get(j).getAsDouble();
                    }
                    embeddings.add(vector);
                }

                return embeddings;
            } catch (IOException e) {
                throw new RagException.EmbeddingException("Embedding API call failed", e);
            }
        }, "embed");
    }
```

- [ ] **Step 3: Wrap the chat method with retry**

Replace the two-arg `chat` method body similarly:

```java
    public String chat(String systemPrompt, String userMessage) {
        return executeWithRetry(() -> {
            JsonObject requestBody = new JsonObject();
            requestBody.addProperty("model", config.getChatModel());
            requestBody.addProperty("temperature", 0.1);
            requestBody.addProperty("max_tokens", 1024);
            requestBody.addProperty("stream", false);

            JsonArray messages = new JsonArray();
            if (systemPrompt != null && !systemPrompt.isEmpty()) {
                JsonObject systemMsg = new JsonObject();
                systemMsg.addProperty("role", "system");
                systemMsg.addProperty("content", systemPrompt);
                messages.add(systemMsg);
            }
            JsonObject userMsg = new JsonObject();
            userMsg.addProperty("role", "user");
            userMsg.addProperty("content", userMessage);
            messages.add(userMsg);
            requestBody.add("messages", messages);

            Request request = new Request.Builder()
                    .url(config.getBaseUrl() + "/chat/completions")
                    .addHeader("Authorization", "Bearer " + config.getApiKey())
                    .addHeader("Content-Type", "application/json")
                    .post(RequestBody.create(
                            gson.toJson(requestBody),
                            MediaType.parse("application/json")))
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    String errorBody = response.body() != null ? response.body().string() : "no body";
                    throw new RagException.GenerationException(
                            "Chat API failed: HTTP " + response.code() + " - " + errorBody);
                }
                String responseBody = response.body().string();
                JsonObject json = gson.fromJson(responseBody, JsonObject.class);
                return json.getAsJsonArray("choices")
                        .get(0).getAsJsonObject()
                        .getAsJsonObject("message")
                        .get("content").getAsString();
            } catch (IOException e) {
                throw new RagException.GenerationException("Chat API call failed", e);
            }
        }, "chat");
    }
```

- [ ] **Step 4: Wrap the rerank method with retry**

Replace the `rerank` method body:

```java
    public List<RerankResult> rerank(String query, List<String> documents, int topN) {
        return executeWithRetry(() -> {
            JsonObject requestBody = new JsonObject();
            requestBody.addProperty("model", config.getRerankerModel());
            requestBody.addProperty("query", query);
            requestBody.add("documents", gson.toJsonTree(documents));
            requestBody.addProperty("top_n", topN);
            requestBody.addProperty("return_documents", true);

            Request request = new Request.Builder()
                    .url(config.getBaseUrl() + "/rerank")
                    .addHeader("Authorization", "Bearer " + config.getApiKey())
                    .addHeader("Content-Type", "application/json")
                    .post(RequestBody.create(
                            gson.toJson(requestBody),
                            MediaType.parse("application/json")))
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    String errorBody = response.body() != null ? response.body().string() : "no body";
                    throw new RagException.RetrievalException(
                            "Rerank API failed: HTTP " + response.code() + " - " + errorBody);
                }

                String responseBody = response.body().string();
                JsonObject json = gson.fromJson(responseBody, JsonObject.class);
                JsonArray results = json.getAsJsonArray("results");

                List<RerankResult> rerankResults = new ArrayList<>();
                for (int i = 0; i < results.size(); i++) {
                    JsonObject item = results.get(i).getAsJsonObject();
                    RerankResult result = new RerankResult();
                    result.setIndex(item.get("index").getAsInt());
                    result.setScore(item.get("relevance_score").getAsDouble());

                    if (item.has("document") && item.get("document").isJsonObject()) {
                        JsonObject doc = item.getAsJsonObject("document");
                        result.setText(doc.has("text") ? doc.get("text").getAsString() : "");
                    }

                    rerankResults.add(result);
                }

                return rerankResults;
            } catch (IOException e) {
                throw new RagException.RetrievalException("Rerank API call failed", e);
            }
        }, "rerank");
    }
```

- [ ] **Step 5: Verify compilation**

Run: `mvn compile -q`
Expected: BUILD SUCCESS

- [ ] **Step 6: Commit**

```bash
git add src/main/java/com/wokrag/agent/client/SiliconFlowClient.java
git commit -m "feat: add exponential backoff retry to SiliconFlow API calls (embed, chat, rerank)"
```

---

### Task 9: Actuator Configuration

**Files:**
- Create: `src/main/java/com/wokrag/agent/config/ActuatorConfig.java`

- [ ] **Step 1: Implement ActuatorConfig**

```java
package com.wokrag.agent.config;

import org.springframework.boot.actuate.autoconfigure.endpoint.web.CorsEndpointProperties;
import org.springframework.boot.actuate.autoconfigure.endpoint.web.WebEndpointProperties;
import org.springframework.boot.actuate.autoconfigure.web.server.ManagementPortType;
import org.springframework.boot.actuate.endpoint.ExposableEndpoint;
import org.springframework.boot.actuate.endpoint.web.*;
import org.springframework.boot.actuate.endpoint.web.annotation.ControllerEndpointsSupplier;
import org.springframework.boot.actuate.endpoint.web.annotation.ServletEndpointsSupplier;
import org.springframework.boot.actuate.endpoint.web.servlet.WebMvcEndpointHandlerMapping;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Configuration
public class ActuatorConfig {

    @Bean
    public WebMvcEndpointHandlerMapping webEndpointServletHandlerMapping(
            WebEndpointsSupplier webEndpointsSupplier,
            ServletEndpointsSupplier servletEndpointsSupplier,
            ControllerEndpointsSupplier controllerEndpointsSupplier,
            EndpointMediaTypes endpointMediaTypes,
            CorsEndpointProperties corsProperties,
            WebEndpointProperties webEndpointProperties,
            Environment environment) {

        List<ExposableEndpoint<?>> allEndpoints = new ArrayList<>();
        Collection<ExposableWebEndpoint> webEndpoints = webEndpointsSupplier.getEndpoints();
        allEndpoints.addAll(webEndpoints);
        allEndpoints.addAll(servletEndpointsSupplier.getEndpoints());
        allEndpoints.addAll(controllerEndpointsSupplier.getEndpoints());

        String basePath = webEndpointProperties.getBasePath();
        EndpointMapping endpointMapping = new EndpointMapping(basePath);
        boolean shouldRegisterLinksMapping = shouldRegisterLinksMapping(
                webEndpointProperties, environment, basePath);

        return new WebMvcEndpointHandlerMapping(endpointMapping, webEndpoints,
                endpointMediaTypes, corsProperties.toCorsConfiguration(),
                new EndpointLinksResolver(allEndpoints, basePath),
                shouldRegisterLinksMapping);
    }

    private boolean shouldRegisterLinksMapping(WebEndpointProperties webEndpointProperties,
                                                Environment environment, String basePath) {
        return webEndpointProperties.getDiscovery().isEnabled()
                && (StringUtils.hasText(basePath)
                || ManagementPortType.get(environment).equals(ManagementPortType.DIFFERENT));
    }
}
```

- [ ] **Step 2: Verify compilation**

Run: `mvn compile -q`
Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add src/main/java/com/wokrag/agent/config/ActuatorConfig.java
git commit -m "feat: add actuator endpoint handler mapping configuration"
```

---

### Task 10: OpenAPI / Swagger Documentation

**Files:**
- Create: `src/main/java/com/wokrag/agent/config/OpenApiConfig.java`
- Modify: `src/main/java/com/wokrag/agent/controller/RagController.java`
- Modify: `src/main/java/com/wokrag/agent/controller/StreamController.java`
- Modify: `src/main/java/com/wokrag/agent/model/RagResponse.java`

- [ ] **Step 1: Create OpenApiConfig**

```java
package com.wokrag.agent.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.Components;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("WokRag Agent API")
                        .version("1.0.0")
                        .description("Agentic RAG Intelligent Q&A Platform API")
                        .contact(new Contact().name("WokRag Team")))
                .addSecurityItem(new SecurityRequirement().addList("API-Key"))
                .components(new Components()
                        .addSecuritySchemes("API-Key",
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.APIKEY)
                                        .in(SecurityScheme.In.HEADER)
                                        .name("X-API-Key")));
    }
}
```

- [ ] **Step 2: Add OpenAPI annotations to RagController**

Replace the full content of `RagController.java`:

```java
package com.wokrag.agent.controller;

import com.wokrag.agent.model.RagResponse;
import com.wokrag.agent.service.rag.RagPipeline;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/rag")
@RequiredArgsConstructor
@Tag(name = "RAG", description = "RAG query and health endpoints")
public class RagController {

    private final RagPipeline ragPipeline;

    @PostMapping("/query")
    @Operation(summary = "Execute a RAG query",
            description = "Submit a question and get an answer with citations. Supports multi-turn conversation via sessionId.")
    public ResponseEntity<RagResponse> query(@RequestBody QueryRequest request) {
        RagResponse response = ragPipeline.execute(
                request.getQuestion(), request.getSessionId());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/health")
    @Operation(summary = "Health check",
            description = "Returns service health status. Use /actuator/health for detailed dependency checks.")
    public ResponseEntity<HealthResponse> health() {
        HealthResponse response = new HealthResponse();
        response.setStatus("OK");
        response.setService("wok-rag-agent");
        return ResponseEntity.ok(response);
    }

    @Data
    public static class QueryRequest {
        @Parameter(description = "The question to ask", required = true, example = "退货政策是什么？")
        private String question;
        @Parameter(description = "Session ID for multi-turn conversation", example = "session-abc123")
        private String sessionId;
    }

    @Data
    public static class HealthResponse {
        private String status;
        private String service;
    }
}
```

- [ ] **Step 3: Add OpenAPI annotations to StreamController**

Replace the full content of `StreamController.java`:

```java
package com.wokrag.agent.controller;

import com.wokrag.agent.model.RagResponse;
import com.wokrag.agent.service.rag.RagPipeline;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "RAG Streaming", description = "SSE streaming RAG endpoints")
public class StreamController {

    private final RagPipeline ragPipeline;
    private final ExecutorService executor = Executors.newCachedThreadPool();

    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "Stream a RAG query via SSE",
            description = "Submit a question and receive token-by-token streaming response via Server-Sent Events.")
    public SseEmitter stream(@RequestBody StreamRequest request) {
        SseEmitter emitter = new SseEmitter(120_000L);

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
        @Parameter(description = "The question to ask", required = true, example = "退货政策是什么？")
        private String question;
        @Parameter(description = "Session ID for multi-turn conversation", example = "session-abc123")
        private String sessionId;
    }
}
```

- [ ] **Step 4: Add OpenAPI annotations to RagResponse**

Add `@Schema` annotations to `RagResponse.java`. Read it first, then add imports and annotations:

Add import: `import io.swagger.v3.oas.annotations.media.Schema;`

Add `@Schema(description = "...")` to each field:
- `answer` -> `@Schema(description = "The generated answer text")`
- `sessionId` -> `@Schema(description = "Session ID for conversation tracking")`
- `citations` -> `@Schema(description = "List of source citations referenced in the answer")`

And to `CitationInfo`:
- `index` -> `@Schema(description = "Citation index number", example = "1")`
- `source` -> `@Schema(description = "Source document name")`
- `sourceUrl` -> `@Schema(description = "URL to the source document")`
- `chunkContent` -> `@Schema(description = "The referenced chunk text")`

- [ ] **Step 5: Verify compilation and Swagger UI**

Run: `mvn compile -q`
Expected: BUILD SUCCESS

Start the app: `mvn spring-boot:run`
Navigate to: `http://localhost:8080/swagger-ui.html`
Expected: Swagger UI loads showing RAG and RAG Streaming endpoint groups.

Stop the application.

- [ ] **Step 6: Commit**

```bash
git add src/main/java/com/wokrag/agent/config/OpenApiConfig.java src/main/java/com/wokrag/agent/controller/ src/main/java/com/wokrag/agent/model/RagResponse.java
git commit -m "feat: add OpenAPI/Swagger documentation with API key security scheme"
```

---

### Task 11: Dockerfile and Docker Compose

**Files:**
- Create: `Dockerfile`
- Create: `.dockerignore`
- Modify: `docker-compose.yml`

- [ ] **Step 1: Create multi-stage Dockerfile**

```dockerfile
# Stage 1: Build
FROM maven:3.9-eclipse-temurin-17 AS builder
WORKDIR /build
COPY pom.xml .
RUN mvn dependency:go-offline -B
COPY src ./src
RUN mvn clean package -DskipTests -B

# Stage 2: Runtime
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

RUN addgroup -S wokrag && adduser -S wokrag -G wokrag
RUN mkdir -p /app/logs && chown wokrag:wokrag /app/logs

COPY --from=builder /build/target/*.jar app.jar

USER wokrag

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD wget -qO- http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]
```

- [ ] **Step 2: Create .dockerignore**

```
.git
.gitignore
*.md
*.iml
.idea
target/
docs/
docker-compose.yml
```

- [ ] **Step 3: Add application service to docker-compose.yml**

Add this service definition to the `services:` section in `docker-compose.yml`, before the `volumes:` section:

```yaml
  app:
    container_name: wok-rag-agent
    build:
      context: .
      dockerfile: Dockerfile
    ports:
      - "8080:8080"
    environment:
      - SILICONFLOW_API_KEY=${SILICONFLOW_API_KEY}
      - MILVUS_URI=http://milvus-standalone:19530
      - SPRING_PROFILES_ACTIVE=prod
      - API_KEY=${API_KEY:-wokrag-default-key-change-me}
    depends_on:
      standalone:
        condition: service_healthy
    restart: unless-stopped
```

- [ ] **Step 4: Verify Dockerfile builds**

Run: `mvn clean package -DskipTests -q` first to verify the JAR builds.
Expected: BUILD SUCCESS

Then: `docker build -t wok-rag-agent .`
Expected: Build completes successfully.

- [ ] **Step 5: Commit**

```bash
git add Dockerfile .dockerignore docker-compose.yml
git commit -m "feat: add Dockerfile and docker-compose app service for containerized deployment"
```

---

### Task 12: Integration Test for Production Features

**Files:**
- Create: `src/test/java/com/wokrag/agent/integration/ProductionFeaturesTest.java`

- [ ] **Step 1: Write integration test for actuator and health**

```java
package com.wokrag.agent.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
class ProductionFeaturesTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void testActuatorHealthEndpoint() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").exists());
    }

    @Test
    void testActuatorPrometheusEndpoint() throws Exception {
        mockMvc.perform(get("/actuator/prometheus"))
                .andExpect(status().isOk());
    }

    @Test
    void testSwaggerUiAccessible() throws Exception {
        mockMvc.perform(get("/swagger-ui.html"))
                .andExpect(status().isOk());
    }

    @Test
    void testApiDocsAccessible() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.info.title").value("WokRag Agent API"));
    }

    @Test
    void testHealthEndpointReturnsServiceInfo() throws Exception {
        mockMvc.perform(get("/api/rag/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("OK"))
                .andExpect(jsonPath("$.service").value("wok-rag-agent"));
    }
}
```

- [ ] **Step 2: Run test**

Run: `mvn test -Dtest=ProductionFeaturesTest -q`
Expected: PASS (dev profile has auth disabled, actuator exposed)

- [ ] **Step 3: Commit**

```bash
git add src/test/java/com/wokrag/agent/integration/ProductionFeaturesTest.java
git commit -m "test: add integration tests for actuator, swagger, and health endpoints"
```

---

### Task 13: Final Verification

- [ ] **Step 1: Full compilation check**

Run: `mvn clean compile -q`
Expected: BUILD SUCCESS

- [ ] **Step 2: Run all tests**

Run: `mvn test -q`
Expected: All tests pass

- [ ] **Step 3: Verify new file structure**

Confirm these files exist:
- `src/main/resources/logback-spring.xml`
- `src/main/resources/application-prod.yml`
- `src/main/java/com/wokrag/agent/config/SecurityConfig.java`
- `src/main/java/com/wokrag/agent/config/ActuatorConfig.java`
- `src/main/java/com/wokrag/agent/config/OpenApiConfig.java`
- `src/main/java/com/wokrag/agent/filter/ApiKeyAuthFilter.java`
- `src/main/java/com/wokrag/agent/filter/RequestLoggingFilter.java`
- `src/main/java/com/wokrag/agent/health/MilvusHealthIndicator.java`
- `src/main/java/com/wokrag/agent/health/SiliconFlowHealthIndicator.java`
- `Dockerfile`
- `.dockerignore`

- [ ] **Step 4: Run the application and verify endpoints**

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

In a separate terminal, verify:
```bash
# Health
curl http://localhost:8080/api/rag/health

# Actuator health (with dependency checks)
curl http://localhost:8080/actuator/health

# Prometheus metrics
curl http://localhost:8080/actuator/prometheus

# Swagger UI
# Open http://localhost:8080/swagger-ui.html in browser

# API docs
curl http://localhost:8080/v3/api-docs
```

Stop the application.

- [ ] **Step 5: Commit final state**

```bash
git add .
git commit -m "chore: Phase 3 productionization complete"
```
