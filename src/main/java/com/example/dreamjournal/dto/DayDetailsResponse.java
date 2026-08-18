package com.example.dreamjournal.dto;

import java.time.Instant;
import java.util.List;

public record DayDetailsResponse(
        String date,
        SleepDetails sleep,
        List<DreamDetails> dreams
) {
    public record SleepDetails(
            Double sleepHours,
            String sleepQuality,
            Instant createdAt,
            Instant updatedAt
    ) {
    }

    public record DreamDetails(
            String id,
            String text,
            String mood,
            String dreamType,
            List<String> tags,
            Integer sortOrder,
            Instant createdAt,
            Instant updatedAt
    ) {
    }
}
