package com.example.dreamjournal.repository.firestore;

import com.example.dreamjournal.exception.FirestoreOperationException;
import com.example.dreamjournal.model.Dream;
import com.example.dreamjournal.repository.DreamRepository;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.Query;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import com.google.cloud.firestore.WriteBatch;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.*;

@Repository
public class FirestoreDreamRepository implements DreamRepository {

    private static final int DELETE_BATCH_SIZE = 450;

    private final Firestore firestore;

    public FirestoreDreamRepository(Firestore firestore) {
        this.firestore = firestore;
    }

    @Override
    public Dream create(String userId, Dream dream) {
        try {
            DocumentReference reference = FirestorePaths.dreamsCollection(firestore, userId, dream.date()).document();
            Dream dreamWithId = new Dream(
                    reference.getId(),
                    dream.date(),
                    dream.text(),
                    dream.mood(),
                    dream.dreamType(),
                    dream.tags(),
                    dream.sortOrder(),
                    dream.createdAt(),
                    dream.updatedAt()
            );
            reference.set(FirestoreMapper.toDocument(dreamWithId)).get();
            return dreamWithId;
        } catch (Exception ex) {
            throw new FirestoreOperationException("Failed to create dream", ex);
        }
    }

    @Override
    public Dream save(String userId, Dream dream) {
        try {
            FirestorePaths.dreamsCollection(firestore, userId, dream.date())
                    .document(dream.id())
                    .set(FirestoreMapper.toDocument(dream))
                    .get();
            return dream;
        } catch (Exception ex) {
            throw new FirestoreOperationException("Failed to save dream", ex);
        }
    }

    @Override
    public Optional<Dream> findById(String userId, LocalDate date, String dreamId) {
        try {
            DocumentSnapshot snapshot = FirestorePaths.dreamsCollection(firestore, userId, date)
                    .document(dreamId)
                    .get()
                    .get();
            return snapshot.exists() ? Optional.of(FirestoreMapper.toDream(date, snapshot)) : Optional.empty();
        } catch (Exception ex) {
            throw new FirestoreOperationException("Failed to read dream", ex);
        }
    }

    @Override
    public List<Dream> findByUserIdAndDate(String userId, LocalDate date) {
        try {
            return FirestorePaths.dreamsCollection(firestore, userId, date)
                    .orderBy("sortOrder", Query.Direction.ASCENDING)
                    .orderBy("createdAt", Query.Direction.ASCENDING)
                    .get()
                    .get()
                    .getDocuments()
                    .stream()
                    .map(snapshot -> FirestoreMapper.toDream(date, snapshot))
                    .toList();
        } catch (Exception ex) {
            throw new FirestoreOperationException("Failed to list dreams", ex);
        }
    }

    @Override
    public boolean delete(String userId, LocalDate date, String dreamId) {
        try {
            DocumentReference reference = FirestorePaths.dreamsCollection(firestore, userId, date).document(dreamId);
            if (!reference.get().get().exists()) {
                return false;
            }
            reference.delete().get();
            return true;
        } catch (Exception ex) {
            throw new FirestoreOperationException("Failed to delete dream", ex);
        }
    }

    @Override
    public int deleteAllForDate(String userId, LocalDate date) {
        int deleted = 0;
        try {
            while (true) {
                List<QueryDocumentSnapshot> documents = FirestorePaths.dreamsCollection(firestore, userId, date)
                        .limit(DELETE_BATCH_SIZE)
                        .get()
                        .get()
                        .getDocuments();
                if (documents.isEmpty()) {
                    return deleted;
                }
                WriteBatch batch = firestore.batch();
                documents.forEach(document -> batch.delete(document.getReference()));
                batch.commit().get();
                deleted += documents.size();
            }
        } catch (Exception ex) {
            throw new FirestoreOperationException("Failed to delete dreams", ex);
        }
    }
    @Override
    public List<Dream> reorder(
            String userId,
            LocalDate date,
            List<String> orderedIds
    ) {
        try {
            var collection = FirestorePaths.dreamsCollection(
                    firestore,
                    userId,
                    date
            );

            WriteBatch batch = firestore.batch();

            for (int i = 0; i < orderedIds.size(); i++) {
                String dreamId = orderedIds.get(i);

                DocumentReference reference = collection.document(dreamId);

                batch.update(
                        reference,
                        "sortOrder",
                        i,
                        "updatedAt",
                        com.google.cloud.Timestamp.now()
                );
            }

            batch.commit().get();

            return findByUserIdAndDate(userId, date);

        } catch (Exception ex) {
            throw new FirestoreOperationException(
                    "Failed to reorder dreams",
                    ex
            );
        }
    }

    @Override
    public List<String> findRecentTags(String userId) {
        try {
            var query = firestore.collectionGroup("dreams")
                    .orderBy("createdAt", Query.Direction.DESCENDING);

            var snapshots = query.get().get();

            Set<String> tags = new LinkedHashSet<>();

            for (var document : snapshots.getDocuments()) {
                // Dream document:
                // users/{userId}/days/{date}/dreams/{dreamId}
                String path = document.getReference().getPath();

                String[] parts = path.split("/");

                if (parts.length != 6) {
                    continue;
                }

                String documentUserId = parts[1];

                if (!documentUserId.equals(userId)) {
                    continue;
                }

                List<String> dreamTags = document.toObject(
                        FirestoreDreamDocument.class
                ).getTags();

                if (dreamTags == null) {
                    continue;
                }

                for (String tag : dreamTags) {
                    if (tag != null && !tag.isBlank()) {
                        tags.add(tag);
                    }
                }
            }

            return new ArrayList<>(tags);

        } catch (Exception ex) {
            throw new FirestoreOperationException(
                    "Failed to get recent tags",
                    ex
            );
        }
    }
}
