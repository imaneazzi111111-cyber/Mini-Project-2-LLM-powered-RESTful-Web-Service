package com.aui.translator.model;

public record TranslationResponse(
        String sourceText,
        String translatedText,
        String sourceLanguage,
        String targetLanguage,
        String provider,
        String status
) {
}
