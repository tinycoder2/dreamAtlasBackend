package com.example.dreamjournal.health.controller;

import com.example.dreamjournal.health.model.IngestionResult;
import com.example.dreamjournal.health.model.SleepHealthData;
import com.example.dreamjournal.health.service.GoogleHealthOAuthService;
import com.example.dreamjournal.health.service.GoogleHealthService;
import com.example.dreamjournal.health.service.HealthIngestionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;

@RestController
@RequestMapping("/api/health")
public class HealthController {

    private final GoogleHealthOAuthService googleHealthOAuthService;
    private final GoogleHealthService googleHealthService;
    private final HealthIngestionService healthIngestionService;
    private static final Logger log =
            LoggerFactory.getLogger(HealthController.class);
    @Value("${google.health.app-callback-uri}")
    private String googleHealthAppCallbackUri;
    public HealthController(
            GoogleHealthOAuthService googleHealthOAuthService,
            GoogleHealthService googleHealthService,
            HealthIngestionService healthIngestionService
    ) {
        this.googleHealthOAuthService = googleHealthOAuthService;
        this.googleHealthService = googleHealthService;
        this.healthIngestionService = healthIngestionService;
    }

    @GetMapping("/google/connect")
    public ResponseEntity<Void> connectGoogleHealth(
            @RequestAttribute("firebaseUid") String firebaseUid
    ) {

        String state =
                googleHealthOAuthService.createState(firebaseUid);

        String authorizationUrl =
                googleHealthOAuthService.buildAuthorizationUrl(state);

        return ResponseEntity
                .status(302)
                .location(URI.create(authorizationUrl))
                .build();
    }
    private ResponseEntity<Void> redirectToApp(String status) {

        URI redirectUri = URI.create(
                googleHealthAppCallbackUri
                        + "?status="
                        + status
        );

        return ResponseEntity
                .status(302)
                .location(redirectUri)
                .build();
    }
    @GetMapping("/google/callback")
    public ResponseEntity<Void> googleCallback(
            @RequestParam String code,
            @RequestParam String state
    ) {

        String firebaseUid =
                googleHealthOAuthService.getFirebaseUidForState(state);

        if (firebaseUid == null) {
            return redirectToApp("error");
        }

        try {

            var tokenResponse =
                    googleHealthOAuthService.exchangeCodeForTokens(code);

            // IMPORTANT:
            // Do not log either token.

            log.info(
                    "Google Health OAuth successful for Firebase UID: {}",
                    firebaseUid
            );

            log.info(
                    "Granted scopes: {}",
                    tokenResponse.getScope()
            );

            log.info(
                    "Access token expires in: {} seconds",
                    tokenResponse.getExpiresInSeconds()
            );

            boolean hasRefreshToken =
                    tokenResponse.getRefreshToken() != null;

            log.info(
                    "Refresh token received: {}",
                    hasRefreshToken
            );

            googleHealthOAuthService.storeConnection(
                    firebaseUid,
                    tokenResponse
            );

            googleHealthOAuthService.removeState(state);

            return redirectToApp("connected");

        } catch (Exception e) {

            googleHealthOAuthService.removeState(state);

            log.error(
                    "Google Health OAuth callback failed for Firebase UID: {}",
                    firebaseUid,
                    e
            );

            return redirectToApp("error");
        }
    }

    @GetMapping("/google/identity")
    public ResponseEntity<String> getGoogleHealthIdentity(
            @RequestAttribute("firebaseUid") String firebaseUid
    ) {

        try {

            String identity =
                    googleHealthService.getIdentity(firebaseUid);

            return ResponseEntity.ok(identity);

        } catch (Exception e) {

            return ResponseEntity
                    .internalServerError()
                    .body(e.getMessage());
        }
    }

    @GetMapping("/google/sleep/raw")
    public ResponseEntity<String> getRawSleep(
            @RequestAttribute("firebaseUid") String firebaseUid,
            @RequestParam String start,
            @RequestParam String end
    ) {

        try {
            String response =
                    googleHealthService.getSleepRaw(
                            firebaseUid,
                            start,
                            end
                    );

            return ResponseEntity.ok(response);

        } catch (Exception e) {

            return ResponseEntity
                    .internalServerError()
                    .body(e.getMessage());
        }
    }

    @GetMapping("/google/heart-rate/raw")
    public ResponseEntity<String> getRawHeartRate(
            @RequestAttribute("firebaseUid") String firebaseUid,
            @RequestParam String start,
            @RequestParam String end
    ) {

        try {

            String response =
                    googleHealthService.getHeartRateRaw(
                            firebaseUid,
                            start,
                            end
                    );

            return ResponseEntity.ok(response);

        } catch (Exception e) {

            return ResponseEntity
                    .internalServerError()
                    .body(e.getMessage());
        }
    }

    @GetMapping("/google/sleep-health")
    public ResponseEntity<List<SleepHealthData>> getSleepHealthData(
            @RequestAttribute("firebaseUid") String firebaseUid,
            @RequestParam String start,
            @RequestParam String end
    ) {

        try {

            List<SleepHealthData> result =
                    googleHealthService.getSleepHealthData(
                            firebaseUid,
                            Instant.parse(start),
                            Instant.parse(end)
                    );

            return ResponseEntity.ok(result);

        } catch (Exception e) {

            return ResponseEntity
                    .internalServerError()
                    .build();
        }
    }

    @PostMapping("/google/ingest")
    public ResponseEntity<IngestionResult> ingestHealthData(
            @RequestAttribute("firebaseUid") String firebaseUid
    ) {
        try {
            IngestionResult result =
                    healthIngestionService.ingest(firebaseUid);

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            log.error(
                    "Health ingestion failed for Firebase UID: {}",
                    firebaseUid,
                    e
            );
            return ResponseEntity.internalServerError().build();
        }
    }
    @GetMapping("/google/status")
    public ResponseEntity<Map<String, Object>> getGoogleHealthStatus(
            @RequestAttribute("firebaseUid") String firebaseUid
    ) {
        boolean connected =
                googleHealthOAuthService.getConnection(firebaseUid) != null;

        return ResponseEntity.ok(
                Map.of("connected", connected)
        );
    }

    @GetMapping("/google/connect-url")
    public ResponseEntity<Map<String, String>> getGoogleHealthConnectUrl(
            @RequestAttribute("firebaseUid") String firebaseUid
    ) {
        String state =
                googleHealthOAuthService.createState(firebaseUid);

        String authorizationUrl =
                googleHealthOAuthService.buildAuthorizationUrl(state);

        return ResponseEntity.ok(
                Map.of("authorizationUrl", authorizationUrl)
        );
    }
}