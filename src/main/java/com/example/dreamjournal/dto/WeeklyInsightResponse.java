package com.example.dreamjournal.dto;

import java.time.LocalDate;

public record WeeklyInsightResponse(
        LocalDate startDate,
        LocalDate endDate,
        boolean hasEnoughDreams,
        WeeklyInsightData insights
) {
}