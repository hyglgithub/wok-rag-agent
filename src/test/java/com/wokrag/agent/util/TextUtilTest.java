package com.wokrag.agent.util;

import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class TextUtilTest {

    @Test
    void testCleanText() {
        String dirty = "  Hello   World  \n\n\n\n  Test  ";
        String cleaned = TextUtil.cleanText(dirty);
        assertEquals("Hello World\n\nTest", cleaned);
    }

    @Test
    void testEstimateTokens() {
        String chineseText = "你好世界";
        String englishText = "Hello World";

        assertTrue(TextUtil.estimateTokens(chineseText) > 0);
        assertTrue(TextUtil.estimateTokens(englishText) > 0);
    }

    @Test
    void testRecursiveChunking() {
        String text = "First paragraph.\n\nSecond paragraph.\n\nThird paragraph.";
        List<String> chunks = TextUtil.recursiveChunk(text, 30, 5);

        assertFalse(chunks.isEmpty());
        for (String chunk : chunks) {
            assertTrue(chunk.length() <= 35);
        }
    }
}
