package com.example.dreamjournal.service;

import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.regex.Pattern;

@Service
public class AnalyticsSqlValidator {

    private static final String PROJECT =
            "project-f148f1df-8994-412a-868";

    private static final String DATASET =
            "dream_atlas_health";

    private static final Pattern ALLOWED_TABLE =
            Pattern.compile(
                    "`" + Pattern.quote(PROJECT)
                            + "\\."
                            + Pattern.quote(DATASET)
                            + "\\."
                            + "(dreams_analytical|daily_dream_sleep|sleep_session_metrics|dreams_for_ai)"
                            + "`",
                    Pattern.CASE_INSENSITIVE
            );

    private static final Pattern FORBIDDEN_OPERATION =
            Pattern.compile(
                    "\\b("
                            + "insert|update|delete|merge|create|drop|alter|truncate|"
                            + "grant|revoke|call|execute|replace|export|"
                            + "declare|set"
                            + ")\\b",
                    Pattern.CASE_INSENSITIVE
            );

    private static final Pattern FORBIDDEN_SOURCE =
            Pattern.compile(
                    "("
                            + "information_schema"
                            + "|session_user"
                            + "|current_user"
                            + "|system\\.\\w+"
                            + ")",
                    Pattern.CASE_INSENSITIVE
            );

    private static final Pattern MULTI_STATEMENT =
            Pattern.compile(";\\s*\\S", Pattern.DOTALL);

    public void validate(String sql) {

        if (sql == null || sql.isBlank()) {
            throw new IllegalArgumentException(
                    "Gemini returned empty SQL"
            );
        }

        String normalized =
                sql.trim()
                        .toLowerCase(Locale.ROOT);

        /*
         * 1. Must start with SELECT.
         */
        if (!normalized.startsWith("select ")) {
            throw new IllegalArgumentException(
                    "Only SELECT queries are allowed"
            );
        }

        /*
         * 2. Only one SQL statement.
         *
         * A single trailing semicolon is allowed.
         */
        String withoutTrailingSemicolon =
                normalized.endsWith(";")
                        ? normalized.substring(
                        0,
                        normalized.length() - 1
                ).trim()
                        : normalized;

        if (MULTI_STATEMENT.matcher(
                withoutTrailingSemicolon
        ).find()) {
            throw new IllegalArgumentException(
                    "Multiple SQL statements are not allowed"
            );
        }

        /*
         * 3. Block mutation, DDL and scripting.
         */
        if (FORBIDDEN_OPERATION.matcher(normalized).find()) {
            throw new IllegalArgumentException(
                    "Forbidden SQL operation"
            );
        }

        /*
         * 4. Block system / identity-based access.
         */
        if (FORBIDDEN_SOURCE.matcher(normalized).find()) {
            throw new IllegalArgumentException(
                    "Forbidden SQL source or identity function"
            );
        }

        /*
         * 5. Query must reference at least one approved table.
         */
        if (!ALLOWED_TABLE.matcher(sql).find()) {
            throw new IllegalArgumentException(
                    "Query references an unauthorized data source"
            );
        }

        /*
         * 6. Reject references to other projects/datasets.
         *
         * Gemini must only access the Dream Atlas dataset.
         */
        Pattern anyQualifiedTable =
                Pattern.compile(
                        "`[^`]+\\.[^`]+\\.[^`]+`",
                        Pattern.CASE_INSENSITIVE
                );

        var matches =
                anyQualifiedTable.matcher(sql);

        while (matches.find()) {

            String tableReference =
                    matches.group();

            if (!ALLOWED_TABLE.matcher(
                    tableReference
            ).matches()) {

                throw new IllegalArgumentException(
                        "Query references an unauthorized table: "
                                + tableReference
                );
            }
        }
    }
}