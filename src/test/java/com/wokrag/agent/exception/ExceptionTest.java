package com.wokrag.agent.exception;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ExceptionTest {

    @Test
    void testRagExceptionCreation() {
        RagException ex = new RagException("TEST_ERROR", "Test error message");
        assertEquals("TEST_ERROR", ex.getErrorCode());
        assertEquals("Test error message", ex.getErrorMessage());
    }

    @Test
    void testDocumentParseException() {
        RagException.DocumentParseException ex =
            new RagException.DocumentParseException("Failed to parse");
        assertEquals("DOCUMENT_PARSE_ERROR", ex.getErrorCode());
    }

    @Test
    void testEmbeddingException() {
        RagException.EmbeddingException ex =
            new RagException.EmbeddingException("Embedding failed");
        assertEquals("EMBEDDING_ERROR", ex.getErrorCode());
    }
}
