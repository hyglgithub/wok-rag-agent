package com.wokrag.agent.controller;

import com.wokrag.agent.model.RagResponse;
import com.wokrag.agent.service.rag.RagPipeline;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(RagController.class)
class RagControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private RagPipeline ragPipeline;

    @Test
    void testQueryEndpoint() throws Exception {
        RagResponse mockResponse = new RagResponse();
        mockResponse.setAnswer("Test answer");
        mockResponse.setCitations(List.of());

        when(ragPipeline.execute(anyString(), any())).thenReturn(mockResponse);

        mockMvc.perform(post("/api/rag/query")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"question\": \"Test question\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.answer").value("Test answer"));
    }

    @Test
    void testQueryEndpointWithSessionId() throws Exception {
        RagResponse mockResponse = new RagResponse();
        mockResponse.setAnswer("Test answer");
        mockResponse.setSessionId("session-1");
        mockResponse.setCitations(List.of());

        when(ragPipeline.execute(anyString(), any())).thenReturn(mockResponse);

        mockMvc.perform(post("/api/rag/query")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"question\": \"Test question\", \"sessionId\": \"session-1\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.answer").value("Test answer"))
                .andExpect(jsonPath("$.sessionId").value("session-1"));
    }
}
