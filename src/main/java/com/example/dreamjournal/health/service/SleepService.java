package com.example.dreamjournal.health.service;

import com.example.dreamjournal.health.model.*;
import com.example.dreamjournal.health.repository.BigQueryHealthRepository;
import com.example.dreamjournal.model.Dream;
import com.example.dreamjournal.service.DreamService;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class SleepService {

    private final BigQueryHealthRepository healthRepository;
    private final DreamService dreamService;

    public SleepService(
            BigQueryHealthRepository healthRepository,
            DreamService dreamService
    ) {
        this.healthRepository = healthRepository;
        this.dreamService = dreamService;
    }
    public WeeklySleepStatsResponse getWeeklyStats(
            String userId,
            LocalDate startDate,
            LocalDate endDate
    ) {
        if (endDate.isBefore(startDate)) {
            throw new IllegalArgumentException(
                    "endDate must not be before startDate"
            );
        }

        if (endDate.isAfter(startDate.plusDays(6))) {
            throw new IllegalArgumentException(
                    "Insights range must not exceed 7 days"
            );
        }

        List<DailyDreamSleep> days =
                healthRepository.findDailyDreamSleep(
                        userId,
                        startDate,
                        endDate
                );

        return new WeeklySleepStatsResponse(
                startDate,
                endDate,
                buildStats(days)
        );
    }
    public WeeklySleepResponse getWeeklySleep(
            String userId,
            LocalDate startDate,
            LocalDate endDate
    ) {

        if (endDate.isBefore(startDate)) {
            throw new IllegalArgumentException(
                    "endDate must not be before startDate"
            );
        }

        if (startDate.plusDays(6).isBefore(endDate)
                || endDate.isAfter(startDate.plusDays(6))) {
            throw new IllegalArgumentException(
                    "Sleep Tracker range must not exceed 7 days"
            );
        }

        List<SleepSessionMetrics> sessions =
                healthRepository.findSleepSessionMetrics(
                        userId,
                        startDate,
                        endDate
                );

        Map<LocalDate, SleepSessionMetrics> sleepByDate =
                sessions.stream()
                        .collect(Collectors.toMap(
                                session ->
                                        LocalDate.parse(
                                                session.localSleepDate()
                                        ),
                                Function.identity(),
                                this::chooseBestSleep
                        ));

        List<SleepDayResponse> days =
                Stream.iterate(
                                startDate,
                                date -> !date.isAfter(endDate),
                                date -> date.plusDays(1)
                        )
                        .map(date -> {

                            SleepSessionMetrics sleep =
                                    sleepByDate.get(date);

                            List<Dream> dreams = sleep == null
                                    ? List.of()
                                    : dreamService.list(
                                    userId,
                                    sleep.localWakeDate()
                            );

                            return new SleepDayResponse(
                                    date,
                                    sleep,
                                    dreams
                            );
                        })
                        .toList();

        return new WeeklySleepResponse(
                startDate,
                endDate,
                days
        );
    }

    private SleepSessionMetrics chooseBestSleep(
            SleepSessionMetrics first,
            SleepSessionMetrics second
    ) {

        if (first.mainSleep() && !second.mainSleep()) {
            return first;
        }

        if (second.mainSleep() && !first.mainSleep()) {
            return second;
        }

        return first.durationMinutes()
                >= second.durationMinutes()
                ? first
                : second;
    }


    private WeeklySleepStats buildStats(
            List<DailyDreamSleep> days
    ) {
        double averageSleepMinutes =
                days.stream()
                        .filter(day -> day.minutesAsleep() != null)
                        .mapToInt(DailyDreamSleep::minutesAsleep)
                        .average()
                        .orElse(0);

        double averageRemMinutes =
                days.stream()
                        .filter(day -> day.remMinutes() != null)
                        .mapToInt(DailyDreamSleep::remMinutes)
                        .average()
                        .orElse(0);

        double averageMeanHr =
                days.stream()
                        .filter(day -> day.meanHr() != null)
                        .mapToDouble(DailyDreamSleep::meanHr)
                        .average()
                        .orElse(0);

        int totalDreams =
                days.stream()
                        .mapToInt(DailyDreamSleep::dreamCount)
                        .sum();

        int vividDreams =
                days.stream()
                        .mapToInt(DailyDreamSleep::vividDreams)
                        .sum();

        int greatDreams =
                days.stream()
                        .mapToInt(DailyDreamSleep::greatDreams)
                        .sum();

        int goodDreams =
                days.stream()
                        .mapToInt(DailyDreamSleep::goodDreams)
                        .sum();

        int neutralDreams =
                days.stream()
                        .mapToInt(DailyDreamSleep::neutralDreams)
                        .sum();

        int badDreams =
                days.stream()
                        .mapToInt(DailyDreamSleep::badDreams)
                        .sum();

        int nightmares =
                days.stream()
                        .mapToInt(DailyDreamSleep::nightmares)
                        .sum();

        return new WeeklySleepStats(
                round(averageSleepMinutes),
                round(averageRemMinutes),
                round(averageMeanHr),
                totalDreams,
                vividDreams,
                greatDreams,
                goodDreams,
                neutralDreams,
                badDreams,
                nightmares
        );
    }

    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}