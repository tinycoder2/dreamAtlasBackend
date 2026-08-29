package com.example.dreamjournal.health.service;

import com.example.dreamjournal.health.model.HeartRateSample;
import com.example.dreamjournal.health.model.SleepSession;
import com.example.dreamjournal.health.model.SleepStage;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

@Component
public class GoogleHealthMapper {

    public SleepSession mapSleep(JsonNode dataPoint) {

        JsonNode sleep =
                dataPoint.path("sleep");

        JsonNode interval =
                sleep.path("interval");

        Instant start =
                Instant.parse(
                        interval.path("startTime").asText()
                );

        Instant end =
                Instant.parse(
                        interval.path("endTime").asText()
                );

        ZoneOffset startOffset =
                parseOffset(
                        interval.path("startUtcOffset").asText()
                );

        ZoneOffset endOffset =
                parseOffset(
                        interval.path("endUtcOffset").asText()
                );

        List<SleepStage> stages =
                new ArrayList<>();

        for (JsonNode stage :
                sleep.path("stages")) {

            Instant stageStart =
                    Instant.parse(
                            stage.path("startTime").asText()
                    );

            Instant stageEnd =
                    Instant.parse(
                            stage.path("endTime").asText()
                    );

            ZoneOffset stageStartOffset =
                    parseOffset(
                            stage.path("startUtcOffset").asText()
                    );

            ZoneOffset stageEndOffset =
                    parseOffset(
                            stage.path("endUtcOffset").asText()
                    );

            long durationMinutes =
                    Duration.between(
                            stageStart,
                            stageEnd
                    ).toMinutes();

            stages.add(
                    new SleepStage(
                            stageStart,
                            stageEnd,
                            stageStartOffset,
                            stageEndOffset,
                            stage.path("type").asText(),
                            durationMinutes
                    )
            );
        }

        JsonNode summary =
                sleep.path("summary");

        JsonNode dataSource =
                dataPoint.path("dataSource");

        JsonNode metadata =
                sleep.path("metadata");

        String deviceName =
                dataSource
                        .path("device")
                        .path("displayName")
                        .asText(null);

        return new SleepSession(
                dataPoint.path("name").asText(),
                start,
                end,
                startOffset,
                endOffset,
                startOffset.getId(),
                sleep.path("type").asText(),
                metadata.path("mainSleep").asBoolean(false),
                dataSource.path("platform").asText(null),
                dataSource.path("recordingMethod").asText(null),
                deviceName,
                Duration.between(start, end).toMinutes(),
                parseLong(summary, "minutesAsleep"),
                parseLong(summary, "minutesAwake"),
                stages
        );
    }

    public HeartRateSample mapHeartRate(
            JsonNode dataPoint
    ) {

        JsonNode heartRate =
                dataPoint.path("heartRate");

        JsonNode sampleTime =
                heartRate.path("sampleTime");

        Instant timestamp =
                Instant.parse(
                        sampleTime
                                .path("physicalTime")
                                .asText()
                );

        ZoneOffset offset =
                parseOffset(
                        sampleTime
                                .path("utcOffset")
                                .asText()
                );

        JsonNode dataSource =
                dataPoint.path("dataSource");

        return new HeartRateSample(
                timestamp,
                offset,
                heartRate
                        .path("beatsPerMinute")
                        .asInt(),
                dataSource
                        .path("platform")
                        .asText(null),
                dataSource
                        .path("device")
                        .path("displayName")
                        .asText(null),
                dataSource
                        .path("recordingMethod")
                        .asText(null),
                heartRate
                        .path("metadata")
                        .path("sensorLocation")
                        .asText(null),
                heartRate
                        .path("metadata")
                        .path("motionContext")
                        .asText(null)
        );
    }

    private ZoneOffset parseOffset(
            String offset
    ) {

        if (offset == null ||
                offset.isBlank()) {

            return ZoneOffset.UTC;
        }

        long seconds =
                Long.parseLong(
                        offset.replace("s", "")
                );

        return ZoneOffset.ofTotalSeconds(
                (int) seconds
        );
    }

    private long parseLong(
            JsonNode node,
            String field
    ) {

        String value =
                node.path(field).asText(null);

        if (value == null ||
                value.isBlank()) {

            return 0;
        }

        return Long.parseLong(value);
    }
}