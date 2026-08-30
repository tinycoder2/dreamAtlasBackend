package com.example.dreamjournal.health.model;

import java.util.List;

public record SleepHealthData(
        SleepSession sleep,
        List<HeartRateSample> heartRate
) {
}