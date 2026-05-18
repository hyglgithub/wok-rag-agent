package com.wokrag.agent.controller;

import com.wokrag.agent.client.MilvusClientWrapper;
import com.wokrag.agent.config.RagConfig;
import com.wokrag.agent.model.Chunk;
import com.wokrag.agent.model.ParseResult;
import com.wokrag.agent.repository.DocumentRepository;
import com.wokrag.agent.service.document.ChunkService;
import com.wokrag.agent.service.document.DocumentService;
import com.wokrag.agent.service.document.FileStorageService;
import com.wokrag.agent.service.embedding.EmbeddingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import org.springframework.http.HttpStatus;

@Slf4j
@RestController
@RequestMapping("/api/documents")
@RequiredArgsConstructor
@Tag(name = "Documents", description = "Knowledge base document management")
public class DocumentController {

    private final DocumentService documentService;
    private final ChunkService chunkService;
    private final EmbeddingService embeddingService;
    private final MilvusClientWrapper milvusClient;
    private final DocumentRepository documentRepository;
    private final RagConfig ragConfig;
    private final FileStorageService fileStorageService;

    @GetMapping
    @Operation(summary = "List all documents")
    public ResponseEntity<Map<String, Object>> listDocuments() {
        List<Map<String, Object>> rows = documentRepository.findAll();

        List<Map<String, Object>> documents = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            Map<String, Object> doc = new LinkedHashMap<>();
            doc.put("id", row.get("doc_id"));
            doc.put("name", row.get("name"));
            doc.put("source", row.get("source") != null ? row.get("source") : "");
            doc.put("uploadTime", row.get("upload_time"));
            doc.put("chunkCount", row.get("chunk_count"));
            documents.add(doc);
        }

