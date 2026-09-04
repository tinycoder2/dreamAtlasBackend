package com.example.dreamjournal.health.service;

import com.example.dreamjournal.health.model.*;
import com.example.dreamjournal.health.repository.BigQueryHealthRepository;
import com.example.dreamjournal.health.repository.HealthIngestionStateRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
public class HealthIngestionService {

    @Value("${health.ingestion.minimum-interval-hours:12}")
    private long minimumIntervalHours;

    private static final Duration INITIAL_LOOKBACK =
            Duration.ofDays(7);

    private static final Duration SAFETY_OVERLAP =
            Duration.ofHours(24);

    private final GoogleHealthService googleHealthService;
    private final HealthIngestionStateRepository stateRepository;
    private final HealthDataValidator validator;
    private final HealthSourcePolicy sourcePolicy;
    private final BigQueryHealthRepository bigQueryHealthRepository;

    public HealthIngestionService(
            GoogleHealthService googleHealthService,
            HealthIngestionStateRepository stateRepository,
            HealthDataValidator validator,
            HealthSourcePolicy sourcePolicy,
            BigQueryHealthRepository bigQueryHealthRepository
    ) {
        this.googleHealthService = googleHealthService;
        this.stateRepository = stateRepository;
        this.validator = validator;
        this.sourcePolicy = sourcePolicy;
        this.bigQueryHealthRepository = bigQueryHealthRepository;
    }

    public IngestionResult ingest(
            String firebaseUid
    ) throws Exception {

        Instant now = Instant.now();

        var existingState =
                stateRepository.find(firebaseUid);

        if (existingState.isPresent()) {

            Instant lastSuccessfulRun =
                    existingState.get().lastSuccessfulRun();

            Instant nextAllowedRun =
                    lastSuccessfulRun.plus(
                            Duration.ofHours(minimumIntervalHours)
                    );

            if (now.isBefore(nextAllowedRun)) {
                return new IngestionResult(
                        "skipped",
                        "INGESTION_TOO_RECENT",
                        0,
                        0,
                        lastSuccessfulRun,
                        nextAllowedRun
                );
            }
        }

        Instant start =
                existingState
                        .map(HealthIngestionState::lastSuccessfulRun)
                        .map(time -> time.minus(SAFETY_OVERLAP))
                        .orElse(now.minus(INITIAL_LOOKBACK));

        List<SleepSession> sleeps =
                googleHealthService.getSleepSessions(
                        firebaseUid,
                        start,
                        now
                );

        List<SleepHealthData> result =
                new ArrayList<>();

        for (SleepSession sleep : sleeps) {

            if (!validator.isValidSleep(sleep)) {
                continue;
            }

            if (!sourcePolicy.includeSleep(sleep)) {
                continue;
            }

            List<HeartRateSample> heartRate =
                    googleHealthService.getHeartRateForSleep(
                            firebaseUid,
                            sleep.startTimeUtc(),
                            sleep.endTimeUtc()
                    );

            List<HeartRateSample> validHeartRate =
                    heartRate.stream()
                            .filter(validator::isValidHeartRate)
                            .filter(sourcePolicy::includeHeartRate)
                            .toList();

            result.add(
                    new SleepHealthData(
                            firebaseUid,
                            sleep,
                            validHeartRate
                    )
            );
        }

        // ------------------------------------------
        // Persist first.
        // ------------------------------------------

        if (!result.isEmpty()) {
            bigQueryHealthRepository.save(result);
        }

        // ------------------------------------------
        // Only advance checkpoint after persistence
        // succeeds.
        // ------------------------------------------ß

        stateRepository.save(
                new HealthIngestionState(
                        firebaseUid,
                        now
                )
        );

        // ------------------------------------------ // Calculate next allowed ingestion // ------------------------------------------
        Instant nextAllowedRun = now.plus(Duration.ofHours(minimumIntervalHours));
        int sleepSessionCount = result.size();
        int heartRateSampleCount = result.stream().mapToInt(data -> data.heartRate().size()).sum();
        return new IngestionResult("success",
                null,
                sleepSessionCount,
                heartRateSampleCount,
                now,
                nextAllowedRun);
    }
}