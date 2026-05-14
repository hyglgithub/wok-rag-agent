package com.wokrag.agent.service.rag;

import com.wokrag.agent.client.SiliconFlowClient;
import com.wokrag.agent.exception.RagException;
import com.wokrag.agent.model.ChatMessage;
import com.wokrag.agent.model.Chunk;
import com.wokrag.agent.model.RagResponse;
import com.wokrag.agent.model.SearchResult;
import com.wokrag.agent.service.embedding.EmbeddingService;
import com.wokrag.agent.service.generation.LlmService;
import com.wokrag.agent.service.generation.PromptService;
import com.wokrag.agent.service.memory.SessionMemoryService;
import com.wokrag.agent.service.retrieval.HybridSearchService;
import com.wokrag.agent.service.rewrite.QueryRewriter;
import com.wokrag.agent.service.tool.FunctionCallService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class RagPipeline {

    private final EmbeddingService embeddingService;
    private final HybridSearchService hybridSearchService;
    private final LlmService llmService;
    private final SessionMemoryService sessionMemoryService;
    private final QueryRewriter queryRewriter;
    private final FunctionCallService functionCallService;
    private final SiliconFlowClient siliconFlowClient;
    private final PromptService promptService;

    public RagResponse execute(String question) {
        return execute(question, null);
    }

    public RagResponse execute(String question, String sessionId) {
        log.info("Executing RAG pipeline for question: {}, sessionId: {}", question, sessionId);

        try {
            // Step 1: Load session history
            List<ChatMessage> history = (sessionId != null)
                    ? sessionMemoryService.getMessages(sessionId)
                    : List.of();

            // Step 2: Rewrite query using history context
            String rewrittenQuery = queryRewriter.rewrite(history, question);
            log.debug("Rewritten query: {}", rewrittenQuery);

            // Step 3: Embedding + Retrieval
            double[] queryVector = embeddingService.embed(rewrittenQuery);
            List<SearchResult> searchResults = hybridSearchService.hybridSearch(
                    queryVector, rewrittenQuery);

            List<Chunk> chunks = new ArrayList<>();
            if (!searchResults.isEmpty()) {
                chunks = convertToChunks(searchResults);
            }

            // Step 4: Generate answer with Function Call support
            String systemPrompt = promptService.getSystemPrompt();
            String summary = sessionMemoryService.getSummary(sessionId);
            if (!history.isEmpty() || (summary != null && !summary.isEmpty())) {
                systemPrompt = buildSystemPromptWithHistory(history, systemPrompt, summary);
            }

            String answer;
            if (!chunks.isEmpty()) {
                // Use RAG generation with retrieved context
                String userPrompt = promptService.buildUserPrompt(chunks, question);
                answer = functionCallService.chatWithTools(systemPrompt, userPrompt);
            } else {
                // No retrieval results, try tool calling directly
                answer = functionCallService.chatWithTools(systemPrompt, question);
            }

            // Step 5: Save to session memory
            if (sessionId != null) {
                sessionMemoryService.addMessage(sessionId, "user", question);
                sessionMemoryService.addMessage(sessionId, "assistant", answer);
            }

            // Build response
            RagResponse response = new RagResponse();
            response.setAnswer(answer);
            response.setSessionId(sessionId);
            if (!chunks.isEmpty()) {
                response.setCitations(parseCitations(answer, chunks));
            } else {
                response.setCitations(new ArrayList<>());
            }

            log.info("RAG pipeline completed successfully");
            return response;

        } catch (RagException e) {
            log.error("RAG pipeline failed", e);
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error in RAG pipeline", e);
            throw new RagException.GenerationException("RAG pipeline failed", e);
        }
    }

    /**
     * Execute RAG without tool calling (used by SearchKnowledgeBaseTool to avoid recursion).
     */
    public RagResponse executeWithoutTools(String question) {
        log.info("Executing RAG pipeline without tools for: {}", question);

        try {
            double[] queryVector = embeddingService.embed(question);
            List<SearchResult> searchResults = hybridSearchService.hybridSearch(
                    queryVector, question);

            if (searchResults.isEmpty()) {
                RagResponse response = new RagResponse();
                response.setAnswer("未找到相关信息。");
                response.setCitations(new ArrayList<>());
                return response;
            }

            List<Chunk> chunks = convertToChunks(searchResults);
            RagResponse response = llmService.generateWithCitations(chunks, question);
            return response;

        } catch (RagException e) {
            throw e;
        } catch (Exception e) {
            throw new RagException.GenerationException("RAG pipeline failed", e);
        }
    }

    /**
     * Streaming execution with token-by-token callback.
     */
    public void executeStreaming(String question, String sessionId,
                                  StreamCallback callback) {
        log.info("Executing streaming RAG pipeline for: {}", question);

        try {
            List<ChatMessage> history = (sessionId != null)
                    ? sessionMemoryService.getMessages(sessionId)
                    : List.of();

            String rewrittenQuery = queryRewriter.rewrite(history, question);

            double[] queryVector = embeddingService.embed(rewrittenQuery);
            List<SearchResult> searchResults = hybridSearchService.hybridSearch(
                    queryVector, rewrittenQuery);

            List<Chunk> chunks = convertToChunks(searchResults);

            String systemPrompt = promptService.getSystemPrompt();
            String summary = sessionMemoryService.getSummary(sessionId);
            if (!history.isEmpty() || (summary != null && !summary.isEmpty())) {
                systemPrompt = buildSystemPromptWithHistory(history, systemPrompt, summary);
            }

            String userPrompt;
            if (!chunks.isEmpty()) {
                userPrompt = promptService.buildUserPrompt(chunks, question);
            } else {
                userPrompt = question;
            }

            // Stream the response
            siliconFlowClient.streamChat(systemPrompt, userPrompt,
                    new SiliconFlowClient.StreamCallback() {
                        StringBuilder fullAnswer = new StringBuilder();

                        @Override
                        public void onToken(String token) {
                            fullAnswer.append(token);
                            callback.onToken(token);
                        }

                        @Override
                        public void onComplete(String fullContent,
                                               int promptTokens, int completionTokens) {
                            // Save to session memory
                            if (sessionId != null) {
                                sessionMemoryService.addMessage(sessionId, "user", question);
                                sessionMemoryService.addMessage(sessionId, "assistant", fullContent);
                            }

                            RagResponse response = new RagResponse();
                            response.setAnswer(fullContent);
                            response.setSessionId(sessionId);
                            if (!chunks.isEmpty()) {
                                response.setCitations(parseCitations(fullContent, chunks));
                            } else {
                                response.setCitations(new ArrayList<>());
                            }
                            callback.onComplete(response);
                        }

                        @Override
                        public void onError(Exception e, String partialContent) {
                            callback.onError(e);
                        }
                    });

        } catch (Exception e) {
            log.error("Streaming RAG pipeline failed", e);
            callback.onError(e);
        }
    }

    private String buildSystemPromptWithHistory(List<ChatMessage> history,
                                                  String baseSystemPrompt,
                                                  String summary) {
        StringBuilder sb = new StringBuilder(baseSystemPrompt);
        if (summary != null && !summary.isEmpty()) {
            sb.append("\n\n【对话背景摘要】\n").append(summary);
        }
        if (!history.isEmpty()) {
            sb.append("\n\n【近期对话】\n");
            for (ChatMessage msg : history) {
                String roleName = "user".equals(msg.getRole()) ? "用户" : "助手";
                sb.append(roleName).append("：").append(msg.getContent()).append("\n");
            }
        }
        return sb.toString();
    }

    private List<Chunk> convertToChunks(List<SearchResult> searchResults) {
        List<Chunk> chunks = new ArrayList<>();
        for (SearchResult result : searchResults) {
            Chunk chunk = new Chunk();
            chunk.setId(result.getChunkId());
            chunk.setContent(result.getContent());
            chunk.setSource(result.getMetadata().getOrDefault("source", "unknown"));
            chunk.setSourceUrl(result.getMetadata().getOrDefault("source_url", ""));
            chunk.setUpdateTime(java.time.LocalDate.now().toString());
            chunk.setMetadata(result.getMetadata());
            chunks.add(chunk);
        }
        return chunks;
    }

    private List<RagResponse.CitationInfo> parseCitations(String answer, List<Chunk> chunks) {
        List<RagResponse.CitationInfo> citations = new ArrayList<>();
        java.util.Set<Integer> addedIndices = new java.util.HashSet<>();
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("\\[(\\d+)]");
        java.util.regex.Matcher matcher = pattern.matcher(answer);

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

    public interface StreamCallback {
        void onToken(String token);
        void onComplete(RagResponse response);
        void onError(Exception e);
    }
}
