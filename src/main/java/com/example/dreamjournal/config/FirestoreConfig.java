package com.example.dreamjournal.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.FirestoreOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.time.Clock;

@Configuration
public class FirestoreConfig {

    @Bean
    Firestore firestore(
            @Value("${google.cloud.project-id}") String projectId,
            @Value("${google.cloud.database-id:(default)}") String databaseId
    ) throws IOException {

        FirestoreOptions options = FirestoreOptions.newBuilder()
                .setProjectId(projectId)
                .setDatabaseId(databaseId)
                .setCredentials(GoogleCredentials.getApplicationDefault())
                .build();

        return options.getService();
    }

    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }
}