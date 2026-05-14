package com.wokrag.agent.service.document;

import com.wokrag.agent.exception.RagException;
import com.wokrag.agent.model.ParseResult;
import com.wokrag.agent.util.TextUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.Tika;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.metadata.TikaCoreProperties;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.Parser;
import org.apache.tika.sax.BodyContentHandler;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
public class DocumentServiceImpl implements DocumentService {

    private static final int MAX_TEXT_LENGTH = 10 * 1024 * 1024; // 10MB

    private final Tika tika = new Tika();
    private final Parser parser = new AutoDetectParser();

    @Override
    public ParseResult parseFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return ParseResult.failure("File is empty");
        }

        String originalFilename = file.getOriginalFilename();
        log.info("Parsing file: {}, size: {} bytes", originalFilename, file.getSize());

        try {
            String mimeType;
            try (InputStream detectStream = file.getInputStream()) {
                mimeType = tika.detect(detectStream, originalFilename);
            }

            BodyContentHandler handler = new BodyContentHandler(MAX_TEXT_LENGTH);
            Metadata metadata = new Metadata();
            metadata.set(TikaCoreProperties.RESOURCE_NAME_KEY, originalFilename);
            ParseContext context = new ParseContext();

            try (InputStream parseStream = file.getInputStream()) {
                parser.parse(parseStream, handler, metadata, context);
            }

            String content = TextUtil.cleanText(handler.toString());
            Map<String, String> metadataMap = extractMetadata(metadata);

            if (content.isEmpty()) {
                log.warn("File {} parsed to empty content", originalFilename);
                return ParseResult.failure("Parsed content is empty");
            }

            log.info("File {} parsed successfully, content length: {}",
                     originalFilename, content.length());
            return ParseResult.success(mimeType, content, metadataMap);

        } catch (IOException e) {
            log.error("Failed to read file: {}", originalFilename, e);
            return ParseResult.failure("Failed to read file: " + e.getMessage());
        } catch (SAXException e) {
            log.error("Failed to parse document structure: {}", originalFilename, e);
            return ParseResult.failure("Document structure parse failed: " + e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error parsing file: {}", originalFilename, e);
            return ParseResult.failure("Unexpected error: " + e.getMessage());
        }
    }

    @Override
    public String detectMimeType(MultipartFile file) {
        try (InputStream inputStream = file.getInputStream()) {
            return tika.detect(inputStream, file.getOriginalFilename());
        } catch (IOException e) {
            throw new RagException.DocumentParseException("Failed to detect MIME type", e);
        }
    }

    private Map<String, String> extractMetadata(Metadata metadata) {
        Map<String, String> result = new HashMap<>();
        for (String name : metadata.names()) {
            String value = metadata.get(name);
            if (value != null && !value.isEmpty()) {
                result.put(name, value);
            }
        }
        return result;
    }
}
