package com.example.dreamjournal.health.repository;

import com.example.dreamjournal.health.model.HealthIngestionState;

import java.util.Optional;

public interface HealthIngestionStateRepository {

    Optional<HealthIngestionState> find(
            String firebaseUid
    );

    void save(
            HealthIngestionState state
    );
}