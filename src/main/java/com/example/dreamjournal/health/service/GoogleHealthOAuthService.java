package com.example.dreamjournal.health.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Service
public class GoogleHealthOAuthService {

    @Value("${google.health.client-id}")
    private String clientId;

    @Value("${google.health.redirect-uri}")
    private String redirectUri;

    private static final String AUTHORIZATION_ENDPOINT =
            "https://accounts.google.com/o/oauth2/v2/auth";

    private static final String SLEEP_SCOPE =
            "https://www.googleapis.com/auth/googlehealth.sleep.readonly";

    private static final String HEALTH_METRICS_SCOPE =
            "https://www.googleapis.com/auth/googlehealth.health_metrics_and_measurements.readonly";

    public String buildAuthorizationUrl() {

        return AUTHORIZATION_ENDPOINT
                + "?client_id=" + encode(clientId)
                + "&redirect_uri=" + encode(redirectUri)
                + "&response_type=code"
                + "&access_type=offline"
                + "&scope=" + encode(
                SLEEP_SCOPE + " " + HEALTH_METRICS_SCOPE
        )
                + "&prompt=consent";
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}