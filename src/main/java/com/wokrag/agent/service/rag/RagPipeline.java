package com.wokrag.agent.service.rag;

import com.wokrag.agent.exception.RagException;
import com.wokrag.agent.model.Chunk;
import com.wokrag.agent.model.RagResponse;
import com.wokrag.agent.model.SearchResult;
import com.wokrag.agent.service.embedding.EmbeddingService;
import com.wokrag.agent.service.generation.LlmService;
import com.wokrag.agent.service.retrieval.HybridSearchService;
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

    public RagResponse execute(String question) {
        log.info("Executing RAG pipeline for question: {}", question);

        try {
            log.debug("Step 1: Embedding query");
            double[] queryVector = embeddingService.embed(question);

            log.debug("Step 2: Performing hybrid search");
            List<SearchResult> searchResults = hybridSearchService.hybridSearch(
                    queryVector, question);

            if (searchResults.isEmpty()) {
                log.warn("No search results found for question: {}", question);
                RagResponse response = new RagResponse();
                response.setAnswer("I could not find relevant information in the knowledge base to answer your question. Please try rephrasing your question or contact customer service for assistance.");
                response.setCitations(new ArrayList<>());
                return response;
            }

            log.debug("Step 3: Converting search results to chunks");
            List<Chunk> chunks = convertToChunks(searchResults);

            log.debug("Step 4: Generating answer");
            RagResponse response = llmService.generateWithCitations(chunks, question);

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
}
