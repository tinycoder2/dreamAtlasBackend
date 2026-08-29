package com.example.dreamjournal.health.controller;

import com.example.dreamjournal.health.service.GoogleHealthOAuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.Map;

@RestController
@RequestMapping("/api/health")
public class HealthController {

    private final GoogleHealthOAuthService googleHealthOAuthService;

    public HealthController(
            GoogleHealthOAuthService googleHealthOAuthService
    ) {
        this.googleHealthOAuthService = googleHealthOAuthService;
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
}