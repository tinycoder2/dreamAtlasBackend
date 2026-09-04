package com.example.dreamjournal.health.service;

import com.example.dreamjournal.health.model.HeartRateSample;
import com.example.dreamjournal.health.model.SleepSession;
import org.springframework.stereotype.Component;

@Component
public class HealthSourcePolicy {

    public boolean includeSleep(
            SleepSession sleep
    ) {

        return "FITBIT".equalsIgnoreCase(
                sleep.platform()
        );
    }

    public boolean includeHeartRate(
            HeartRateSample sample
    ) {

        return "FITBIT".equalsIgnoreCase(
                sample.platform()
        )
                && "PASSIVELY_MEASURED".equalsIgnoreCase(
                sample.recordingMethod()
        );
    }
}