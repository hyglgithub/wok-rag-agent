package com.wokrag.agent.exception;

import lombok.Getter;

@Getter
public class RagException extends RuntimeException {
    private final String errorCode;
    private final String errorMessage;

    public RagException(String errorCode, String errorMessage) {
        super(errorMessage);
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
    }

    public RagException(String errorCode, String errorMessage, Throwable cause) {
        super(errorMessage, cause);
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
    }

    public static class DocumentParseException extends RagException {
        public DocumentParseException(String message) {
            super("DOCUMENT_PARSE_ERROR", message);
        }

        public DocumentParseException(String message, Throwable cause) {
            super("DOCUMENT_PARSE_ERROR", message, cause);
        }
    }

    public static class EmbeddingException extends RagException {
        public EmbeddingException(String message) {
            super("EMBEDDING_ERROR", message);
        }

        public EmbeddingException(String message, Throwable cause) {
            super("EMBEDDING_ERROR", message, cause);
        }
    }

    public static class RetrievalException extends RagException {
        public RetrievalException(String message) {
            super("RETRIEVAL_ERROR", message);
        }

        public RetrievalException(String message, Throwable cause) {
            super("RETRIEVAL_ERROR", message, cause);
        }
    }

    public static class GenerationException extends RagException {
        public GenerationException(String message) {
            super("GENERATION_ERROR", message);
        }

        public GenerationException(String message, Throwable cause) {
            super("GENERATION_ERROR", message, cause);
        }
    }
}
