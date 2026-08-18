package com.example.dreamjournal.repository.firestore;

import com.example.dreamjournal.model.DayLog;
import com.example.dreamjournal.model.Dream;
import com.google.cloud.Timestamp;
import com.google.cloud.firestore.DocumentSnapshot;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

final class FirestoreMapper {

    private FirestoreMapper() {
    }

    static FirestoreDayLogDocument toDocument(DayLog dayLog) {
        FirestoreDayLogDocument document = new FirestoreDayLogDocument();
        document.setSleepHours(dayLog.sleepHours());
        document.setSleepQuality(dayLog.sleepQuality());
        document.setCreatedAt(toTimestamp(dayLog.createdAt()));
        document.setUpdatedAt(toTimestamp(dayLog.updatedAt()));
        return document;
    }

    static DayLog toDayLog(DocumentSnapshot snapshot) {
        FirestoreDayLogDocument document = snapshot.toObject(FirestoreDayLogDocument.class);
        return new DayLog(
                LocalDate.parse(snapshot.getId()),
                document.getSleepHours(),
                document.getSleepQuality(),
                toInstant(document.getCreatedAt()),
                toInstant(document.getUpdatedAt())
        );
    }

    static FirestoreDreamDocument toDocument(Dream dream) {
        FirestoreDreamDocument document = new FirestoreDreamDocument();
        document.setText(dream.text());
        document.setMood(dream.mood());
        document.setDreamType(dream.dreamType());
        document.setTags(dream.tags() == null ? List.of() : dream.tags());
        document.setSortOrder(dream.sortOrder());
        document.setCreatedAt(toTimestamp(dream.createdAt()));
        document.setUpdatedAt(toTimestamp(dream.updatedAt()));
        return document;
    }

    static Dream toDream(LocalDate date, DocumentSnapshot snapshot) {
        FirestoreDreamDocument document = snapshot.toObject(FirestoreDreamDocument.class);
        return new Dream(
                snapshot.getId(),
                date,
                document.getText(),
                document.getMood(),
                document.getDreamType(),
                document.getTags() == null ? List.of() : document.getTags(),
                document.getSortOrder() == null ? 0 : document.getSortOrder(),
                toInstant(document.getCreatedAt()),
                toInstant(document.getUpdatedAt())
        );
    }

    private static Timestamp toTimestamp(Instant instant) {
        return instant == null ? null : Timestamp.ofTimeSecondsAndNanos(instant.getEpochSecond(), instant.getNano());
    }

    private static Instant toInstant(Timestamp timestamp) {
        return timestamp == null ? null : Instant.ofEpochSecond(timestamp.getSeconds(), timestamp.getNanos());
    }
}
