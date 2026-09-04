package com.example.dreamjournal.health.model;

import java.time.Instant;
import java.time.ZoneOffset;

public record SleepStage(
        Instant startTimeUtc,
        Instant endTimeUtc,
        ZoneOffset startOffset,
        ZoneOffset endOffset,
        String type,
        long durationMinutes
) {
}