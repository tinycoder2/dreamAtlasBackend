package com.example.dreamjournal.health.repository.firestore;

import com.example.dreamjournal.health.model.HealthIngestionState;
import com.example.dreamjournal.health.repository.HealthIngestionStateRepository;
import com.example.dreamjournal.repository.firestore.FirestorePaths;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.Firestore;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;

@Repository
public class FirestoreHealthIngestionStateRepository
        implements HealthIngestionStateRepository {

    private final Firestore firestore;

    public FirestoreHealthIngestionStateRepository(
            Firestore firestore
    ) {
        this.firestore = firestore;
    }

    @Override
    public Optional<HealthIngestionState> find(
            String firebaseUid
    ) {

        try {

            DocumentReference document =
                    FirestorePaths.healthIngestionState(
                            firestore,
                            firebaseUid
                    );

            var snapshot =
                    document.get().get();

            if (!snapshot.exists()) {
                return Optional.empty();
            }

            String timestamp =
                    snapshot.getString(
                            "lastSuccessfulRun"
                    );

            if (timestamp == null) {
                return Optional.empty();
            }

            return Optional.of(
                    new HealthIngestionState(
                            firebaseUid,
                            Instant.parse(timestamp)
                    )
            );

        } catch (Exception e) {

            throw new RuntimeException(
                    "Failed to read health ingestion state",
                    e
            );
        }
    }

    @Override
    public void save(
            HealthIngestionState state
    ) {

        try {

            DocumentReference document =
                    FirestorePaths.healthIngestionState(
                            firestore,
                            state.firebaseUid()
                    );

            document.set(
                    java.util.Map.of(
                            "lastSuccessfulRun",
                            state.lastSuccessfulRun()
                                    .toString()
                    )
            ).get();

        } catch (Exception e) {

            throw new RuntimeException(
                    "Failed to save health ingestion state",
                    e
            );
        }
    }
}