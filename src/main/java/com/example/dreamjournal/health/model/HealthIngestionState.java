package com.example.dreamjournal.health.model;

import java.time.Instant;

public record HealthIngestionState(
        String firebaseUid,
        Instant lastSuccessfulRun
) {
}