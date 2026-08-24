package com.example.dreamjournal.dto;

import java.util.List;

public record GeminiDreamResponse(
        List<GeminiDream> dreams
) {
    public record GeminiDream(
            String text,
            String mood,
            String dreamType,
            List<String> tags
    ) {
    }
}