package com.example.dreamjournal.repository;

import com.example.dreamjournal.dto.WeeklyInsightData;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;

public interface WeeklyInsightRepository {

    Optional<WeeklyInsightData> find(
            String userId,
            LocalDate startDate
    );

    void save(
            String userId,
            LocalDate startDate,
            LocalDate endDate,
            WeeklyInsightData insights,
            Instant generatedAt
    );
}