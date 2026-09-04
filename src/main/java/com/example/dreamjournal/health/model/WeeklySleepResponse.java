package com.example.dreamjournal.health.model;

import java.time.LocalDate;
import java.util.List;

public record WeeklySleepResponse(
        LocalDate weekStart,
        LocalDate weekEnd,
        List<SleepDayResponse> days
) {}