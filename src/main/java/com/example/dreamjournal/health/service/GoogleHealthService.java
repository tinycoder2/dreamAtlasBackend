package com.example.dreamjournal.health.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.client.googleapis.auth.oauth2.GoogleTokenResponse;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import com.fasterxml.jackson.databind.JsonNode;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
@Service
public class GoogleHealthService {
    private final ObjectMapper objectMapper =
            new ObjectMapper();

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

    public String getHeartRateRaw(
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
                "heart_rate.sample_time.physical_time >= \"" + start + "\""
                        + " AND "
                        + "heart_rate.sample_time.physical_time < \"" + end + "\"";

        String encodedFilter =
                URLEncoder.encode(
                        filter,
                        StandardCharsets.UTF_8
                );

        List<String> allDataPoints =
                new ArrayList<>();

        String nextPageToken = null;

        do {

            StringBuilder url =
                    new StringBuilder(
                            HEALTH_API_BASE
                                    + "/users/me/dataTypes/heart-rate/dataPoints"
                    );

            url.append("?pageSize=10000");
            url.append("&filter=").append(encodedFilter);

            if (nextPageToken != null) {
                url.append("&pageToken=")
                        .append(
                                URLEncoder.encode(
                                        nextPageToken,
                                        StandardCharsets.UTF_8
                                )
                        );
            }

            HttpRequest request =
                    HttpRequest.newBuilder()
                            .uri(URI.create(url.toString()))
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

            JsonNode root =
                    objectMapper.readTree(response.body());

            JsonNode dataPoints =
                    root.path("dataPoints");

            dataPoints.forEach(
                    point -> allDataPoints.add(
                            point.toString()
                    )
            );

            nextPageToken =
                    root.path("nextPageToken")
                            .asText(null);

        } while (nextPageToken != null &&
                !nextPageToken.isBlank());

        return objectMapper.writeValueAsString(
                Map.of(
                        "dataPoints",
                        allDataPoints.stream()
                                .map(
                                        json -> {
                                            try {
                                                return objectMapper
                                                        .readTree(json);
                                            } catch (Exception e) {
                                                throw new RuntimeException(e);
                                            }
                                        }
                                )
                                .toList()
                )
        );
    }
}