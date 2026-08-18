package com.example.dreamjournal.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;

public record DayLogRequest(
        @DecimalMin(value = "0.0", message = "sleepHours must be between 0 and 24")
        @DecimalMax(value = "24.0", message = "sleepHours must be between 0 and 24")
        Double sleepHours,
        String sleepQuality
) {
}
