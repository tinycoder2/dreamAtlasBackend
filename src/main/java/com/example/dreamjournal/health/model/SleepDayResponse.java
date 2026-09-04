package com.example.dreamjournal.health.model;
import com.example.dreamjournal.model.Dream;

import java.time.LocalDate;
import java.util.List;

public record SleepDayResponse(
        LocalDate date,
        SleepSessionMetrics sleep,
        List<Dream> dreams
) {}