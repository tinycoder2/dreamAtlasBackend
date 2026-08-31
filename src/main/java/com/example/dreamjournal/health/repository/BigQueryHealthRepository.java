package com.example.dreamjournal.health.repository;

import com.example.dreamjournal.health.model.SleepHealthData;

import java.util.List;

public interface BigQueryHealthRepository {

    void save(
            List<SleepHealthData> healthData
    );
}