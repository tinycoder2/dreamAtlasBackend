package com.example.dreamjournal.health.model;

public record SleepDreamPattern(
        String title,
        String description,
        String metric,
        Double value,
        String unit
) {}