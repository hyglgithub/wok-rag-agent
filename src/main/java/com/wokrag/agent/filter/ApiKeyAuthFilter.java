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

        // Debug: log all headers for troubleshooting
        if (log.isDebugEnabled()) {
            log.debug("Request headers for path: {}", path);
            java.util.Enumeration<String> headerNames = request.getHeaderNames();
            while (headerNames.hasMoreElements()) {
                String headerName = headerNames.nextElement();
                log.debug("  Header: {} = {}", headerName, request.getHeader(headerName));
            }
        }
        
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
        // CORS preflight requests always pass through
        if ("OPTIONS".equalsIgnoreCase(method)) {
            return true;
        }
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
