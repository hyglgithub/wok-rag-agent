package com.wokrag.agent.service.generation;

import com.wokrag.agent.client.SiliconFlowClient;
import com.wokrag.agent.model.Chunk;
import com.wokrag.agent.model.RagResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class LlmServiceImpl implements LlmService {

    private final SiliconFlowClient client;
    private final PromptService promptService = new PromptService();

    @Override
    public String generate(String systemPrompt, String userMessage) {
        return client.chat(systemPrompt, userMessage);
    }

    @Override
    public RagResponse generateWithCitations(List<Chunk> chunks, String question) {
        String systemPrompt = promptService.getSystemPrompt();
        String userPrompt = promptService.buildUserPrompt(chunks, question);

        String answer = generate(systemPrompt, userPrompt);

        List<RagResponse.CitationInfo> citations = parseCitations(answer, chunks);

        RagResponse response = new RagResponse();
        response.setAnswer(answer);
        response.setCitations(citations);

        return response;
    }

    private List<RagResponse.CitationInfo> parseCitations(String answer, List<Chunk> chunks) {
        List<RagResponse.CitationInfo> citations = new ArrayList<>();
        java.util.Set<Integer> addedIndices = new java.util.HashSet<>();
        Pattern pattern = Pattern.compile("\\[(\\d+)]");
        Matcher matcher = pattern.matcher(answer);

        while (matcher.find()) {
            int index = Integer.parseInt(matcher.group(1));
            if (index >= 1 && index <= chunks.size() && !addedIndices.contains(index)) {
                Chunk chunk = chunks.get(index - 1);
                RagResponse.CitationInfo citation = new RagResponse.CitationInfo();
                citation.setIndex(index);
                citation.setSource(chunk.getSource());
                citation.setSourceUrl(chunk.getSourceUrl());
                citation.setChunkContent(chunk.getContent());
                citations.add(citation);
                addedIndices.add(index);
            }
        }

        return citations;
    }
}
