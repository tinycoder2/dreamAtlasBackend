package com.example.dreamjournal.health.repository.bigquery;

public final class BigQuerySql {

    private BigQuerySql() {
    }

    public static String escape(String value) {

        if (value == null) {
            return null;
        }

        return value
                .replace("\\", "\\\\")
                .replace("'", "\\'");
    }
}