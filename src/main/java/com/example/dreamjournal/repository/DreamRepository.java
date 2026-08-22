package com.example.dreamjournal.repository;

import com.example.dreamjournal.model.Dream;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface DreamRepository {

    Dream create(String userId, Dream dream);

    Dream save(String userId, Dream dream);

    Optional<Dream> findById(String userId, LocalDate date, String dreamId);

    List<Dream> findByUserIdAndDate(String userId, LocalDate date);

    boolean delete(String userId, LocalDate date, String dreamId);

    int deleteAllForDate(String userId, LocalDate date);
    List<Dream> reorder(
            String userId,
            LocalDate date,
            List<String> orderedIds
    );

    List<String> findRecentTags(String userId);
}
