package com.example.dreamjournal.health.service;

import com.example.dreamjournal.repository.firestore.FirestorePaths;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeTokenRequest;
import com.google.api.client.googleapis.auth.oauth2.GoogleRefreshTokenRequest;
import com.google.api.client.googleapis.auth.oauth2.GoogleTokenResponse;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.SetOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.HashMap;
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

    private static final String SLEEP_SCOPE =
            "https://www.googleapis.com/auth/googlehealth.sleep.readonly";

    private static final String HEALTH_METRICS_SCOPE =
            "https://www.googleapis.com/auth/googlehealth.health_metrics_and_measurements.readonly";

    /*
     * Refresh slightly before the real expiry time.
     *
     * This avoids starting a Health API request with a token
     * that is about to expire.
     */
    private static final long TOKEN_REFRESH_BUFFER_SECONDS = 60;

    private final NetHttpTransport httpTransport =
            new NetHttpTransport();

    private final GsonFactory jsonFactory =
            GsonFactory.getDefaultInstance();

    private final SecureRandom secureRandom =
            new SecureRandom();

    /*
     * OAuth state only needs to survive between:
     *
     *   /google/connect
     *          ↓
     *   /google/callback
     *
     * The actual OAuth credentials are persisted in Firestore.
     */
    private final Map<String, String> oauthStates =
            new ConcurrentHashMap<>();

    private final Firestore firestore;

    public GoogleHealthOAuthService(Firestore firestore) {
        this.firestore = firestore;
    }

    // ==================================================
    // OAuth state
    // ==================================================

    public String createState(String firebaseUid) {

        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);

        String state =
                Base64.getUrlEncoder()
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

    // ==================================================
    // Authorization URL
    // ==================================================

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

    // ==================================================
    // Authorization code → tokens
    // ==================================================

    public GoogleTokenResponse exchangeCodeForTokens(
            String code
    ) throws Exception {

        return new GoogleAuthorizationCodeTokenRequest(
                httpTransport,
                jsonFactory,
                clientId,
                clientSecret,
                code,
                redirectUri
        ).execute();
    }

    // ==================================================
    // Store OAuth connection
    // ==================================================

    public void storeConnection(
            String firebaseUid,
            GoogleTokenResponse tokenResponse
    ) {

        try {

            DocumentReference document =
                    FirestorePaths.healthIngestionState(
                            firestore,
                            firebaseUid
                    );

            Map<String, Object> data =
                    new HashMap<>();

            /*
             * Always replace the access token.
             */
            data.put(
                    "accessToken",
                    tokenResponse.getAccessToken()
            );

            /*
             * Google may not return a refresh token on
             * subsequent authorization.
             *
             * Therefore only write it when one is present.
             *
             * SetOptions.merge() preserves an existing
             * refresh token otherwise.
             */
            if (tokenResponse.getRefreshToken() != null) {

                data.put(
                        "refreshToken",
                        tokenResponse.getRefreshToken()
                );
            }

            /*
             * Store the absolute expiration time.
             */
            if (tokenResponse.getExpiresInSeconds() != null) {

                Instant expiresAt =
                        Instant.now().plusSeconds(
                                tokenResponse.getExpiresInSeconds()
                        );

                data.put(
                        "accessTokenExpiresAt",
                        expiresAt.toString()
                );
            }

            if (tokenResponse.getScope() != null) {

                data.put(
                        "scope",
                        tokenResponse.getScope()
                );
            }

            data.put(
                    "connectedAt",
                    Instant.now().toString()
            );

            /*
             * IMPORTANT:
             *
             * Merge instead of replacing the document.
             *
             * This preserves:
             *
             *   lastSuccessfulRun
             *   refreshToken
             *   other health-ingestion fields
             */
            document.set(
                    data,
                    SetOptions.merge()
            ).get();

        } catch (Exception e) {

            throw new RuntimeException(
                    "Failed to store Google Health connection",
                    e
            );
        }
    }

    // ==================================================
    // Get a valid Google connection
    // ==================================================

    public GoogleTokenResponse getConnection(
            String firebaseUid
    ) {

        try {

            DocumentReference document =
                    FirestorePaths.healthIngestionState(
                            firestore,
                            firebaseUid
                    );

            var snapshot =
                    document.get().get();

            if (!snapshot.exists()) {
                return null;
            }

            String accessToken =
                    snapshot.getString("accessToken");

            String refreshToken =
                    snapshot.getString("refreshToken");

            String expiresAtString =
                    snapshot.getString(
                            "accessTokenExpiresAt"
                    );

            /*
             * User has not connected Google Health.
             */
            if (accessToken == null) {
                return null;
            }

            /*
             * If we don't have an expiry time, return the
             * stored token rather than unnecessarily failing.
             *
             * This also makes the code tolerant of any
             * credentials stored before expiry tracking
             * was introduced.
             */
            if (expiresAtString == null) {

                return buildTokenResponse(
                        accessToken,
                        refreshToken,
                        null
                );
            }

            Instant expiresAt =
                    Instant.parse(expiresAtString);

            Instant refreshThreshold =
                    Instant.now().plusSeconds(
                            TOKEN_REFRESH_BUFFER_SECONDS
                    );

            /*
             * Token is still valid.
             */
            if (expiresAt.isAfter(refreshThreshold)) {

                return buildTokenResponse(
                        accessToken,
                        refreshToken,
                        expiresAt
                );
            }

            /*
             * Token is expired or about to expire.
             *
             * We need the refresh token.
             */
            if (refreshToken == null) {

                throw new IllegalStateException(
                        "Google Health access token has expired " +
                                "and no refresh token is available. " +
                                "User must reconnect Google Health."
                );
            }

            /*
             * Refresh the access token.
             */
            GoogleTokenResponse refreshedToken =
                    refreshAccessToken(refreshToken);

            /*
             * Persist the newly refreshed access token.
             *
             * Do NOT overwrite the existing refresh token
             * unless Google actually gives us a new one.
             */
            saveRefreshedToken(
                    firebaseUid,
                    refreshedToken,
                    refreshToken
            );

            return refreshedToken;

        } catch (Exception e) {

            if (e instanceof IllegalStateException) {
                throw (IllegalStateException) e;
            }

            throw new RuntimeException(
                    "Failed to retrieve Google Health connection",
                    e
            );
        }
    }

    // ==================================================
    // Refresh access token
    // ==================================================

    private GoogleTokenResponse refreshAccessToken(
            String refreshToken
    ) throws Exception {

        return new GoogleRefreshTokenRequest(
                httpTransport,
                jsonFactory,
                refreshToken,
                clientId,
                clientSecret
        ).execute();
    }

    // ==================================================
    // Save refreshed access token
    // ==================================================

    private void saveRefreshedToken(
            String firebaseUid,
            GoogleTokenResponse refreshedToken,
            String existingRefreshToken
    ) throws Exception {

        DocumentReference document =
                FirestorePaths.healthIngestionState(
                        firestore,
                        firebaseUid
                );

        Map<String, Object> data =
                new HashMap<>();

        data.put(
                "accessToken",
                refreshedToken.getAccessToken()
        );

        /*
         * Usually Google does not return a new refresh token
         * during an access-token refresh.
         *
         * Preserve the existing one.
         */
        String refreshTokenToStore =
                refreshedToken.getRefreshToken() != null
                        ? refreshedToken.getRefreshToken()
                        : existingRefreshToken;

        if (refreshTokenToStore != null) {

            data.put(
                    "refreshToken",
                    refreshTokenToStore
            );
        }

        if (refreshedToken.getExpiresInSeconds() != null) {

            Instant expiresAt =
                    Instant.now().plusSeconds(
                            refreshedToken.getExpiresInSeconds()
                    );

            data.put(
                    "accessTokenExpiresAt",
                    expiresAt.toString()
            );
        }

        document.set(
                data,
                SetOptions.merge()
        ).get();
    }

    // ==================================================
    // Build GoogleTokenResponse
    // ==================================================

    private GoogleTokenResponse buildTokenResponse(
            String accessToken,
            String refreshToken,
            Instant expiresAt
    ) {

        GoogleTokenResponse token =
                new GoogleTokenResponse()
                        .setAccessToken(accessToken);

        if (refreshToken != null) {

            token.setRefreshToken(
                    refreshToken
            );
        }

        if (expiresAt != null) {

            long expiresInSeconds =
                    Math.max(
                            0,
                            expiresAt.getEpochSecond()
                                    - Instant.now()
                                    .getEpochSecond()
                    );

            token.setExpiresInSeconds(
                    expiresInSeconds
            );
        }

        return token;
    }

    // ==================================================
    // Helpers
    // ==================================================

    private String encode(String value) {

        return URLEncoder.encode(
                value,
                StandardCharsets.UTF_8
        );
    }
}