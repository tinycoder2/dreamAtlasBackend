package com.example.dreamjournal.health.model;

import java.time.Instant;
import java.util.List;

public record SleepSession(
        String id,
        Instant startTime,
        Instant endTime,
        long durationMinutes,
        List<SleepStage> stages
) {
}