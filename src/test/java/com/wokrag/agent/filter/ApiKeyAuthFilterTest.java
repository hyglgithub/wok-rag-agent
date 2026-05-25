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

    @Test
    void testOptionsPreflightBypassesAuth() throws ServletException, IOException {
        apiKeyConfig.setEnabled(true);
        request.setRequestURI("/api/rag/sessions/test-session");
        request.setMethod("OPTIONS");
        filter.doFilter(request, response, filterChain);
        assertEquals(200, response.getStatus());
    }
}
