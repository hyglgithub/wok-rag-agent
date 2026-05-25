package com.wokrag.agent.filter;

import com.wokrag.agent.config.ApiKeyConfig;
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
    private ApiKeyConfig apiKeyConfig;

    @BeforeEach
    void setUp() {
        apiKeyConfig = new ApiKeyConfig();
        filter = new ApiKeyAuthFilter(apiKeyConfig);
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        filterChain = (req, res) -> {};
    }

    @Test
    void testDisabledFilterPassesThrough() throws ServletException, IOException {
        apiKeyConfig.setEnabled(false);
        apiKeyConfig.setKey("test-key");

        request.setRequestURI("/api/rag/query");
        filter.doFilter(request, response, filterChain);

        assertEquals(200, response.getStatus());
    }

    @Test
    void testHealthEndpointBypassesAuth() throws ServletException, IOException {
        apiKeyConfig.setEnabled(true);
        apiKeyConfig.setKey("test-key");

        request.setRequestURI("/actuator/health");
        filter.doFilter(request, response, filterChain);

        assertEquals(200, response.getStatus());
    }

    @Test
    void testSwaggerEndpointBypassesAuth() throws ServletException, IOException {
        apiKeyConfig.setEnabled(true);
        apiKeyConfig.setKey("test-key");

        request.setRequestURI("/swagger-ui.html");
        filter.doFilter(request, response, filterChain);

        assertEquals(200, response.getStatus());
    }

    @Test
    void testMissingApiKeyReturns401() throws ServletException, IOException {
        apiKeyConfig.setEnabled(true);
        apiKeyConfig.setKey("test-key");

        request.setRequestURI("/api/rag/query");
        filter.doFilter(request, response, filterChain);

        assertEquals(401, response.getStatus());
        assertTrue(response.getContentAsString().contains("Missing API key"));
    }

    @Test
    void testWrongApiKeyReturns403() throws ServletException, IOException {
        apiKeyConfig.setEnabled(true);
        apiKeyConfig.setKey("correct-key");

        request.setRequestURI("/api/rag/query");
        request.addHeader("X-API-Key", "wrong-key");
        filter.doFilter(request, response, filterChain);

        assertEquals(403, response.getStatus());
        assertTrue(response.getContentAsString().contains("Invalid API key"));
    }

    @Test
    void testCorrectApiKeyPassesThrough() throws ServletException, IOException {
        apiKeyConfig.setEnabled(true);
        apiKeyConfig.setKey("correct-key");

        request.setRequestURI("/api/rag/query");
        request.addHeader("X-API-Key", "correct-key");
        filter.doFilter(request, response, filterChain);

        assertEquals(200, response.getStatus());
    }
}
