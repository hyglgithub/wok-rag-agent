package com.wokrag.agent.controller;

import com.wokrag.agent.config.ApiKeyConfig;
import com.wokrag.agent.service.AuthTokenService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@Import(ApiKeyConfig.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuthTokenService tokenService;

    @Test
    void testLoginWithValidToken() throws Exception {
        when(tokenService.validate("valid-token")).thenReturn(true);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\": \"valid-token\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("OK"));
    }

    @Test
    void testLoginWithInvalidToken() throws Exception {
        when(tokenService.validate(anyString())).thenReturn(false);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\": \"wrong-token\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("INVALID_TOKEN"))
                .andExpect(jsonPath("$.errorMessage").value("Invalid token"));
    }

    @Test
    void testStatusWithValidHeader() throws Exception {
        when(tokenService.validate("valid-key")).thenReturn(true);

        mockMvc.perform(get("/api/auth/status")
                        .header("X-API-Key", "valid-key"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.authenticated").value(true));
    }

    @Test
    void testStatusWithInvalidHeader() throws Exception {
        when(tokenService.validate("wrong")).thenReturn(false);

        mockMvc.perform(get("/api/auth/status")
                        .header("X-API-Key", "wrong"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.errorMessage").value("Invalid or missing API key"));
    }

    @Test
    void testStatusWithoutHeader() throws Exception {
        when(tokenService.validate(any())).thenReturn(false);

        mockMvc.perform(get("/api/auth/status"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("UNAUTHORIZED"));
    }
}
