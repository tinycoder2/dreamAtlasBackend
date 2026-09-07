package com.example.dreamjournal.dto;

import jakarta.validation.constraints.NotBlank;

public record AnalyticsChatRequest(
        @NotBlank(message = "message is required")
        String message
) {
}