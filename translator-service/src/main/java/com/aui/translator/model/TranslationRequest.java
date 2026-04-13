package com.aui.translator.model;

public record TranslationRequest(
        String text,
        String sourceLanguage,
        String targetLanguage,
        String style
) {
}
