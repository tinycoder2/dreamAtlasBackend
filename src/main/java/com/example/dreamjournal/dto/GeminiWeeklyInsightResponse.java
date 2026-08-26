package com.example.dreamjournal.dto;

import java.util.List;

public record GeminiWeeklyInsightResponse(
        String weeklySummary,
        List<WeeklyTheme> themes,
        List<EmotionalPattern> emotionalPatterns
) {
}