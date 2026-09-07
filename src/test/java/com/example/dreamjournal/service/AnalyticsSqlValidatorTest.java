package com.example.dreamjournal.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AnalyticsSqlValidatorTest {

    private final AnalyticsSqlValidator validator =
            new AnalyticsSqlValidator();

    @Test
    void allowsApprovedSelect() {

        String sql = """
            SELECT
                user_id,
                journal_date,
                minutes_asleep
            FROM
                `project-f148f1df-8994-412a-868.dream_atlas_health.daily_dream_sleep`
            """;

        assertDoesNotThrow(
                () -> validator.validate(sql)
        );
    }

    @Test
    void rejectsInsert() {

        String sql = """
            INSERT INTO
                `project-f148f1df-8994-412a-868.dream_atlas_health.daily_dream_sleep`
            VALUES ('x')
            """;

        assertThrows(
                IllegalArgumentException.class,
                () -> validator.validate(sql)
        );
    }

    @Test
    void rejectsDelete() {

        String sql = """
            DELETE FROM
                `project-f148f1df-8994-412a-868.dream_atlas_health.daily_dream_sleep`
            WHERE user_id = 'someone'
            """;

        assertThrows(
                IllegalArgumentException.class,
                () -> validator.validate(sql)
        );
    }

    @Test
    void rejectsUnauthorizedTable() {

        String sql = """
            SELECT *
            FROM `project-f148f1df-8994-412a-868.secret_dataset.secret_table`
            """;

        assertThrows(
                IllegalArgumentException.class,
                () -> validator.validate(sql)
        );
    }

    @Test
    void rejectsInformationSchema() {

        String sql = """
            SELECT *
            FROM `project-f148f1df-8994-412a-868.region-us.INFORMATION_SCHEMA.TABLES`
            """;

        assertThrows(
                IllegalArgumentException.class,
                () -> validator.validate(sql)
        );
    }

    @Test
    void rejectsSessionUser() {

        String sql = """
            SELECT
                SESSION_USER()
            FROM
                `project-f148f1df-8994-412a-868.dream_atlas_health.daily_dream_sleep`
            """;

        assertThrows(
                IllegalArgumentException.class,
                () -> validator.validate(sql)
        );
    }

    @Test
    void rejectsCurrentUser() {

        String sql = """
            SELECT
                CURRENT_USER()
            FROM
                `project-f148f1df-8994-412a-868.dream_atlas_health.daily_dream_sleep`
            """;

        assertThrows(
                IllegalArgumentException.class,
                () -> validator.validate(sql)
        );
    }

    @Test
    void allowsNestedQueryUsingApprovedTables() {

        String sql = """
            SELECT *
            FROM (
                SELECT
                    user_id,
                    journal_date,
                    minutes_asleep
                FROM
                    `project-f148f1df-8994-412a-868.dream_atlas_health.daily_dream_sleep`
            )
            """;

        assertDoesNotThrow(
                () -> validator.validate(sql)
        );
    }

    @Test
    void rejectsNestedUnauthorizedTable() {

        String sql = """
            SELECT *
            FROM (
                SELECT *
                FROM `project-f148f1df-8994-412a-868.secret_dataset.secret_table`
            )
            """;

        assertThrows(
                IllegalArgumentException.class,
                () -> validator.validate(sql)
        );
    }

    @Test
    void rejectsMultipleStatements() {

        String sql = """
            SELECT *
            FROM `project-f148f1df-8994-412a-868.dream_atlas_health.daily_dream_sleep`;

            DELETE FROM
            `project-f148f1df-8994-412a-868.dream_atlas_health.daily_dream_sleep`
            """;

        assertThrows(
                IllegalArgumentException.class,
                () -> validator.validate(sql)
        );
    }
}