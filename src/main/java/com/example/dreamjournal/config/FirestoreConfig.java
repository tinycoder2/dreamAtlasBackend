package com.example.dreamjournal.config;

import com.google.api.gax.core.NoCredentialsProvider;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.FirestoreOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.time.Clock;

@Configuration
public class FirestoreConfig {

    @Bean
    Firestore firestore(@Value("${google.cloud.project-id:}") String projectId) throws IOException {
        FirestoreOptions.Builder builder = FirestoreOptions.newBuilder();
        if (StringUtils.hasText(projectId)) {
            builder.setProjectId(projectId);
        }
        if (StringUtils.hasText(System.getenv("FIRESTORE_EMULATOR_HOST"))) {
            builder.setCredentialsProvider(NoCredentialsProvider.create());
        } else {
            builder.setCredentials(GoogleCredentials.getApplicationDefault());
        }
        return builder.build().getService();
    }

    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }
}
