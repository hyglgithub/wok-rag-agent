package com.wokrag.agent.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class AuthTokenServiceTest {

    @TempDir
    Path tempDir;

    @Test
    void testGenerateTokenCreatesFile() throws IOException {
        AuthTokenService service = new AuthTokenService();
        service.setTokenFilePath(tempDir.resolve("auth-token.txt").toString());
        service.init();

        String token = service.getToken();
        assertNotNull(token);
        assertEquals(36, token.length()); // UUID format

        String fileContent = Files.readString(tempDir.resolve("auth-token.txt"));
        assertEquals(token, fileContent.trim());
    }

    @Test
    void testValidateCorrectToken() throws IOException {
        AuthTokenService service = new AuthTokenService();
        service.setTokenFilePath(tempDir.resolve("auth-token.txt").toString());
        service.init();

        assertTrue(service.validate(service.getToken()));
    }

    @Test
    void testValidateWrongToken() throws IOException {
        AuthTokenService service = new AuthTokenService();
        service.setTokenFilePath(tempDir.resolve("auth-token.txt").toString());
        service.init();

        assertFalse(service.validate("wrong-token"));
        assertFalse(service.validate(null));
        assertFalse(service.validate(""));
    }

    @Test
    void testTokenOverwrittenOnRestart() throws IOException {
        AuthTokenService service1 = new AuthTokenService();
        service1.setTokenFilePath(tempDir.resolve("auth-token.txt").toString());
        service1.init();
        String token1 = service1.getToken();

        AuthTokenService service2 = new AuthTokenService();
        service2.setTokenFilePath(tempDir.resolve("auth-token.txt").toString());
        service2.init();
        String token2 = service2.getToken();

        assertNotEquals(token1, token2);
    }
}
