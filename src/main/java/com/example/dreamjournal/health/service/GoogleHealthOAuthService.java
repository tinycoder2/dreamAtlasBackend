package com.example.dreamjournal.health.service;

import com.google.api.client.auth.oauth2.AuthorizationCodeTokenRequest;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.auth.oauth2.TokenResponse;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeTokenRequest;
import com.google.api.client.googleapis.auth.oauth2.GoogleTokenResponse;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class GoogleHealthOAuthService {
    @Value("${google.health.client-id}")
    private String clientId;

    @Value("${google.health.client-secret}")
    private String clientSecret;

    @Value("${google.health.redirect-uri}")
    private String redirectUri;

    private static final String AUTHORIZATION_ENDPOINT =
            "https://accounts.google.com/o/oauth2/v2/auth";

    private static final String TOKEN_ENDPOINT =
            "https://oauth2.googleapis.com/token";

    private static final String SLEEP_SCOPE =
            "https://www.googleapis.com/auth/googlehealth.sleep.readonly";

    private static final String HEALTH_METRICS_SCOPE =
            "https://www.googleapis.com/auth/googlehealth.health_metrics_and_measurements.readonly";

    private final NetHttpTransport httpTransport =
            new NetHttpTransport();

    private final GsonFactory jsonFactory =
            GsonFactory.getDefaultInstance();

    private final SecureRandom secureRandom =
            new SecureRandom();

    private final Map<String, String> oauthStates =
            new ConcurrentHashMap<>();

    private final Map<String, GoogleTokenResponse> connections =
            new ConcurrentHashMap<>();

    public String createState(String firebaseUid) {

        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);

        String state = Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(bytes);

        oauthStates.put(state, firebaseUid);

        return state;
    }

    public String getFirebaseUidForState(String state) {
        return oauthStates.get(state);
    }

    public void removeState(String state) {
        oauthStates.remove(state);
    }

    public String buildAuthorizationUrl(String state) {

        return AUTHORIZATION_ENDPOINT
                + "?client_id=" + encode(clientId)
                + "&redirect_uri=" + encode(redirectUri)
                + "&response_type=code"
                + "&access_type=offline"
                + "&prompt=consent"
                + "&state=" + encode(state)
                + "&scope=" + encode(
                SLEEP_SCOPE + " " + HEALTH_METRICS_SCOPE
        );
    }

    public GoogleTokenResponse exchangeCodeForTokens(String code)
            throws Exception {

        return new GoogleAuthorizationCodeTokenRequest(
                httpTransport,
                jsonFactory,
                clientId,
                clientSecret,
                code,
                redirectUri
        ).execute();
    }

    public void storeConnection(
            String firebaseUid,
            GoogleTokenResponse tokenResponse
    ) {
        connections.put(firebaseUid, tokenResponse);
    }

    public GoogleTokenResponse getConnection(String firebaseUid) {
        return connections.get(firebaseUid);
    }

    private String encode(String value) {
        return URLEncoder.encode(
                value,
                StandardCharsets.UTF_8
        );
    }
}