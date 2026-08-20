package com.example.dreamjournal.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.FirestoreOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.time.Clock;

@Configuration
public class FirestoreConfig {

    private static final Logger logger = LoggerFactory.getLogger(FirestoreConfig.class);

    @Bean
    Firestore firestore(@Value("${google.cloud.project-id:}") String projectId,
                        @Value("${google.cloud.database-id:}") String databaseId) {
        if (!StringUtils.hasText(projectId)) {
            throw new IllegalStateException("google.cloud.project-id must be configured via GOOGLE_CLOUD_PROJECT_ID or application properties");
        }

        FirestoreOptions.Builder builder = FirestoreOptions.newBuilder();
        builder.setProjectId(projectId);
        if (StringUtils.hasText(databaseId)) {
            builder.setDatabaseId(databaseId);
        }

        String emulatorHost = System.getenv("FIRESTORE_EMULATOR_HOST");
        if (StringUtils.hasText(emulatorHost)) {
            logger.info("Connecting to Firestore Emulator at host: {}", emulatorHost);
            builder.setEmulatorHost(emulatorHost);
        } else {
            logger.info("Connecting to live GCP Firestore. Project: {}, Database: {}", projectId, databaseId);
            builder.setCredentials(loadApplicationDefaultCredentials());
        }
        return builder.build().getService();
    }

    private GoogleCredentials loadApplicationDefaultCredentials() {
        try {
            GoogleCredentials credentials = GoogleCredentials.getApplicationDefault();
            if (credentials == null) {
                throw new IllegalStateException("Google Application Default Credentials are not available. Run gcloud auth application-default login or set FIRESTORE_EMULATOR_HOST for local emulator mode.");
            }
            return credentials;
        } catch (IOException ex) {
            throw new IllegalStateException("Google Application Default Credentials are not available. Run gcloud auth application-default login or set FIRESTORE_EMULATOR_HOST for local emulator mode.", ex);
        }
    }

    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }
}
