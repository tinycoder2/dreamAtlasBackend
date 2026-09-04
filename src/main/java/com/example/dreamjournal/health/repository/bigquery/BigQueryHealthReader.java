package com.example.dreamjournal.health.repository.bigquery;

import com.example.dreamjournal.health.model.SleepSessionMetrics;
import com.google.cloud.bigquery.BigQuery;
import com.google.cloud.bigquery.FieldValueList;
import com.google.cloud.bigquery.QueryJobConfiguration;
import com.google.cloud.bigquery.QueryParameterValue;
import com.google.cloud.bigquery.TableResult;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Component
public class BigQueryHealthReader {

    private final BigQuery bigQuery;
    private final String projectId;
    private final String dataset;

    public BigQueryHealthReader(
            BigQuery bigQuery,
            @Value("${google.cloud.project-id}") String projectId,
            @Value("${google.cloud.bigquery.dataset}") String dataset
    ) {
        this.bigQuery = bigQuery;
        this.projectId = projectId;
        this.dataset = dataset;
    }

    public List<SleepSessionMetrics> findSleepSessionMetrics(
            String userId,
            LocalDate startDate,
            LocalDate endDate
    ) throws InterruptedException {

        String table =
                String.format(
                        "`%s.%s.sleep_session_metrics`",
                        projectId,
                        dataset
                );

        String sql = """
                SELECT
                  sleep_id,
                  start_time_utc,
                  end_time_utc,
                  start_utc_offset_seconds,
                  end_utc_offset_seconds,
                  timezone,
                  local_sleep_date,
                  local_wake_date,
                  sleep_type,
                  main_sleep,
                  platform,
                  device_name,
                  recording_method,
                  duration_minutes,
                  minutes_asleep,
                  minutes_awake,
                  stage_awake_minutes,
                  light_minutes,
                  deep_minutes,
                  rem_minutes,
                  stage_count,
                  hr_sample_count,
                  mean_hr,
                  min_hr,
                  max_hr,
                  hr_stddev
                FROM %s
                WHERE user_id = @userId
                  AND local_sleep_date BETWEEN @startDate AND @endDate
                ORDER BY
                  local_sleep_date ASC,
                  main_sleep DESC,
                  start_time_utc DESC
                """.formatted(table);

        QueryJobConfiguration query =
                QueryJobConfiguration.newBuilder(sql)
                        .addNamedParameter(
                                "userId",
                                QueryParameterValue.string(userId)
                        )
                        .addNamedParameter(
                                "startDate",
                                QueryParameterValue.date(
                                        startDate.toString()
                                )
                        )
                        .addNamedParameter(
                                "endDate",
                                QueryParameterValue.date(
                                        endDate.toString()
                                )
                        )
                        .build();

        TableResult result =
                bigQuery.query(query);

        List<SleepSessionMetrics> metrics =
                new ArrayList<>();

        for (FieldValueList row : result.iterateAll()) {
            metrics.add(map(row));
        }

        return metrics;
    }

    private SleepSessionMetrics map(FieldValueList row) {

        return new SleepSessionMetrics(
                row.get("sleep_id").getStringValue(),

                timestampToInstant(row, "start_time_utc"),
                timestampToInstant(row, "end_time_utc"),

                (int) row.get("start_utc_offset_seconds")
                        .getLongValue(),

                (int) row.get("end_utc_offset_seconds")
                        .getLongValue(),

                nullableString(row, "timezone"),

                row.get("local_sleep_date")
                        .getStringValue(),

                row.get("local_wake_date")
                        .getStringValue(),

                nullableString(row, "sleep_type"),

                row.get("main_sleep")
                        .getBooleanValue(),

                nullableString(row, "platform"),

                nullableString(row, "device_name"),

                nullableString(row, "recording_method"),

                row.get("duration_minutes")
                        .getLongValue(),

                row.get("minutes_asleep")
                        .getLongValue(),

                row.get("minutes_awake")
                        .getLongValue(),

                row.get("stage_awake_minutes")
                        .getLongValue(),

                row.get("light_minutes")
                        .getLongValue(),

                row.get("deep_minutes")
                        .getLongValue(),

                row.get("rem_minutes")
                        .getLongValue(),

                row.get("stage_count")
                        .getLongValue(),

                row.get("hr_sample_count")
                        .getLongValue(),

                nullableDouble(row, "mean_hr"),

                nullableInteger(row, "min_hr"),

                nullableInteger(row, "max_hr"),

                nullableDouble(row, "hr_stddev")
        );
    }
    private String nullableString(
            FieldValueList row,
            String field
    ) {
        return row.get(field).isNull()
                ? null
                : row.get(field).getStringValue();
    }

    private Double nullableDouble(
            FieldValueList row,
            String field
    ) {
        return row.get(field).isNull()
                ? null
                : row.get(field).getDoubleValue();
    }

    private Integer nullableInteger(
            FieldValueList row,
            String field
    ) {
        return row.get(field).isNull()
                ? null
                : (int) row.get(field).getLongValue();
    }

    private Instant timestampToInstant(
            FieldValueList row,
            String field
    ) {
        long micros =
                row.get(field).getTimestampValue();

        long seconds = micros / 1_000_000;
        long nanos = (micros % 1_000_000) * 1_000;

        return Instant.ofEpochSecond(
                seconds,
                nanos
        );
    }
}