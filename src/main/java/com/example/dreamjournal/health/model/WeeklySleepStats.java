package com.example.dreamjournal.health.model;

public record WeeklySleepStats(
        double averageSleepMinutes,
        double averageRemMinutes,
        double averageMeanHr,
        int totalDreams,
        int vividDreams,
        int greatDreams,
        int goodDreams,
        int neutralDreams,
        int badDreams,
        int nightmares
) {}