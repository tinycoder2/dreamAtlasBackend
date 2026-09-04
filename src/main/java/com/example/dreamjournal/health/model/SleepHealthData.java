package com.example.dreamjournal.health.model;

import java.util.List;

public record SleepHealthData(
        String firebaseUid,
        SleepSession sleep,
        List<HeartRateSample> heartRate
) {
}