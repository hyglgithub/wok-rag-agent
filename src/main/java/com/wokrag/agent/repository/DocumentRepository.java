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

    public void save(String docId, String name, String source, int chunkCount, String fileHash, String filePath) {
        jdbc.update(
                "INSERT INTO documents (doc_id, name, source, chunk_count, file_hash, file_path) VALUES (?, ?, ?, ?, ?, ?)",
                docId, name, source, chunkCount, fileHash, filePath
        );
    }

    public List<Map<String, Object>> findAll() {
        return jdbc.queryForList(
                "SELECT doc_id, name, source, upload_time, chunk_count, file_hash, file_path FROM documents ORDER BY upload_time DESC"
        );
    }

    public Map<String, Object> findByHash(String fileHash) {
        List<Map<String, Object>> results = jdbc.queryForList(
                "SELECT doc_id, name, source, upload_time, chunk_count FROM documents WHERE file_hash = ? LIMIT 1",
                fileHash
        );
        return results.isEmpty() ? null : results.get(0);
    }

    public String findFilePath(String docId) {
        return jdbc.queryForObject(
                "SELECT file_path FROM documents WHERE doc_id = ?",
                String.class, docId
        );
    }

    public void incrementChunkCount(String docId) {
        jdbc.update(
                "UPDATE documents SET chunk_count = chunk_count + 1 WHERE doc_id = ?",
                docId
        );
    }

    public String findNameByDocId(String docId) {
        return jdbc.queryForObject(
                "SELECT name FROM documents WHERE doc_id = ?",
                String.class, docId
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
