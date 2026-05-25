package com.wokrag.agent.filter;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.wokrag.agent.config.ApiKeyConfig;
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
    private final Gson gson = new Gson();

    private static final String AUTH_HEADER = "X-API-Key";
    private static final Set<String> BYPASS_PATHS = Set.of(
            "/actuator/health",
            "/actuator/info",
            "/actuator/prometheus",
            "/swagger-ui.html",
            "/v3/api-docs"
    );

    public ApiKeyAuthFilter(ApiKeyConfig apiKeyConfig) {
        this.apiKeyConfig = apiKeyConfig;
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

        if (!apiKeyConfig.getKey().equals(providedKey)) {
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
