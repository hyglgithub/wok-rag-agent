package com.wokrag.agent.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
public class RagResponse {
    private String answer;
    private String sessionId;
    private List<CitationInfo> citations;

    @Data
    @NoArgsConstructor
    public static class CitationInfo {
        private Integer index;
        private String source;
        private String sourceUrl;
        private String chunkContent;
    }
}
