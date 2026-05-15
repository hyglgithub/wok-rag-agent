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
