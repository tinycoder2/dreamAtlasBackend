package com.example.dreamjournal.repository.firestore;

import com.example.dreamjournal.exception.FirestoreOperationException;
import com.example.dreamjournal.model.DayLog;
import com.example.dreamjournal.repository.DayLogRepository;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.FieldPath;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public class FirestoreDayLogRepository implements DayLogRepository {

    private final Firestore firestore;

    public FirestoreDayLogRepository(Firestore firestore) {
        this.firestore = firestore;
    }

    @Override
    public Optional<DayLog> findByUserIdAndDate(String userId, LocalDate date) {
        try {
            DocumentSnapshot snapshot = FirestorePaths.dayDocument(firestore, userId, date).get().get();
            return snapshot.exists() ? Optional.of(FirestoreMapper.toDayLog(snapshot)) : Optional.empty();
        } catch (Exception ex) {
            throw new FirestoreOperationException("Failed to read day log", ex);
        }
    }

    @Override
    public DayLog save(String userId, DayLog dayLog) {
        try {
            FirestorePaths.dayDocument(firestore, userId, dayLog.date())
                    .set(FirestoreMapper.toDocument(dayLog))
                    .get();
            return dayLog;
        } catch (Exception ex) {
            throw new FirestoreOperationException("Failed to save day log", ex);
        }
    }

    @Override
    public boolean delete(String userId, LocalDate date) {
        try {
            if (!FirestorePaths.dayDocument(firestore, userId, date).get().get().exists()) {
                return false;
            }
            FirestorePaths.dayDocument(firestore, userId, date).delete().get();
            return true;
        } catch (Exception ex) {
            throw new FirestoreOperationException("Failed to delete day log", ex);
        }
    }

    @Override
    public List<DayLog> findByUserId(String userId, LocalDate from, LocalDate to) {
        try {
            Query query = firestore.collection("users")
                    .document(userId)
                    .collection("days")
                    .orderBy(FieldPath.documentId(), Query.Direction.DESCENDING);
            if (to != null) {
                query = query.startAt(to.toString());
            }
            if (from != null) {
                query = query.endAt(from.toString());
            }
            return query.get().get().getDocuments().stream()
                    .map(FirestoreMapper::toDayLog)
                    .toList();
        } catch (Exception ex) {
            throw new FirestoreOperationException("Failed to list day logs", ex);
        }
    }
}
