package com.example.dreamjournal.health.model;

import java.time.Instant;

public record SleepSessionMetrics(
        String sleepId,
        Instant startTimeUtc,
        Instant endTimeUtc,
        int startUtcOffsetSeconds,
        int endUtcOffsetSeconds,
        String timezone,
        String localSleepDate,
        String localWakeDate,
        String sleepType,
        boolean mainSleep,
        String platform,
        String deviceName,
        String recordingMethod,
        long durationMinutes,
        long minutesAsleep,
        long minutesAwake,
        long stageAwakeMinutes,
        long lightMinutes,
        long deepMinutes,
        long remMinutes,
        long stageCount,
        long hrSampleCount,
        Double meanHr,
        Integer minHr,
        Integer maxHr,
        Double hrStddev
) {
}