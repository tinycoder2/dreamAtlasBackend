package com.example.dreamjournal.dto;

import java.util.List;

public record WeeklyInsightData(
        String weeklySummary,
        int recurringThemeCount,
        int emotionalPatternCount,
        List<WeeklyTheme> themes,
        List<EmotionalPattern> emotionalPatterns
) {
}