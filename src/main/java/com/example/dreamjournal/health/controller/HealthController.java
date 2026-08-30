package com.example.dreamjournal.health.controller;

import com.example.dreamjournal.health.model.SleepHealthData;
import com.example.dreamjournal.health.service.GoogleHealthOAuthService;
import com.example.dreamjournal.health.service.GoogleHealthService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/health")
public class HealthController {

    private final GoogleHealthOAuthService googleHealthOAuthService;
    private final GoogleHealthService googleHealthService;

    public HealthController(
            GoogleHealthOAuthService googleHealthOAuthService, GoogleHealthService googleHealthService
    ) {
        this.googleHealthOAuthService = googleHealthOAuthService;
        this.googleHealthService = googleHealthService;
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

    @GetMapping("/google/callback")
    public ResponseEntity<Map<String, String>> googleCallback(
            @RequestParam String code,
            @RequestParam String state
    ) {

        String firebaseUid =
                googleHealthOAuthService.getFirebaseUidForState(state);

        if (firebaseUid == null) {
            return ResponseEntity.badRequest().body(
                    Map.of(
                            "status", "error",
                            "message", "Invalid or expired OAuth state"
                    )
            );
        }

        try {

            var tokenResponse =
                    googleHealthOAuthService.exchangeCodeForTokens(code);

            // IMPORTANT:
            // Do not log either token.

            System.out.println(
                    "Google Health OAuth successful for Firebase UID: "
                            + firebaseUid
            );

            System.out.println(
                    "Granted scopes: "
                            + tokenResponse.getScope()
            );

            System.out.println(
                    "Access token expires in: "
                            + tokenResponse.getExpiresInSeconds()
                            + " seconds"
            );

            boolean hasRefreshToken =
                    tokenResponse.getRefreshToken() != null;

            System.out.println(
                    "Refresh token received: "
                            + hasRefreshToken
            );
            googleHealthOAuthService.storeConnection(
                    firebaseUid,
                    tokenResponse
            );

            googleHealthOAuthService.removeState(state);

            return ResponseEntity.ok(
                    Map.of(
                            "status", "connected",
                            "message", "Google Health authorization successful"
                    )
            );

        } catch (Exception e) {

            googleHealthOAuthService.removeState(state);

            return ResponseEntity.internalServerError().body(
                    Map.of(
                            "status", "error",
                            "message", "Failed to exchange authorization code"
                    )
            );
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
}