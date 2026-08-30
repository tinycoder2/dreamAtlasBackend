package com.example.dreamjournal.health.service;

import com.example.dreamjournal.health.model.HeartRateSample;
import com.example.dreamjournal.health.model.SleepSession;
import org.springframework.stereotype.Component;

@Component
public class HealthDataValidator {

    public boolean isValidSleep(
            SleepSession sleep
    ) {

        if (sleep.startTimeUtc() == null ||
                sleep.endTimeUtc() == null) {
            return false;
        }

        if (!sleep.endTimeUtc()
                .isAfter(sleep.startTimeUtc())) {
            return false;
        }

        return sleep.durationMinutes() > 0;
    }

    public boolean isValidHeartRate(
            HeartRateSample sample
    ) {

        return sample.timestampUtc() != null
                && sample.beatsPerMinute() > 0
                && sample.beatsPerMinute() < 300;
    }
}