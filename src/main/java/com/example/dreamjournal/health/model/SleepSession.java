package com.example.dreamjournal.health.model;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

public record SleepSession(
        String id,
        Instant startTimeUtc,
        Instant endTimeUtc,
        ZoneOffset startOffset,
        ZoneOffset endOffset,
        String timezone,
        String type,
        boolean mainSleep,
        String platform,
        String recordingMethod,
        String deviceName,
        long durationMinutes,
        long minutesAsleep,
        long minutesAwake,
        List<SleepStage> stages
) {
}