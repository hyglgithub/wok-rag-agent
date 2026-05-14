package com.wokrag.agent.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.Map;

@Data
@NoArgsConstructor
public class SearchResult {
    private String chunkId;
    private String content;
    private double score;
    private Map<String, String> metadata;
}
