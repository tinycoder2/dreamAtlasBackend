package com.example.dreamjournal.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;

@Configuration
public class FirebaseAdminConfig {

    public FirebaseAdminConfig(
            @Value("${google.cloud.project-id}") String projectId
    ) throws IOException {

        if (FirebaseApp.getApps().isEmpty()) {
            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.getApplicationDefault())
                    .setProjectId(projectId)
                    .build();

            FirebaseApp app = FirebaseApp.initializeApp(options);

            System.out.println(
                    "Firebase Admin initialized: "
                            + app.getOptions().getProjectId()
            );
        }
    }
}