        return ResponseEntity.ok(Map.of("documents", documents));
    }

    @PostMapping("/upload")
    @Operation(summary = "Upload a document to the knowledge base")
    public ResponseEntity<Map<String, Object>> uploadDocument(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "source", required = false) String source) {

        String docId = UUID.randomUUID().toString();
        String fileName = file.getOriginalFilename();
        if (source == null || source.isEmpty()) {
            source = fileName;
        }

        log.info("Uploading document: {} (docId={})", fileName, docId);

        // Step 0: Compute file hash and check for duplicates
        String fileHash;
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = md.digest(file.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : hashBytes) {
                sb.append(String.format("%02x", b));
            }
            fileHash = sb.toString();
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                    "error", "HASH_ERROR",
                    "message", "Failed to compute file hash"
            ));
        }

        Map<String, Object> existing = documentRepository.findByHash(fileHash);
        if (existing != null) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(
                    "error", "DUPLICATE_FILE",
                    "message", "文件已存在: " + existing.get("name")
            ));
        }

        // Step 1: Parse file
        ParseResult parseResult = documentService.parseFile(file);
        if (!parseResult.isSuccess()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "PARSE_ERROR",
                    "message", parseResult.getErrorMessage()
            ));
        }

        // Step 2: Chunk text
        List<Chunk> chunks = chunkService.chunkText(
                parseResult.getContent(),
                ragConfig.getChunkSize(),
                ragConfig.getChunkOverlap(),
                source
        );

        if (chunks.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "EMPTY_DOCUMENT",
                    "message", "Document produced no usable content"
            ));
        }

        // Step 3: Generate embeddings
        List<String> texts = chunks.stream().map(Chunk::getContent).toList();
        List<double[]> embeddings = embeddingService.embedBatch(texts);

        // Step 4: Insert into Milvus
        List<Map<String, Object>> rows = new ArrayList<>();
        for (int i = 0; i < chunks.size(); i++) {
            Chunk chunk = chunks.get(i);
            Map<String, Object> row = new HashMap<>();
            row.put("chunk_text", chunk.getContent());
            row.put("text_dense", embeddings.get(i));
            row.put("doc_id", docId);
            row.put("source", source);
            row.put("source_url", "");
            rows.add(row);
        }
        milvusClient.insert(rows);

        // Step 5: Store original file
        String filePath;
        try {
            filePath = fileStorageService.store(docId, fileName, file.getBytes());
        } catch (Exception e) {
            log.error("Failed to store file: {}", fileName, e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "error", "FILE_STORAGE_ERROR",
                    "message", "Failed to store file: " + e.getMessage()
            ));
        }

        // Step 6: Save metadata to SQLite
        documentRepository.save(docId, fileName, source, chunks.size(), fileHash, filePath);

        log.info("Document uploaded successfully: {} ({} chunks)", fileName, chunks.size());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", docId);
        result.put("name", fileName);
        result.put("source", source);
        result.put("uploadTime", java.time.LocalDateTime.now().toString());
        result.put("chunkCount", chunks.size());

        return ResponseEntity.ok(result);
    }

    @GetMapping("/{docId}/download")
    @Operation(summary = "Download original document file")
    public ResponseEntity<Resource> downloadDocument(@PathVariable String docId) {
        String filePath = documentRepository.findFilePath(docId);
        if (filePath == null || filePath.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        String[] parts = filePath.split("/", 2);
        Resource resource = fileStorageService.load(parts[0], parts[1]);

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + parts[1] + "\"")
                .body(resource);
    }

    @GetMapping("/{docId}/preview")
    @Operation(summary = "Preview document file (for PDF inline viewing)")
    public ResponseEntity<Resource> previewDocument(@PathVariable String docId) {
        String filePath = documentRepository.findFilePath(docId);
        if (filePath == null || filePath.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        String[] parts = filePath.split("/", 2);
        Resource resource = fileStorageService.load(parts[0], parts[1]);

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + parts[1] + "\"")
                .body(resource);
    }

    @GetMapping("/{docId}/chunks")
    @Operation(summary = "Get all chunks for a document")
    public ResponseEntity<Map<String, Object>> getChunks(@PathVariable String docId) {
        var results = milvusClient.queryByDocId(docId);

        List<Map<String, Object>> chunks = new ArrayList<>();
        for (int i = 0; i < results.size(); i++) {
            var result = results.get(i);
            Map<String, Object> chunk = new LinkedHashMap<>();
            var entity = result.getEntity();
            chunk.put("milvusId", entity.get("id"));
            chunk.put("chunkText", entity.get("chunk_text"));
            chunk.put("chunkIndex", i);
            chunk.put("source", entity.get("source"));
            chunks.add(chunk);
        }

        return ResponseEntity.ok(Map.of("chunks", chunks));
    }

    @DeleteMapping("/chunks/{milvusId}")
    @Operation(summary = "Delete a single chunk")
    public ResponseEntity<Map<String, String>> deleteChunk(
            @PathVariable long milvusId,
            @RequestParam String docId) {
        milvusClient.deleteByPrimaryKey(milvusId);
        documentRepository.decrementChunkCount(docId);
        return ResponseEntity.ok(Map.of("status", "ok"));
    }

    @PutMapping("/chunks/{milvusId}")
    @Operation(summary = "Update a chunk's text and re-embed")
    public ResponseEntity<Map<String, String>> updateChunk(
            @PathVariable long milvusId,
            @RequestBody Map<String, String> body) {
        String newText = body.get("text");
        if (newText == null || newText.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "text is required"));
        }

        List<double[]> embeddings = embeddingService.embedBatch(List.of(newText));
        float[] vector = toFloatArray(embeddings.get(0));
        milvusClient.updateByPrimaryKey(milvusId, newText, vector);

        return ResponseEntity.ok(Map.of("status", "ok"));
    }

    @PostMapping("/{docId}/chunks")
    @Operation(summary = "Add a new chunk to a document")
    public ResponseEntity<Map<String, Object>> addChunk(
            @PathVariable String docId,
            @RequestBody Map<String, String> body) {
        String text = body.get("text");
        if (text == null || text.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "text is required"));
        }

        String source = documentRepository.findNameByDocId(docId);

        List<double[]> embeddings = embeddingService.embedBatch(List.of(text));
        float[] vector = toFloatArray(embeddings.get(0));

        Map<String, Object> row = new HashMap<>();
        row.put("chunk_text", text);
        row.put("text_dense", vector);
        row.put("doc_id", docId);
        row.put("source", source);
        row.put("source_url", "");
        milvusClient.insert(List.of(row));

        documentRepository.incrementChunkCount(docId);

        return ResponseEntity.ok(Map.of("status", "ok"));
    }

    @DeleteMapping("/{docId}")
    @Operation(summary = "Delete a document and its chunks")
    public ResponseEntity<Map<String, String>> deleteDocument(@PathVariable String docId) {
        log.info("Deleting document: {}", docId);

        // Delete from Milvus
        milvusClient.deleteByDocId(docId);

        // Delete stored files
        fileStorageService.delete(docId);

        // Delete from SQLite
        documentRepository.delete(docId);

        log.info("Document deleted: {}", docId);
        return ResponseEntity.ok(Map.of("status", "ok"));
    }

    private float[] toFloatArray(double[] doubles) {
        float[] floats = new float[doubles.length];
        for (int i = 0; i < doubles.length; i++) {
            floats[i] = (float) doubles[i];
        }
        return floats;
    }
}
