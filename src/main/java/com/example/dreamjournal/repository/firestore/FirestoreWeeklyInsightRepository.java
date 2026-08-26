package com.example.dreamjournal.repository.firestore;

import com.example.dreamjournal.dto.EmotionalPattern;
import com.example.dreamjournal.dto.WeeklyInsightData;
import com.example.dreamjournal.dto.WeeklyTheme;
import com.example.dreamjournal.exception.FirestoreOperationException;
import com.example.dreamjournal.repository.WeeklyInsightRepository;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public class FirestoreWeeklyInsightRepository
        implements WeeklyInsightRepository {

    private final Firestore firestore;

    public FirestoreWeeklyInsightRepository(Firestore firestore) {
        this.firestore = firestore;
    }

    @Override
    public Optional<WeeklyInsightData> find(
            String userId,
            LocalDate startDate
    ) {
        try {
            DocumentSnapshot snapshot =
                    FirestorePaths.weeklyInsightsCollection(
                                    firestore,
                                    userId
                            )
                            .document(startDate.toString())
                            .get()
                            .get();

            if (!snapshot.exists()) {
                return Optional.empty();
            }

            return Optional.of(toInsight(snapshot));

        } catch (Exception ex) {
            throw new FirestoreOperationException(
                    "Failed to read weekly insight",
                    ex
            );
        }
    }

    @Override
    public void save(
            String userId,
            LocalDate startDate,
            LocalDate endDate,
            WeeklyInsightData insights,
            Instant generatedAt
    ) {
        try {
            Map<String, Object> data = Map.of(
                    "startDate", startDate.toString(),
                    "endDate", endDate.toString(),
                    "weeklySummary", insights.weeklySummary(),
                    "recurringThemeCount",
                    insights.recurringThemeCount(),
                    "emotionalPatternCount",
                    insights.emotionalPatternCount(),
                    "themes", insights.themes(),
                    "emotionalPatterns", insights.emotionalPatterns(),
                    "generatedAt", generatedAt.toString()
            );

            FirestorePaths.weeklyInsightsCollection(
                            firestore,
                            userId
                    )
                    .document(startDate.toString())
                    .set(data)
                    .get();

        } catch (Exception ex) {
            throw new FirestoreOperationException(
                    "Failed to save weekly insight",
                    ex
            );
        }
    }

    @SuppressWarnings("unchecked")
    private WeeklyInsightData toInsight(
            DocumentSnapshot snapshot
    ) {
        List<WeeklyTheme> themes =
                snapshot.get("themes") == null
                        ? List.of()
                        : ((List<Map<String, Object>>) snapshot.get("themes"))
                        .stream()
                        .map(theme -> new WeeklyTheme(
                                (String) theme.get("name"),
                                ((Number) theme.get("prominence")).intValue()
                        ))
                        .toList();

        List<EmotionalPattern> emotionalPatterns =
                snapshot.get("emotionalPatterns") == null
                        ? List.of()
                        : ((List<Map<String, Object>>)
                        snapshot.get("emotionalPatterns"))
                        .stream()
                        .map(pattern -> new EmotionalPattern(
                                (String) pattern.get("pattern"),
                                (String) pattern.get("jungianConcept"),
                                (String) pattern.get("interpretation")
                        ))
                        .toList();

        return new WeeklyInsightData(
                snapshot.getString("weeklySummary"),
                snapshot.getLong("recurringThemeCount").intValue(),
                snapshot.getLong("emotionalPatternCount").intValue(),
                themes,
                emotionalPatterns
        );
    }
}