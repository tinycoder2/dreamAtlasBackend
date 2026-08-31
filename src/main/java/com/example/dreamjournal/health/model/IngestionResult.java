package com.example.dreamjournal.health.model;

import java.time.Instant;

public record IngestionResult(
        String status,
        String reason,
        int sleepSessions,
        int heartRateSamples,
        Instant lastSuccessfulRun,
        Instant nextAllowedRun
) {
}