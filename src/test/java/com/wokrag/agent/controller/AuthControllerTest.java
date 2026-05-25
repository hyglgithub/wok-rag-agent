package com.wokrag.agent.controller;

import com.wokrag.agent.service.AuthTokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AuthControllerTest {

    private MockMvc mockMvc;
    private AuthTokenService tokenService;

    @BeforeEach
    void setUp() throws Exception {
        tokenService = new AuthTokenService();
        tokenService.setTokenFilePath("data/test-auth-token.txt");
        tokenService.init();
        AuthController controller = new AuthController(tokenService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void testLoginWithValidToken() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\": \"" + tokenService.getToken() + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("OK"));
    }

    @Test
    void testLoginWithInvalidToken() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\": \"wrong-token\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void testStatusWithValidHeader() throws Exception {
        mockMvc.perform(get("/api/auth/status")
                        .header("X-API-Key", tokenService.getToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.authenticated").value(true));
    }

    @Test
    void testStatusWithInvalidHeader() throws Exception {
        mockMvc.perform(get("/api/auth/status")
                        .header("X-API-Key", "wrong"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void testStatusWithoutHeader() throws Exception {
        mockMvc.perform(get("/api/auth/status"))
                .andExpect(status().isUnauthorized());
    }
}
