package com.wokrag.agent.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Chunk {
    private String id;
    private String content;
    private String source;
    private String sourceUrl;
    private String updateTime;
    private Map<String, String> metadata;
}
