package com.wokrag.agent.model;

import lombok.Data;

@Data
public class StreamUsage {
    private int promptTokens;
    private int completionTokens;
    private int totalTokens;
}
