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
import com.wokrag.agent.service.intent.IntentClassifier;
import com.wokrag.agent.service.memory.SessionMemoryService;
import com.wokrag.agent.service.retrieval.HybridSearchService;
import com.wokrag.agent.service.rewrite.QueryRewriter;
import com.wokrag.agent.service.tool.FunctionCallService;
import com.wokrag.agent.repository.SessionRepository;
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
    private final IntentClassifier intentClassifier;
    private final SessionRepository sessionRepository;

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

            // Step 2: Intent classification (after history, before rewrite)
            IntentClassifier.IntentResult intentResult = intentClassifier.classify(history, question);
            log.info("Detected intent: {} (confidence={})", intentResult.getIntent(), intentResult.getConfidence());

            // Step 3: Route based on intent
            switch (intentResult.getIntent()) {
                case IntentClassifier.INTENT_TOOL:
                    return handleToolIntent(question, sessionId, history);
                case IntentClassifier.INTENT_CHITCHAT:
                    return handleChitchatIntent(question, sessionId, history, intentResult);
                case IntentClassifier.INTENT_CLARIFICATION:
                    return handleClarificationIntent(question, sessionId, intentResult);
                default:
                    return handleKnowledgeIntent(question, sessionId, history);
            }

        } catch (RagException e) {
            log.error("RAG pipeline failed", e);
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error in RAG pipeline", e);
            throw new RagException.GenerationException("RAG pipeline failed", e);
        }
    }

    private RagResponse handleKnowledgeIntent(String question, String sessionId,
                                                List<ChatMessage> history) {
        String rewrittenQuery = queryRewriter.rewrite(history, question);
        log.debug("Rewritten query: {}", rewrittenQuery);

        double[] queryVector = embeddingService.embed(rewrittenQuery);
        List<SearchResult> searchResults = hybridSearchService.hybridSearch(
                queryVector, rewrittenQuery);

        List<Chunk> chunks = new ArrayList<>();
        if (!searchResults.isEmpty()) {
            chunks = convertToChunks(searchResults);
        }

        String systemPrompt = promptService.getSystemPrompt();
        String summary = sessionMemoryService.getSummary(sessionId);
        if (!history.isEmpty() || (summary != null && !summary.isEmpty())) {
            systemPrompt = buildSystemPromptWithHistory(history, systemPrompt, summary);
        }

        String answer;
        if (!chunks.isEmpty()) {
            String userPrompt = promptService.buildUserPrompt(chunks, question);
            answer = functionCallService.chatWithTools(systemPrompt, userPrompt);
        } else {
            answer = functionCallService.chatWithTools(systemPrompt, question);
        }

        if (sessionId != null) {
            sessionMemoryService.addMessage(sessionId, "user", question);
            sessionMemoryService.addMessage(sessionId, "assistant", answer);
        }

        RagResponse response = new RagResponse();
        response.setAnswer(answer);
        response.setSessionId(sessionId);
        if (!chunks.isEmpty()) {
            response.setCitations(parseCitations(answer, chunks));
        } else {
            response.setCitations(new ArrayList<>());
        }

        persistToSqlite(sessionId, question, answer, response.getCitations());
        log.info("Knowledge intent completed successfully");
        return response;
    }

    private RagResponse handleToolIntent(String question, String sessionId,
                                         List<ChatMessage> history) {
        String systemPrompt = promptService.getSystemPrompt();
        String summary = sessionMemoryService.getSummary(sessionId);
        if (!history.isEmpty() || (summary != null && !summary.isEmpty())) {
            systemPrompt = buildSystemPromptWithHistory(history, systemPrompt, summary);
        }

        String answer = functionCallService.chatWithTools(systemPrompt, question);

        if (sessionId != null) {
            sessionMemoryService.addMessage(sessionId, "user", question);
            sessionMemoryService.addMessage(sessionId, "assistant", answer);
        }

        RagResponse response = new RagResponse();
        response.setAnswer(answer);
        response.setSessionId(sessionId);
        response.setCitations(new ArrayList<>());

        persistToSqlite(sessionId, question, answer, response.getCitations());
        log.info("Tool intent completed successfully");
        return response;
    }



    private RagResponse handleChitchatIntent(String question, String sessionId,
                                               List<ChatMessage> history,
                                               IntentClassifier.IntentResult intentResult) {
        String answer;
        if (intentResult.getReply() != null && !intentResult.getReply().isEmpty()) {
            answer = intentResult.getReply();
        } else {
            if (question.contains("你好") || question.contains("您好")) {
                answer = "您好！请问有什么可以帮您的？";
            } else if (question.contains("谢谢") || question.contains("感谢")) {
                answer = "不客气，还有其他问题随时问我。";
            } else {
                answer = "好的，如果您有任何问题，随时告诉我。";
            }
        }

        if (sessionId != null) {
            sessionMemoryService.addMessage(sessionId, "user", question);
            sessionMemoryService.addMessage(sessionId, "assistant", answer);
        }

        RagResponse response = new RagResponse();
        response.setAnswer(answer);
        response.setSessionId(sessionId);
        response.setCitations(new ArrayList<>());

        persistToSqlite(sessionId, question, answer, response.getCitations());
        log.info("Chitchat intent completed");
        return response;
    }

    private RagResponse handleClarificationIntent(String question, String sessionId,
                                                    IntentClassifier.IntentResult intentResult) {
        String answer;
        if (intentResult.getReply() != null && !intentResult.getReply().isEmpty()) {
            answer = intentResult.getReply();
        } else {
            answer = "您的问题我还不太明确，能否告诉我您想了解哪方面的信息？"
                    + "比如：产品信息、订单查询、退换货政策等。";
        }

        RagResponse response = new RagResponse();
        response.setAnswer(answer);
        response.setSessionId(sessionId);
        response.setCitations(new ArrayList<>());

        persistToSqlite(sessionId, question, answer, response.getCitations());
        log.info("Clarification intent, returned guiding prompt");
        return response;
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
     * Streaming execution with token-by-token callback and intent routing.
     */
    public void executeStreaming(String question, String sessionId,
                                  StreamCallback callback) {
        log.info("Executing streaming RAG pipeline for: {}", question);

        try {
            // Step 1: Load session history
            List<ChatMessage> history = (sessionId != null)
                    ? sessionMemoryService.getMessages(sessionId)
                    : List.of();

            // Step 2: Intent classification
            IntentClassifier.IntentResult intentResult = intentClassifier.classify(history, question);
            log.info("Streaming - detected intent: {} (confidence={})",
                    intentResult.getIntent(), intentResult.getConfidence());

            // Step 3: Route based on intent
            switch (intentResult.getIntent()) {
                case IntentClassifier.INTENT_CHITCHAT:
                    handleChitchatIntentStreaming(question, sessionId, history, intentResult, callback);
                    break;
                case IntentClassifier.INTENT_CLARIFICATION:
                    handleClarificationIntentStreaming(question, sessionId, intentResult, callback);
                    break;
                case IntentClassifier.INTENT_TOOL:
                    handleToolIntentStreaming(question, sessionId, history, callback);
                    break;
                default:
                    handleKnowledgeIntentStreaming(question, sessionId, history, callback);
            }

        } catch (Exception e) {
            log.error("Streaming RAG pipeline failed", e);
            callback.onError(e);
        }
    }

    private void handleKnowledgeIntentStreaming(String question, String sessionId,
                                                 List<ChatMessage> history,
                                                 StreamCallback callback) {
        String rewrittenQuery = queryRewriter.rewrite(history, question);
        log.debug("Rewritten query: {}", rewrittenQuery);

        double[] queryVector = embeddingService.embed(rewrittenQuery);
        List<SearchResult> searchResults = hybridSearchService.hybridSearch(
                queryVector, rewrittenQuery);

        List<Chunk> chunks = new ArrayList<>();
        if (!searchResults.isEmpty()) {
            chunks = convertToChunks(searchResults);
        }

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

        List<Chunk> finalChunks = chunks;
        siliconFlowClient.streamChat(systemPrompt, userPrompt,
                new SiliconFlowClient.StreamCallback() {
                    @Override
                    public void onToken(String token) {
                        callback.onToken(token);
                    }

                    @Override
                    public void onComplete(String fullContent,
                                           int promptTokens, int completionTokens) {
                        if (sessionId != null) {
                            sessionMemoryService.addMessage(sessionId, "user", question);
                            sessionMemoryService.addMessage(sessionId, "assistant", fullContent);
                        }

                        RagResponse response = new RagResponse();
                        response.setAnswer(fullContent);
                        response.setSessionId(sessionId);
                        if (!finalChunks.isEmpty()) {
                            response.setCitations(parseCitations(fullContent, finalChunks));
                        } else {
                            response.setCitations(new ArrayList<>());
                        }
                        persistToSqlite(sessionId, question, fullContent, response.getCitations());
                        callback.onComplete(response);
                    }

                    @Override
                    public void onError(Exception e, String partialContent) {
                        callback.onError(e);
                    }
                });
    }

    private void handleToolIntentStreaming(String question, String sessionId,
                                            List<ChatMessage> history,
                                            StreamCallback callback) {
        String systemPrompt = promptService.getSystemPrompt();
        String summary = sessionMemoryService.getSummary(sessionId);
        if (!history.isEmpty() || (summary != null && !summary.isEmpty())) {
            systemPrompt = buildSystemPromptWithHistory(history, systemPrompt, summary);
        }

        functionCallService.chatWithToolsStreaming(systemPrompt, question,
                new SiliconFlowClient.StreamCallback() {
                    @Override
                    public void onToken(String token) {
                        callback.onToken(token);
                    }

                    @Override
                    public void onComplete(String fullContent,
                                           int promptTokens, int completionTokens) {
                        if (sessionId != null) {
                            sessionMemoryService.addMessage(sessionId, "user", question);
                            sessionMemoryService.addMessage(sessionId, "assistant", fullContent);
                        }

                        RagResponse response = new RagResponse();
                        response.setAnswer(fullContent);
                        response.setSessionId(sessionId);
                        response.setCitations(new ArrayList<>());
                        persistToSqlite(sessionId, question, fullContent, response.getCitations());
                        callback.onComplete(response);
                    }

                    @Override
                    public void onError(Exception e, String partialContent) {
                        callback.onError(e);
                    }
                });
    }

    private void handleChitchatIntentStreaming(String question, String sessionId,
                                                List<ChatMessage> history,
                                                IntentClassifier.IntentResult intentResult,
                                                StreamCallback callback) {
        String answer;
        if (intentResult.getReply() != null && !intentResult.getReply().isEmpty()) {
            answer = intentResult.getReply();
        } else {
            if (question.contains("你好") || question.contains("您好")) {
                answer = "您好！请问有什么可以帮您的？";
            } else if (question.contains("谢谢") || question.contains("感谢")) {
                answer = "不客气，还有其他问题随时问我。";
            } else {
                answer = "好的，如果您有任何问题，随时告诉我。";
            }
        }

        if (sessionId != null) {
            sessionMemoryService.addMessage(sessionId, "user", question);
            sessionMemoryService.addMessage(sessionId, "assistant", answer);
        }

        RagResponse response = new RagResponse();
        response.setAnswer(answer);
        response.setSessionId(sessionId);
        response.setCitations(new ArrayList<>());

        persistToSqlite(sessionId, question, answer, response.getCitations());
        callback.onToken(answer);
        callback.onComplete(response);
    }

    private void handleClarificationIntentStreaming(String question, String sessionId,
                                                     IntentClassifier.IntentResult intentResult,
                                                     StreamCallback callback) {
        String answer;
        if (intentResult.getReply() != null && !intentResult.getReply().isEmpty()) {
            answer = intentResult.getReply();
        } else {
            answer = "您的问题我还不太明确，能否告诉我您想了解哪方面的信息？"
                    + "比如：产品信息、订单查询、退换货政策等。";
        }

        RagResponse response = new RagResponse();
        response.setAnswer(answer);
        response.setSessionId(sessionId);
        response.setCitations(new ArrayList<>());

        callback.onToken(answer);
        callback.onComplete(response);
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

    private void persistToSqlite(String sessionId, String question, String answer,
                              java.util.List<RagResponse.CitationInfo> citations) {
        if (sessionId == null) return;
        try {
            // Save or update session (title = first user message, truncated to 50 chars)
            String title = question.length() > 50 ? question.substring(0, 50) + "..." : question;
            int messageCount = sessionMemoryService.getMessages(sessionId).size();
            sessionRepository.updateSessionOnMessage(sessionId, title, messageCount);

            // Save user message
            sessionRepository.saveMessage(sessionId, "user", question, null);

            // Save assistant message with citations
            sessionRepository.saveMessage(sessionId, "assistant", answer, citations);
        } catch (Exception e) {
            log.warn("Failed to persist session to SQLite: {}", e.getMessage());
        }
    }

    public interface StreamCallback {
        void onToken(String token);
        void onComplete(RagResponse response);
        void onError(Exception e);
    }
}
