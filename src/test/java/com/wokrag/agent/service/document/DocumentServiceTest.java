package com.wokrag.agent.service.document;

import com.wokrag.agent.model.Chunk;
import com.wokrag.agent.model.ParseResult;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DocumentServiceTest {

    private final DocumentService documentService = new DocumentServiceImpl();
    private final ChunkService chunkService = new ChunkService();

    @Test
    void testParseTextFile() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "test.txt", "text/plain", "Hello World".getBytes());

        ParseResult result = documentService.parseFile(file);

        assertTrue(result.isSuccess());
        assertEquals("Hello World", result.getContent());
        assertEquals("text/plain", result.getMimeType());
    }

    @Test
    void testChunkText() {
        String text = "First sentence. Second sentence. Third sentence. Fourth sentence.";
        List<Chunk> chunks = chunkService.chunkText(text, 30, 5, "test.txt");

        assertFalse(chunks.isEmpty());
        for (Chunk chunk : chunks) {
            assertNotNull(chunk.getId());
            assertNotNull(chunk.getContent());
            assertEquals("test.txt", chunk.getSource());
        }
    }
}
