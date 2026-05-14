package com.wokrag.agent.controller;

import com.wokrag.agent.model.RagResponse;
import com.wokrag.agent.service.rag.RagPipeline;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/rag")
@RequiredArgsConstructor
public class RagController {

    private final RagPipeline ragPipeline;

    @PostMapping("/query")
    public ResponseEntity<RagResponse> query(@RequestBody QueryRequest request) {
        RagResponse response = ragPipeline.execute(
                request.getQuestion(), request.getSessionId());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/health")
    public ResponseEntity<HealthResponse> health() {
        HealthResponse response = new HealthResponse();
        response.setStatus("OK");
        response.setService("wok-rag-agent");
        return ResponseEntity.ok(response);
    }

    @Data
    public static class QueryRequest {
        private String question;
        private String sessionId;
    }

    @Data
    public static class HealthResponse {
        private String status;
        private String service;
    }
}
