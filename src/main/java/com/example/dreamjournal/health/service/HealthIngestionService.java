package com.example.dreamjournal.health.service;

import com.example.dreamjournal.health.model.HealthIngestionState;
import com.example.dreamjournal.health.model.HeartRateSample;
import com.example.dreamjournal.health.model.SleepHealthData;
import com.example.dreamjournal.health.model.SleepSession;
import com.example.dreamjournal.health.repository.BigQueryHealthRepository;
import com.example.dreamjournal.health.repository.HealthIngestionStateRepository;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
public class HealthIngestionService {

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

    public List<SleepHealthData> ingest(
            String firebaseUid
    ) throws Exception {

        Instant now = Instant.now();

        Instant start =
                stateRepository.find(firebaseUid)
                        .map(HealthIngestionState::lastSuccessfulRun)
                        .map(time ->
                                time.minus(SAFETY_OVERLAP)
                        )
                        .orElse(
                                now.minus(INITIAL_LOOKBACK)
                        );

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
        // ------------------------------------------

        stateRepository.save(
                new HealthIngestionState(
                        firebaseUid,
                        now
                )
        );

        return result;
    }
}