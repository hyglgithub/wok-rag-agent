package com.wokrag.agent.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
public class RagResponse {
    @Schema(description = "The generated answer text")
    private String answer;
    @Schema(description = "Session ID for conversation tracking")
    private String sessionId;
    @Schema(description = "List of source citations referenced in the answer")
    private List<CitationInfo> citations;

    @Data
    @NoArgsConstructor
    public static class CitationInfo {
        @Schema(description = "Citation index number", example = "1")
        private Integer index;
        @Schema(description = "Source document name")
        private String source;
        @Schema(description = "URL to the source document")
        private String sourceUrl;
        @Schema(description = "The referenced chunk text")
        private String chunkContent;
    }
}
