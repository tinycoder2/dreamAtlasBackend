package com.example.dreamjournal.service;

import com.google.cloud.bigquery.*;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class UserAnalyticsService {

    private final BigQuery bigQuery;

    private static final String PROJECT_ID =
            "project-f148f1df-8994-412a-868";

    private static final String DATASET =
            "dream_atlas_health";

    public UserAnalyticsService(BigQuery bigQuery) {
        this.bigQuery = bigQuery;
    }

    public List<Map<String, Object>> executeUserQuery(
            String sql,
            String firebaseUid
    ) {
        String userScopedSql = """
        SELECT *
        FROM (
            %s
        )
        WHERE user_id = @userId
        """.formatted(sql);

        QueryJobConfiguration queryConfig =
                QueryJobConfiguration.newBuilder(userScopedSql)
                        .addNamedParameter(
                                "userId",
                                QueryParameterValue.string(firebaseUid)
                        )
                        .setUseLegacySql(false)
                        .build();

        TableResult result = runQuery(queryConfig);

        return convertRows(result);
    }
    public List<Map<String, Object>> getDailyDreamSleep(String userId) {

        String sql = """
            SELECT *
            FROM `%s.%s.daily_dream_sleep`
            WHERE user_id = @userId
            ORDER BY journal_date DESC
            LIMIT 90
            """.formatted(PROJECT_ID, DATASET);

        QueryJobConfiguration queryConfig =
                QueryJobConfiguration.newBuilder(sql)
                        .addNamedParameter(
                                "userId",
                                QueryParameterValue.string(userId)
                        )
                        .setUseLegacySql(false)
                        .build();

        TableResult result = runQuery(queryConfig);

        return convertRows(result);
    }


    private TableResult runQuery(
            QueryJobConfiguration queryConfig
    ) {
        try {
            TableResult result = bigQuery.query(queryConfig);

            return result;

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(
                    "BigQuery query was interrupted",
                    e
            );
        }
    }

    private List<Map<String, Object>> convertRows(
            TableResult result
    ) {
        List<Map<String, Object>> rows = new ArrayList<>();

        for (FieldValueList row : result.iterateAll()) {

            Map<String, Object> values =
                    new java.util.LinkedHashMap<>();

            for (Field field : result.getSchema().getFields()) {

                FieldValue value =
                        row.get(field.getName());

                values.put(
                        field.getName(),
                        value.isNull()
                                ? null
                                : value.getValue()
                );
            }

            rows.add(values);
        }

        return rows;
    }
}