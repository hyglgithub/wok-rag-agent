package com.wokrag.agent.model;

import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class ModelTest {

    @Test
    void testChunkCreation() {
        Chunk chunk = new Chunk();
        chunk.setId("chunk-001");
        chunk.setContent("Test content");
        chunk.setSource("test.pdf");
        chunk.setSourceUrl("/docs/test.pdf");
        chunk.setUpdateTime("2026-05-14");
        chunk.setMetadata(Map.of("doc_id", "doc-001"));

        assertEquals("chunk-001", chunk.getId());
        assertEquals("Test content", chunk.getContent());
        assertEquals("test.pdf", chunk.getSource());
        assertEquals(1, chunk.getMetadata().size());
    }

    @Test
    void testSearchResultCreation() {
        SearchResult result = new SearchResult();
        result.setChunkId("chunk-001");
        result.setContent("Test content");
        result.setScore(0.95);
        result.setMetadata(Map.of("source", "test.pdf"));

        assertEquals("chunk-001", result.getChunkId());
        assertEquals(0.95, result.getScore());
    }

    @Test
    void testRagResponseWithCitations() {
        RagResponse response = new RagResponse();
        response.setAnswer("Test answer [1]");

        RagResponse.CitationInfo citation = new RagResponse.CitationInfo();
        citation.setIndex(1);
        citation.setSource("test.pdf");
        citation.setSourceUrl("/docs/test.pdf");
        citation.setChunkContent("Referenced content");

        response.setCitations(List.of(citation));

        assertEquals("Test answer [1]", response.getAnswer());
        assertEquals(1, response.getCitations().size());
        assertEquals(1, response.getCitations().get(0).getIndex());
    }
}
