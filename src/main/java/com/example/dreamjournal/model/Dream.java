package com.example.dreamjournal.model;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record Dream(
        String id,
        LocalDate date,
        String text,
        String mood,
        String dreamType,
        List<String> tags,
        Integer sortOrder,
        Instant createdAt,
        Instant updatedAt
) {
}
