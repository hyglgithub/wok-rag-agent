package com.wokrag.agent.util;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.regex.Pattern;

public class TextUtil {

    private static final Pattern HEADING_PATTERN = Pattern.compile("^#{1,4}\\s+");

    private TextUtil() {}

    public static boolean isMarkdown(String mimeType) {
        if (mimeType == null) return false;
        return mimeType.equals("text/markdown") || mimeType.equals("text/x-markdown") || mimeType.equals("text/x-web-markdown");
    }

    public static String cleanText(String text) {
        if (text == null) return "";

        return text
                // 1. 统一换行符
                .replaceAll("\\r\\n", "\n")
                .replaceAll("\\r", "\n")

                // 2. 关键！清洗爬虫最常见的 \u00A0 不间断空格（你之前无效的根源）
                .replace("\\u00A0", " ")
                .replace("\u00A0", " ")

                // 3. 移除不可见乱码字符
                .replaceAll("[\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F\\u00AD]", "")

                // 4. 去掉每行 开头 / 结尾 的空白
                .replaceAll("(?m)^[ \\t]+", "")
                .replaceAll("(?m)[ \\t]+$", "")

                // 5. 清洗掉 af://nxxx 这种垃圾链接（你文本里的噪音）
                .replaceAll("af://n\\d+", "")

                // 6. 核心：把所有连续空行 → 只保留 1 个空行
                .replaceAll("(\n\\s*){2,}", "\n\n")

                // 7. 最终清理首尾空白
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

    public static List<String> markdownChunk(String text, int chunkSize, int overlap) {
        List<String> chunks = new ArrayList<>();
        if (text == null || text.isEmpty()) return chunks;

        String[] lines = text.split("\n");
        Section root = parseSectionTree(lines);
        flattenTree(root, "", chunkSize, chunks);
        return chunks;
    }

    private static class Section {
        String heading;       // full heading line, e.g. "## Auth"
        StringBuilder intro;  // content before first child heading
        List<Section> children;

        Section(String heading) {
            this.heading = heading;
            this.intro = new StringBuilder();
            this.children = new ArrayList<>();
        }

        int level() {
            if (heading == null) return 0;
            int count = 0;
            for (char c : heading.toCharArray()) {
                if (c == '#') count++;
                else break;
            }
            return count;
        }
    }

    private static Section parseSectionTree(String[] lines) {
        Section root = new Section(null);
        Deque<Section> stack = new ArrayDeque<>();
        stack.push(root);

        for (String line : lines) {
            if (HEADING_PATTERN.matcher(line).find()) {
                int level = countHashes(line);
                Section section = new Section(line);

                // Pop back to parent level
                while (stack.size() > 1 && stack.peek().level() >= level) {
                    stack.pop();
                }
                stack.peek().children.add(section);
                stack.push(section);
            } else {
                stack.peek().intro.append(line).append("\n");
            }
        }
        return root;
    }

    private static void flattenTree(Section node, String parentPrefix, int chunkSize, List<String> chunks) {
        if (node.heading == null) {
            // Root node: just process children
            String intro = node.intro.toString().trim();
            if (!intro.isEmpty()) {
                chunks.add(intro);
            }
            for (Section child : node.children) {
                flattenTree(child, "", chunkSize, chunks);
            }
            return;
        }

        String prefix = parentPrefix + node.heading + "\n";
        String fullText = getSectionText(node, parentPrefix);
        if (fullText.length() <= chunkSize) {
            // Whole section fits → emit as one chunk with all parent headings
            chunks.add(fullText);
        } else {
            // Too big → emit heading+intro (with parents), then recurse into children
            String intro = node.intro.toString().trim();
            String headingAndIntro = prefix + intro;
            if (!intro.isEmpty()) {
                chunks.add(headingAndIntro.trim());
            }
            for (Section child : node.children) {
                flattenTree(child, prefix, chunkSize, chunks);
            }
        }
    }

    private static String getSectionText(Section node, String parentPrefix) {
        StringBuilder sb = new StringBuilder();
        if (node.heading != null) {
            sb.append(parentPrefix).append(node.heading).append("\n");
        }
        String intro = node.intro.toString().trim();
        if (!intro.isEmpty()) {
            sb.append(intro).append("\n");
        }
        String childPrefix = (node.heading != null) ? parentPrefix + node.heading + "\n" : parentPrefix;
        for (Section child : node.children) {
            sb.append(getSectionText(child, childPrefix));
        }
        return sb.toString().trim();
    }

    private static int countHashes(String line) {
        int count = 0;
        for (char c : line.toCharArray()) {
            if (c == '#') count++;
            else break;
        }
        return count;
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
