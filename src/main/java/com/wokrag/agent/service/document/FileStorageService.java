package com.wokrag.agent.service.document;

import com.wokrag.agent.config.FileStorageConfig;
import com.wokrag.agent.exception.RagException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDate;

@Slf4j
@Service
public class FileStorageService {

    private final Path storageRoot;

    public FileStorageService(FileStorageConfig config) {
        this.storageRoot = Paths.get(config.getPath()).toAbsolutePath();
        try {
            Files.createDirectories(storageRoot);
        } catch (IOException e) {
            throw new RagException("Failed to create storage directory: " + storageRoot, e.getMessage());
        }
    }

    public String store(String docId, String fileName, byte[] content) {
        try {
            LocalDate today = LocalDate.now();
            String datePath = String.format("%d/%02d/%02d", today.getYear(), today.getMonthValue(), today.getDayOfMonth());
            Path dateDir = storageRoot.resolve(datePath);
            Files.createDirectories(dateDir);
            Path filePath = dateDir.resolve(fileName);
            Files.write(filePath, content, StandardOpenOption.CREATE, StandardOpenOption.WRITE);
            log.info("Stored file: {}", filePath);
            return datePath + "/" + fileName;
        } catch (IOException e) {
            throw new RagException("Failed to store file for docId: " + docId, e.getMessage());
        }
    }

    public Resource load(String filePath) {
        Path fullPath = storageRoot.resolve(filePath);
        if (!Files.exists(fullPath)) {
            throw new RagException.DocumentParseException("File not found: " + fullPath);
        }
        return new FileSystemResource(fullPath);
    }

    public void delete(String filePath) {
        try {
            Path fullPath = storageRoot.resolve(filePath);
            if (Files.exists(fullPath)) {
                Files.delete(fullPath);
                log.info("Deleted file: {}", fullPath);
            }
        } catch (IOException e) {
            log.warn("Failed to delete file: {}", filePath, e);
        }
    }
}
