package com.wokrag.agent.service.document;

import com.wokrag.agent.config.FileStorageConfig;
import com.wokrag.agent.exception.RagException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.*;

@Slf4j
@Service
public class FileStorageService {

    private final Path storageRoot;

    public FileStorageService(FileStorageConfig config) {
        this.storageRoot = Paths.get(config.getPath()).toAbsolutePath();
        try {
            Files.createDirectories(storageRoot);
        } catch (IOException e) {
            throw new RagException("Failed to create storage directory: " + storageRoot, e);
        }
    }

    public String store(String docId, String fileName, byte[] content) {
        try {
            Path docDir = storageRoot.resolve(docId);
            Files.createDirectories(docDir);
            Path filePath = docDir.resolve(fileName);
            Files.write(filePath, content, StandardOpenOption.CREATE, StandardOpenOption.WRITE);
            log.info("Stored file: {}", filePath);
            return docId + "/" + fileName;
        } catch (IOException e) {
            throw new RagException("Failed to store file for docId: " + docId, e);
        }
    }

    public Resource load(String docId, String fileName) {
        Path filePath = storageRoot.resolve(docId).resolve(fileName);
        if (!Files.exists(filePath)) {
            throw new RagException.DocumentParseException("File not found: " + filePath);
        }
        return new FileSystemResource(filePath);
    }

    public void delete(String docId) {
        try {
            Path docDir = storageRoot.resolve(docId);
            if (Files.exists(docDir)) {
                Files.walk(docDir)
                        .sorted((a, b) -> b.compareTo(a))
                        .forEach(path -> {
                            try { Files.delete(path); } catch (IOException ignored) {}
                        });
                log.info("Deleted file directory: {}", docDir);
            }
        } catch (IOException e) {
            log.warn("Failed to delete file directory for docId: {}", docId, e);
        }
    }
}
