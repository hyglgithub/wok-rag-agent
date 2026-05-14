package com.wokrag.agent.service.document;

import com.wokrag.agent.model.ParseResult;
import org.springframework.web.multipart.MultipartFile;

public interface DocumentService {
    ParseResult parseFile(MultipartFile file);
    String detectMimeType(MultipartFile file);
}
