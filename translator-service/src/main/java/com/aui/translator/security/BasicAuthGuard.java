package com.aui.translator.security;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import jakarta.ws.rs.WebApplicationException;

public final class BasicAuthGuard {
    private BasicAuthGuard() {
    }

    public static void ensureAuthorized(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Basic ")) {
            throw new WebApplicationException("Missing basic authorization header.", 401);
        }

        String base64Credentials = authorizationHeader.substring("Basic ".length()).trim();
        String decoded = new String(Base64.getDecoder().decode(base64Credentials), StandardCharsets.UTF_8);
        String[] credentials = decoded.split(":", 2);

        if (credentials.length != 2) {
            throw new WebApplicationException("Invalid basic authorization format.", 401);
        }

        if (!expectedUser().equals(credentials[0]) || !expectedPassword().equals(credentials[1])) {
            throw new WebApplicationException("Invalid credentials.", 401);
        }
    }

    private static String expectedUser() {
        return System.getenv().getOrDefault("TRANSLATOR_BASIC_USER", "student");
    }

    private static String expectedPassword() {
        return System.getenv().getOrDefault("TRANSLATOR_BASIC_PASSWORD", "translator");
    }
}
