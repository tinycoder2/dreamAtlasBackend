package com.example.dreamjournal.health.service;

import com.google.api.client.googleapis.auth.oauth2.GoogleTokenResponse;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

@Service
public class GoogleHealthService {

    private static final String HEALTH_API_BASE =
            "https://health.googleapis.com/v4";

    private final GoogleHealthOAuthService oauthService;

    private final HttpClient httpClient =
            HttpClient.newHttpClient();

    public GoogleHealthService(
            GoogleHealthOAuthService oauthService
    ) {
        this.oauthService = oauthService;
    }

    public String getIdentity(String firebaseUid)
            throws Exception {

        GoogleTokenResponse token =
                oauthService.getConnection(firebaseUid);

        if (token == null) {
            throw new IllegalStateException(
                    "Google Health is not connected"
            );
        }

        String accessToken =
                token.getAccessToken();

        HttpRequest request =
                HttpRequest.newBuilder()
                        .uri(URI.create(
                                HEALTH_API_BASE
                                        + "/users/me/identity"
                        ))
                        .header(
                                "Authorization",
                                "Bearer " + accessToken
                        )
                        .header(
                                "Accept",
                                "application/json"
                        )
                        .GET()
                        .build();

        HttpResponse<String> response =
                httpClient.send(
                        request,
                        HttpResponse.BodyHandlers.ofString()
                );

        if (response.statusCode() < 200 ||
                response.statusCode() >= 300) {

            throw new RuntimeException(
                    "Google Health API returned "
                            + response.statusCode()
                            + ": "
                            + response.body()
            );
        }

        return response.body();
    }

    public String getSleepRaw(
            String firebaseUid,
            String start,
            String end
    ) throws Exception {

        GoogleTokenResponse token =
                oauthService.getConnection(firebaseUid);

        if (token == null) {
            throw new IllegalStateException(
                    "Google Health is not connected"
            );
        }

        String filter =
                "sleep.interval.end_time >= \"" + start + "\""
                        + " AND "
                        + "sleep.interval.end_time < \"" + end + "\"";

        String encodedFilter =
                java.net.URLEncoder.encode(
                        filter,
                        java.nio.charset.StandardCharsets.UTF_8
                );

        String url =
                HEALTH_API_BASE
                        + "/users/me/dataTypes/sleep/dataPoints"
                        + "?pageSize=25"
                        + "&filter="
                        + encodedFilter;

        HttpRequest request =
                HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .header(
                                "Authorization",
                                "Bearer " + token.getAccessToken()
                        )
                        .header(
                                "Accept",
                                "application/json"
                        )
                        .GET()
                        .build();

        HttpResponse<String> response =
                httpClient.send(
                        request,
                        HttpResponse.BodyHandlers.ofString()
                );

        if (response.statusCode() < 200 ||
                response.statusCode() >= 300) {

            throw new RuntimeException(
                    "Google Health API returned "
                            + response.statusCode()
                            + ": "
                            + response.body()
            );
        }

        return response.body();
    }
}