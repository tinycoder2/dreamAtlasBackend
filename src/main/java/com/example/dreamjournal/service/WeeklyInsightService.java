package com.example.dreamjournal.service;

import com.example.dreamjournal.dto.GeminiWeeklyInsightResponse;
import com.example.dreamjournal.dto.WeeklyInsightData;
import com.example.dreamjournal.dto.WeeklyInsightResponse;
import com.example.dreamjournal.dto.WeeklyTheme;
import com.example.dreamjournal.model.Dream;
import com.example.dreamjournal.repository.DreamRepository;
import com.example.dreamjournal.repository.WeeklyInsightRepository;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class WeeklyInsightService {

    private static final int MINIMUM_DREAMS = 3;

    private final DreamRepository dreamRepository;
    private final GeminiService geminiService;

    private final WeeklyInsightRepository weeklyInsightRepository;
    private final Clock clock;

    public WeeklyInsightService(
            DreamRepository dreamRepository,
            WeeklyInsightRepository weeklyInsightRepository,
            GeminiService geminiService,
            Clock clock
    ) {
        this.dreamRepository = dreamRepository;
        this.weeklyInsightRepository = weeklyInsightRepository;
        this.geminiService = geminiService;
        this.clock = clock;
    }

    public WeeklyInsightResponse getWeeklyInsights(
            String userId,
            LocalDate startDate
    ) {
        RequestGuards.requireUserId(userId);

        LocalDate endDate = startDate.plusDays(6);

        List<Dream> dreams =
                getDreamsForWeek(userId, startDate, endDate);

        if (dreams.size() < MINIMUM_DREAMS) {
            return new WeeklyInsightResponse(
                    startDate,
                    endDate,
                    false,
                    null
            );
        }

        Optional<WeeklyInsightData> cached =
                weeklyInsightRepository.find(userId, startDate);

        if (cached.isPresent()) {
            return new WeeklyInsightResponse(
                    startDate,
                    endDate,
                    true,
                    cached.get()
            );
        }

        return generateAndSave(
                userId,
                startDate,
                endDate,
                dreams
        );
    }

    public WeeklyInsightResponse refreshWeeklyInsights(
            String userId,
            LocalDate startDate
    ) {
        RequestGuards.requireUserId(userId);

        LocalDate endDate = startDate.plusDays(6);

        List<Dream> dreams =
                getDreamsForWeek(userId, startDate, endDate);

        if (dreams.size() < MINIMUM_DREAMS) {
            return new WeeklyInsightResponse(
                    startDate,
                    endDate,
                    false,
                    null
            );
        }

        // Deliberately ignores the cached insight.
        return generateAndSave(
                userId,
                startDate,
                endDate,
                dreams
        );
    }

    private WeeklyInsightResponse generateAndSave(
            String userId,
            LocalDate startDate,
            LocalDate endDate,
            List<Dream> dreams
    ) {
        GeminiWeeklyInsightResponse generated =
                geminiService.generateWeeklyInsights(dreams);

        validateInsight(generated);

        WeeklyInsightData insights =
                new WeeklyInsightData(
                        generated.weeklySummary(),
                        generated.themes().size(),
                        generated.emotionalPatterns().size(),
                        generated.themes(),
                        generated.emotionalPatterns()
                );

        weeklyInsightRepository.save(
                userId,
                startDate,
                endDate,
                insights,
                Instant.now(clock)
        );

        return new WeeklyInsightResponse(
                startDate,
                endDate,
                true,
                insights
        );
    }
    private List<Dream> getDreamsForWeek(
            String userId,
            LocalDate startDate,
            LocalDate endDate
    ) {
        List<Dream> dreams = new ArrayList<>();

        LocalDate current = startDate;

        while (!current.isAfter(endDate)) {
            dreams.addAll(
                    dreamRepository.findByUserIdAndDate(
                            userId,
                            current
                    )
            );

            current = current.plusDays(1);
        }

        return dreams;
    }

    private void validateInsight(GeminiWeeklyInsightResponse insight) {
        if (insight == null) {
            throw new IllegalStateException("Gemini returned no weekly insight");
        }

        if (insight.weeklySummary() == null
                || insight.weeklySummary().isBlank()) {
            throw new IllegalStateException("Gemini returned no weekly summary");
        }

        if (insight.themes() == null
                || insight.themes().isEmpty()
                || insight.themes().size() > 5) {
            throw new IllegalStateException(
                    "Gemini returned an invalid number of themes"
            );
        }

        if (insight.emotionalPatterns() == null
                || insight.emotionalPatterns().size() > 3) {
            throw new IllegalStateException(
                    "Gemini returned an invalid number of emotional patterns"
            );
        }

        for (WeeklyTheme theme : insight.themes()) {
            if (theme.prominence() < 0 || theme.prominence() > 100) {
                throw new IllegalStateException(
                        "Theme prominence must be between 0 and 100"
                );
            }
        }
    }
}