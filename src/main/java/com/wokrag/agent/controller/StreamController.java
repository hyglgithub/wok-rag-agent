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
