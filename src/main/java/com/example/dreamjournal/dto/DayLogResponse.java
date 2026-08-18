package com.example.dreamjournal.dto;

import com.example.dreamjournal.model.DayLog;

import java.time.Instant;

public record DayLogResponse(
        String date,
        Double sleepHours,
        String sleepQuality,
        Instant createdAt,
        Instant updatedAt
) {
    public static DayLogResponse from(DayLog dayLog) {
        return new DayLogResponse(
                dayLog.date().toString(),
                dayLog.sleepHours(),
                dayLog.sleepQuality(),
                dayLog.createdAt(),
                dayLog.updatedAt()
        );
    }
}
