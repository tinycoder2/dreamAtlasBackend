package com.example.dreamjournal.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

public record DreamRequest(
        @NotBlank(message = "text must not be blank")
        @Size(max = 20000, message = "text must be at most 20000 characters")
        String text,
        String mood,
        String dreamType,
        List<String> tags,
        @Min(value = 0, message = "sortOrder must be greater than or equal to 0")
        Integer sortOrder
) {
}
