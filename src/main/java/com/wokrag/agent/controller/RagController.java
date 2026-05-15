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
