package com.example.dreamjournal.health.model;

import java.time.Instant;
import java.time.ZoneOffset;

public record HeartRateSample(
        Instant timestampUtc,
        ZoneOffset utcOffset,
        int beatsPerMinute,
        String platform,
        String deviceName,
        String recordingMethod,
        String sensorLocation,
        String motionContext
) {
}