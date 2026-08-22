package com.example.dreamjournal.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record DreamReorderRequest(
        @NotNull
        @NotEmpty
        List<String> orderedIds
) {
}