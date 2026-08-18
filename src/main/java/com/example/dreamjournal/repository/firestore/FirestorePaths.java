package com.example.dreamjournal.repository.firestore;

import com.google.cloud.firestore.CollectionReference;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.Firestore;

import java.time.LocalDate;

final class FirestorePaths {

    private FirestorePaths() {
    }

    static DocumentReference dayDocument(Firestore firestore, String userId, LocalDate date) {
        return firestore.collection("users")
                .document(userId)
                .collection("days")
                .document(date.toString());
    }

    static CollectionReference dreamsCollection(Firestore firestore, String userId, LocalDate date) {
        return dayDocument(firestore, userId, date).collection("dreams");
    }
}
