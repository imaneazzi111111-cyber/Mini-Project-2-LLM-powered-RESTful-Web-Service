package com.aui.translator.service;

import com.aui.translator.model.TranslationRequest;
import com.aui.translator.model.TranslationResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;

public class TranslationService {
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(60);

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(20))
            .build();

    public TranslationResponse translate(TranslationRequest request) {
        String sourceLanguage = pickOrDefault(request.sourceLanguage(), "English");
        String targetLanguage = pickOrDefault(request.targetLanguage(), "Darija");
        String style = pickOrDefault(request.style(), "natural");
        String translatedText = translateWithGemini(request.text(), sourceLanguage, targetLanguage, style);

        return new TranslationResponse(
                request.text(),
                translatedText,
                sourceLanguage,
                targetLanguage,
                activeModel(),
                "success"
        );
    }

    private String pickOrDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private String translateWithGemini(String text, String sourceLanguage, String targetLanguage, String style) {
        String apiKey = System.getenv("GEMINI_API_KEY");
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("GEMINI_API_KEY is missing. Export it before starting the translator service.");
        }

        try {
            String requestBody = objectMapper.writeValueAsString(
                    new GeminiGenerateRequest(
                            new GeminiInstruction(List.of(new GeminiPart(buildInstruction(sourceLanguage, targetLanguage, style)))),
                            List.of(new GeminiContent("user", List.of(new GeminiPart(text)))),
                            new GeminiGenerationConfig(0.25)
                    )
            );

            HttpRequest request = HttpRequest.newBuilder(activeApiUri())
                    .timeout(REQUEST_TIMEOUT)
                    .header("Content-Type", "application/json")
                    .header("x-goog-api-key", apiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() >= 400) {
                throw new RuntimeException(extractGeminiErrorMessage(response.body(), response.statusCode()));
            }

            return extractGeminiText(response.body());
        } catch (IOException exception) {
            throw new RuntimeException("Failed to serialize or parse the Gemini response.", exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("The Gemini request was interrupted.", exception);
        }
    }

    private String extractGeminiText(String rawResponse) throws IOException {
        JsonNode root = objectMapper.readTree(rawResponse);
        StringBuilder builder = new StringBuilder();
        JsonNode candidates = root.path("candidates");

        if (candidates.isArray()) {
            for (JsonNode candidate : candidates) {
                JsonNode parts = candidate.path("content").path("parts");
                if (!parts.isArray()) {
                    continue;
                }

                for (JsonNode part : parts) {
                    String text = part.path("text").asText("");
                    if (!text.isBlank()) {
                        if (builder.length() > 0) {
                            builder.append('\n');
                        }
                        builder.append(text);
                    }
                }
            }
        }

        String translatedText = builder.toString().trim();
        if (translatedText.isEmpty()) {
            throw new RuntimeException("The LLM response did not include translated text.");
        }

        return translatedText;
    }

    private String extractGeminiErrorMessage(String rawResponse, int statusCode) throws IOException {
        JsonNode root = objectMapper.readTree(rawResponse);
        String apiMessage = root.path("error").path("message").asText();

        if (apiMessage == null || apiMessage.isBlank()) {
            return "Gemini request failed with status " + statusCode + ".";
        }

        return apiMessage;
    }

    private String buildInstruction(String sourceLanguage, String targetLanguage, String style) {
        return """
                You are a translation assistant for a university project.
                Translate the user's text from %s to Moroccan Arabic Dialect (%s).
                Style: %s.
                Return only the translated text with no explanation, no markdown, and no quotation marks.
                If the user text already contains Darija, improve it while preserving meaning.
                """.formatted(sourceLanguage, targetLanguage, style);
    }

    private URI activeApiUri() {
        String model = activeModel();
        return URI.create(
                System.getenv().getOrDefault(
                        "GEMINI_API_URL",
                        "https://generativelanguage.googleapis.com/v1beta/models/" + model + ":generateContent"
                )
        );
    }

    private String activeModel() {
        return System.getenv().getOrDefault("GEMINI_MODEL", "gemini-2.5-flash");
    }

    private record GeminiGenerateRequest(
            GeminiInstruction system_instruction,
            List<GeminiContent> contents,
            GeminiGenerationConfig generationConfig
    ) {
    }

    private record GeminiInstruction(
            List<GeminiPart> parts
    ) {
    }

    private record GeminiContent(
            String role,
            List<GeminiPart> parts
    ) {
    }

    private record GeminiPart(
            String text
    ) {
    }

    private record GeminiGenerationConfig(
            double temperature
    ) {
    }
}
