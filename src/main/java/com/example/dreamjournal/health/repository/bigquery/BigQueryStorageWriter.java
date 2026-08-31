package com.example.dreamjournal.health.repository.bigquery;

import com.google.api.core.ApiFuture;
import com.google.cloud.bigquery.storage.v1.AppendRowsResponse;
import com.google.cloud.bigquery.storage.v1.BigQueryWriteClient;
import com.google.cloud.bigquery.storage.v1.JsonStreamWriter;
import com.google.cloud.bigquery.storage.v1.TableName;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class BigQueryStorageWriter {

    private final BigQueryWriteClient writeClient;
    private final String projectId;
    private final String dataset;

    public BigQueryStorageWriter(
            @Value("${google.cloud.project-id}") String projectId,
            @Value("${google.cloud.bigquery.dataset}") String dataset
    ) throws Exception {

        this.projectId = projectId;
        this.dataset = dataset;

        this.writeClient =
                BigQueryWriteClient.create();
    }

    public void writeJsonRows(
            String tableName,
            List<Map<String, Object>> rows
    ) throws Exception {

        if (rows == null || rows.isEmpty()) {
            return;
        }

        String tablePath =
                TableName.of(
                        projectId,
                        dataset,
                        tableName
                ).toString();

        try (JsonStreamWriter writer =
                     JsonStreamWriter
                             .newBuilder(
                                     tablePath,
                                     writeClient
                             )
                             .setLocation("us-south1")
                             .build()) {

            JSONArray jsonRows = new JSONArray();

            for (Map<String, Object> row : rows) {
                jsonRows.put(
                        new JSONObject(row)
                );
            }

            ApiFuture<AppendRowsResponse> future =
                    writer.append(jsonRows);

            future.get();
        }
    }
}