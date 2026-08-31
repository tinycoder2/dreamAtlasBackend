package com.example.dreamjournal.health.repository.bigquery;

import com.example.dreamjournal.health.model.HeartRateSample;
import com.example.dreamjournal.health.model.SleepHealthData;
import com.example.dreamjournal.health.model.SleepSession;
import com.example.dreamjournal.health.model.SleepStage;
import com.example.dreamjournal.health.repository.BigQueryHealthRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Repository
public class BigQueryHealthRepositoryImpl
        implements BigQueryHealthRepository {

    private final BigQueryStorageWriter storageWriter;

    public BigQueryHealthRepositoryImpl(
            BigQueryStorageWriter storageWriter
    ) {
        this.storageWriter = storageWriter;
    }

    @Override
    public void save(List<SleepHealthData> healthData) {

        if (healthData == null || healthData.isEmpty()) {
            return;
        }

        Instant ingestionTime = Instant.now();

        List<Map<String, Object>> sleepRows =
                buildSleepSessionRows(
                        healthData,
                        ingestionTime
                );

        List<Map<String, Object>> stageRows =
                buildSleepStageRows(
                        healthData,
                        ingestionTime
                );

        List<Map<String, Object>> heartRateRows =
                buildHeartRateRows(
                        healthData,
                        ingestionTime
                );

        try {

            storageWriter.writeJsonRows(
                    "sleep_sessions",
                    sleepRows
            );

            storageWriter.writeJsonRows(
                    "sleep_stages",
                    stageRows
            );

            storageWriter.writeJsonRows(
                    "heart_rate_samples",
                    heartRateRows
            );

        } catch (Exception e) {

            throw new IllegalStateException(
                    "Failed to write health data to BigQuery",
                    e
            );
        }
    }

    private List<Map<String, Object>> buildSleepSessionRows(
            List<SleepHealthData> healthData,
            Instant ingestionTime
    ) {

        List<Map<String, Object>> rows =
                new ArrayList<>();

        for (SleepHealthData data : healthData) {

            SleepSession sleep = data.sleep();

            Map<String, Object> row =
                    new HashMap<>();

            row.put(
                    "sleep_id",
                    sleep.id()
            );

            row.put(
                    "user_id",
                    data.firebaseUid()
            );

            row.put(
                    "start_time_utc",
                    sleep.startTimeUtc().toString()
            );

            row.put(
                    "end_time_utc",
                    sleep.endTimeUtc().toString()
            );

            row.put(
                    "start_utc_offset_seconds",
                    offsetSeconds(
                            sleep.startOffset()
                    )
            );

            row.put(
                    "end_utc_offset_seconds",
                    offsetSeconds(
                            sleep.endOffset()
                    )
            );

            row.put(
                    "timezone",
                    sleep.timezone()
            );

            row.put(
                    "local_sleep_date",
                    localSleepDate(sleep)
            );

            row.put(
                    "sleep_type",
                    sleep.type()
            );

            row.put(
                    "main_sleep",
                    sleep.mainSleep()
            );

            row.put(
                    "platform",
                    sleep.platform()
            );

            row.put(
                    "device_name",
                    sleep.deviceName()
            );

            row.put(
                    "recording_method",
                    sleep.recordingMethod()
            );

            row.put(
                    "duration_minutes",
                    sleep.durationMinutes()
            );

            row.put(
                    "minutes_asleep",
                    sleep.minutesAsleep()
            );

            row.put(
                    "minutes_awake",
                    sleep.minutesAwake()
            );

            row.put(
                    "ingested_at",
                    ingestionTime.toString()
            );

            row.put(
                    "updated_at",
                    ingestionTime.toString()
            );

            rows.add(row);
        }

        return rows;
    }

    private List<Map<String, Object>> buildSleepStageRows(
            List<SleepHealthData> healthData,
            Instant ingestionTime
    ) {

        List<Map<String, Object>> rows =
                new ArrayList<>();

        for (SleepHealthData data : healthData) {

            SleepSession sleep = data.sleep();

            if (sleep.stages() == null) {
                continue;
            }

            for (SleepStage stage : sleep.stages()) {

                String stageId =
                        HealthIdGenerator.stageId(
                                sleep.id(),
                                stage
                        );

                Map<String, Object> row =
                        new HashMap<>();

                row.put(
                        "stage_id",
                        stageId
                );

                row.put(
                        "sleep_id",
                        sleep.id()
                );

                row.put(
                        "user_id",
                        data.firebaseUid()
                );

                row.put(
                        "start_time_utc",
                        stage.startTimeUtc().toString()
                );

                row.put(
                        "end_time_utc",
                        stage.endTimeUtc().toString()
                );

                row.put(
                        "start_utc_offset_seconds",
                        offsetSeconds(
                                stage.startOffset()
                        )
                );

                row.put(
                        "end_utc_offset_seconds",
                        offsetSeconds(
                                stage.endOffset()
                        )
                );

                row.put(
                        "stage_type",
                        stage.type()
                );

                row.put(
                        "duration_minutes",
                        stage.durationMinutes()
                );

                row.put(
                        "ingested_at",
                        ingestionTime.toString()
                );

                rows.add(row);
            }
        }

        return rows;
    }

    private List<Map<String, Object>> buildHeartRateRows(
            List<SleepHealthData> healthData,
            Instant ingestionTime
    ) {

        List<Map<String, Object>> rows =
                new ArrayList<>();

        for (SleepHealthData data : healthData) {

            SleepSession sleep = data.sleep();

            if (data.heartRate() == null) {
                continue;
            }

            for (HeartRateSample sample : data.heartRate()) {

                String sampleId =
                        HealthIdGenerator.heartRateId(
                                data.firebaseUid(),
                                sample
                        );

                Map<String, Object> row =
                        new HashMap<>();

                row.put(
                        "sample_id",
                        sampleId
                );

                row.put(
                        "sleep_id",
                        sleep.id()
                );

                row.put(
                        "user_id",
                        data.firebaseUid()
                );

                row.put(
                        "timestamp_utc",
                        sample.timestampUtc().toString()
                );

                row.put(
                        "utc_offset_seconds",
                        offsetSeconds(
                                sample.utcOffset()
                        )
                );

                row.put(
                        "local_date",
                        localDate(sample)
                );

                row.put(
                        "beats_per_minute",
                        sample.beatsPerMinute()
                );

                row.put(
                        "platform",
                        sample.platform()
                );

                row.put(
                        "device_name",
                        sample.deviceName()
                );

                row.put(
                        "recording_method",
                        sample.recordingMethod()
                );

                row.put(
                        "sensor_location",
                        sample.sensorLocation()
                );

                row.put(
                        "motion_context",
                        sample.motionContext()
                );

                row.put(
                        "ingested_at",
                        ingestionTime.toString()
                );

                rows.add(row);
            }
        }

        return rows;
    }

    private int offsetSeconds(ZoneOffset offset) {

        return offset == null
                ? 0
                : offset.getTotalSeconds();
    }

    private String localSleepDate(
            SleepSession sleep
    ) {

        ZoneOffset offset =
                sleep.startOffset() != null
                        ? sleep.startOffset()
                        : ZoneOffset.UTC;

        return sleep.startTimeUtc()
                .atOffset(offset)
                .toLocalDate()
                .toString();
    }

    private String localDate(
            HeartRateSample sample
    ) {

        ZoneOffset offset =
                sample.utcOffset() != null
                        ? sample.utcOffset()
                        : ZoneOffset.UTC;

        return sample.timestampUtc()
                .atOffset(offset)
                .toLocalDate()
                .toString();
    }
}