package com.wokrag.agent.repository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

@Slf4j
@Repository
@RequiredArgsConstructor
public class DocumentRepository {

    private final JdbcTemplate jdbc;

    public void save(String docId, String name, String source, int chunkCount) {
        jdbc.update(
                "INSERT INTO documents (doc_id, name, source, chunk_count) VALUES (?, ?, ?, ?)",
                docId, name, source, chunkCount
        );
    }

    public List<Map<String, Object>> findAll() {
        return jdbc.queryForList(
                "SELECT doc_id, name, source, upload_time, chunk_count FROM documents ORDER BY upload_time DESC"
        );
    }

    public void delete(String docId) {
        jdbc.update("DELETE FROM documents WHERE doc_id = ?", docId);
    }

    public boolean exists(String docId) {
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM documents WHERE doc_id = ?",
                Integer.class, docId
        );
        return count != null && count > 0;
    }
}
