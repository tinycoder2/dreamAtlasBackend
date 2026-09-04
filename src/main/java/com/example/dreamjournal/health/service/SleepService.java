package com.example.dreamjournal.health.service;

import com.example.dreamjournal.health.model.SleepDayResponse;
import com.example.dreamjournal.health.model.SleepSessionMetrics;
import com.example.dreamjournal.health.model.WeeklySleepResponse;
import com.example.dreamjournal.health.repository.BigQueryHealthRepository;
import com.example.dreamjournal.model.Dream;
import com.example.dreamjournal.service.DreamService;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Comparator;
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
}