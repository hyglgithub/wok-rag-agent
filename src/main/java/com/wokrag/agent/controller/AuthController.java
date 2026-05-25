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
