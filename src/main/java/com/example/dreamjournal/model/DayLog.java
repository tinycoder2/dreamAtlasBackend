package com.example.dreamjournal.model;

import java.time.Instant;
import java.time.LocalDate;

public record DayLog(
        LocalDate date,
        Double sleepHours,
        String sleepQuality,
        Instant createdAt,
        Instant updatedAt
) {
}
