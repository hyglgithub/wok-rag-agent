package com.wokrag.agent.service.generation;

import com.wokrag.agent.model.Chunk;
import com.wokrag.agent.model.RagResponse;

import java.util.List;

public interface LlmService {
    String generate(String systemPrompt, String userMessage);
    RagResponse generateWithCitations(List<Chunk> chunks, String question);
}
