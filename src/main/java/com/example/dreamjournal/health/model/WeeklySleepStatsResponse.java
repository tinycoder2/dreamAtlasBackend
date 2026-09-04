package com.example.dreamjournal.health.model;

import java.time.LocalDate;

public record WeeklySleepStatsResponse(
        LocalDate weekStart,
        LocalDate weekEnd,
        WeeklySleepStats stats
) {}