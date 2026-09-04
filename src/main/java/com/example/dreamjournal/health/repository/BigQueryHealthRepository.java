package com.example.dreamjournal.health.repository;

import com.example.dreamjournal.health.model.SleepHealthData;
import com.example.dreamjournal.health.model.SleepSessionMetrics;

import java.time.LocalDate;
import java.util.List;

public interface BigQueryHealthRepository {

    void save(
            List<SleepHealthData> healthData
    );

    List<SleepSessionMetrics> findSleepSessionMetrics(
            String userId,
            LocalDate startDate,
            LocalDate endDate
    );
}