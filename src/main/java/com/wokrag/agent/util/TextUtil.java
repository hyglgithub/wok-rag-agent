package com.wokrag.agent.util;

import java.util.ArrayList;
import java.util.List;

public class TextUtil {

    private TextUtil() {}

    public static String cleanText(String text) {
        if (text == null) return "";

        return text
            .replaceAll("\\r\\n", "\n")
            .replaceAll("\\r", "\n")
            .replaceAll("(?m)^[ \\t]+|[ \\t]+$", "")
            .replaceAll("\\n{3,}", "\n\n")
            .replaceAll("[ \\t]+", " ")
            .trim();
    }

    public static int estimateTokens(String text) {
        if (text == null || text.isEmpty()) return 0;

        int chineseChars = 0;
        int otherChars = 0;

        for (char c : text.toCharArray()) {
            if (Character.UnicodeScript.of(c) == Character.UnicodeScript.HAN) {
                chineseChars++;
            } else if (!Character.isWhitespace(c)) {
                otherChars++;
            }
        }

        return (int) (chineseChars * 1.5 + otherChars / 4.0);
    }

    public static List<String> recursiveChunk(String text, int chunkSize, int overlap) {
        List<String> chunks = new ArrayList<>();
        if (text == null || text.isEmpty()) return chunks;

        String[] separators = {"\n\n", "\n", "。", ".", "！", "!", "？", "?"};
        recursiveSplit(text, separators, 0, chunkSize, overlap, chunks);

        return chunks;
    }

    private static void recursiveSplit(String text, String[] separators, int sepIndex,
                                       int chunkSize, int overlap, List<String> chunks) {
        if (text.length() <= chunkSize) {
            chunks.add(text.trim());
            return;
        }

        if (sepIndex >= separators.length) {
            for (int i = 0; i < text.length(); i += chunkSize - overlap) {
                int end = Math.min(i + chunkSize, text.length());
                chunks.add(text.substring(i, end).trim());
            }
            return;
        }

        String separator = separators[sepIndex];
        String[] parts = text.split(java.util.regex.Pattern.quote(separator), -1);

        StringBuilder current = new StringBuilder();
        for (String part : parts) {
            if (current.length() + part.length() + separator.length() > chunkSize) {
                if (current.length() > 0) {
                    chunks.add(current.toString().trim());
                    current = new StringBuilder();
                }
                if (part.length() > chunkSize) {
                    recursiveSplit(part, separators, sepIndex + 1, chunkSize, overlap, chunks);
                } else {
                    current.append(part).append(separator);
                }
            } else {
                current.append(part).append(separator);
            }
        }

        if (current.length() > 0) {
            chunks.add(current.toString().trim());
        }
    }
}
