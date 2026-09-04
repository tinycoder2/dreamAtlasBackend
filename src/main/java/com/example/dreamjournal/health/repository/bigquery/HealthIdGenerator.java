package com.example.dreamjournal.health.repository.bigquery;

import com.example.dreamjournal.health.model.HeartRateSample;
import com.example.dreamjournal.health.model.SleepStage;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public final class HealthIdGenerator {

    private HealthIdGenerator() {
    }

    public static String stageId(
            String sleepId,
            SleepStage stage
    ) {
        String value =
                sleepId
                        + "|"
                        + stage.startTimeUtc()
                        + "|"
                        + stage.endTimeUtc()
                        + "|"
                        + stage.type();

        return sha256(value);
    }

    public static String heartRateId(
            String userId,
            HeartRateSample sample
    ) {
        String value =
                userId
                        + "|"
                        + sample.timestampUtc()
                        + "|"
                        + sample.platform()
                        + "|"
                        + sample.deviceName();

        return sha256(value);
    }

    private static String sha256(String value) {

        try {
            MessageDigest digest =
                    MessageDigest.getInstance("SHA-256");

            byte[] hash =
                    digest.digest(
                            value.getBytes(StandardCharsets.UTF_8)
                    );

            StringBuilder result = new StringBuilder();

            for (byte b : hash) {
                result.append(
                        String.format("%02x", b)
                );
            }

            return result.toString();

        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(
                    "SHA-256 unavailable",
                    e
            );
        }
    }
}