package com.example.dreamjournal.health.model;

import java.time.Instant;

public record SleepStage(
        String stage,
        Instant startTime,
        Instant endTime,
        long durationMinutes
) {
}