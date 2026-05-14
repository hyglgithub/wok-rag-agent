package com.wokrag.agent.service.document;

import com.wokrag.agent.model.Chunk;
import com.wokrag.agent.util.TextUtil;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class ChunkService {

    public List<Chunk> chunkText(String text, int chunkSize, int overlap, String source) {
        List<String> textChunks = TextUtil.recursiveChunk(text, chunkSize, overlap);
        List<Chunk> chunks = new ArrayList<>();

        for (int i = 0; i < textChunks.size(); i++) {
            String content = textChunks.get(i);
            if (content.isEmpty()) continue;

            Chunk chunk = new Chunk();
            chunk.setId(UUID.randomUUID().toString());
            chunk.setContent(content);
            chunk.setSource(source);
            chunk.setUpdateTime(java.time.LocalDate.now().toString());
            chunk.setMetadata(new java.util.HashMap<>());
            chunk.getMetadata().put("chunk_index", String.valueOf(i));
            chunk.getMetadata().put("start_offset", String.valueOf(i * (chunkSize - overlap)));

            chunks.add(chunk);
        }

        return chunks;
    }
}
