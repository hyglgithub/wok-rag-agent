package com.wokrag.agent.service.generation;

import com.wokrag.agent.model.Chunk;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PromptService {

    private static final String SYSTEM_PROMPT = """
            # Role and Boundaries
            You are a professional knowledge base Q&A assistant. Your task is to answer [User Question] based solely on [Reference Materials].

            # Answer Rules
            1. Only use information from the reference materials for statements; do not use your pre-trained knowledge to fill in details.
            2. If the reference materials are insufficient to support a conclusion, ask 1-2 clarification questions first; if clarification is not possible, use a fallback response.
            3. Do not fabricate any information not mentioned in the reference materials, including numbers, dates, amounts, etc.
            4. If multiple reference materials contain conflicting information, point out the conflict and inform the user that the most recent material takes precedence.

            # Citation Rules
            1. Place citation numbers immediately after key facts, e.g.: ......[1]
            2. Citations must be able to "point to the chunk that supports the statement"
            3. Only cite reference materials you actually used

            # Output Format
            - Use Markdown output
            - Provide "Conclusion" first, then "Supporting Evidence and Explanation"
            - Default 120-200 words; if listing points, maximum 5 points
            - If materials involve conditions/exclusions, they must be covered

            # Fallback Response (when unable to answer from materials and clarification is not possible)
            Sorry, I did not find supporting evidence in the knowledge base for this question. You can:
            1. Try rephrasing the question or adding key information
            2. Contact human customer service for assistance
            """;

    public String getSystemPrompt() {
        return SYSTEM_PROMPT;
    }

    public String buildUserPrompt(List<Chunk> chunks, String question) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("# Reference Materials\n\n");

        for (int i = 0; i < chunks.size(); i++) {
            Chunk chunk = chunks.get(i);
            prompt.append(String.format("[%d] Source: %s | Updated: %s\n",
                    i + 1, chunk.getSource(), chunk.getUpdateTime()));
            prompt.append(chunk.getContent()).append("\n\n");
        }

        prompt.append("---\n\n");
        prompt.append("# User Question\n");
        prompt.append(question);

        return prompt.toString();
    }
}
