package com.example.dreamjournal.health.controller;

import com.example.dreamjournal.health.service.GoogleHealthOAuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
    public ResponseEntity<Void> connectGoogleHealth() {

        String authorizationUrl =
                googleHealthOAuthService.buildAuthorizationUrl();

        return ResponseEntity
                .status(302)
                .location(URI.create(authorizationUrl))
                .build();
    }

    @GetMapping("/google/callback")
    public ResponseEntity<Map<String, String>> googleCallback(
            String code
    ) {

        return ResponseEntity.ok(
                Map.of(
                        "status", "received",
                        "message", "OAuth callback received"
                )
        );
    }
}