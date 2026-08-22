package com.example.dreamjournal.service;

import com.example.dreamjournal.dto.DreamRequest;
import com.example.dreamjournal.exception.ResourceNotFoundException;
import com.example.dreamjournal.model.Dream;
import com.example.dreamjournal.repository.DreamRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;

@Service
public class DreamService {

    private static final Logger logger = LoggerFactory.getLogger(DreamService.class);

    private final DreamRepository dreamRepository;
    private final Clock clock;

    public DreamService(DreamRepository dreamRepository, Clock clock) {
        this.dreamRepository = dreamRepository;
        this.clock = clock;
    }

    public Dream create(String userId, String dateValue, DreamRequest request) {
        RequestGuards.requireUserId(userId);
        LocalDate date = DateParser.parse(dateValue);
        long started = System.nanoTime();
        Instant now = Instant.now(clock);
        Dream dream = new Dream(
                null,
                date,
                request.text().trim(),
                request.mood(),
                request.dreamType(),
                normalizeTags(request.tags()),
                request.sortOrder() == null ? 0 : request.sortOrder(),
                now,
                now
        );
        Dream saved = dreamRepository.create(userId, dream);
        logSuccess("createDream", userId, date, saved.id(), started);
        return saved;
    }

    public Dream get(String userId, String dateValue, String dreamId) {
        RequestGuards.requireUserId(userId);
        RequestGuards.requireDreamId(dreamId);
        LocalDate date = DateParser.parse(dateValue);
        return dreamRepository.findById(userId, date, dreamId)
                .orElseThrow(() -> new ResourceNotFoundException("Dream not found"));
    }

    public Dream update(String userId, String dateValue, String dreamId, DreamRequest request) {
        RequestGuards.requireUserId(userId);
        RequestGuards.requireDreamId(dreamId);
        LocalDate date = DateParser.parse(dateValue);
        long started = System.nanoTime();
        Dream existing = dreamRepository.findById(userId, date, dreamId)
                .orElseThrow(() -> new ResourceNotFoundException("Dream not found"));
        Dream updated = new Dream(
                existing.id(),
                existing.date(),
                request.text().trim(),
                request.mood(),
                request.dreamType(),
                normalizeTags(request.tags()),
                request.sortOrder() == null ? 0 : request.sortOrder(),
                existing.createdAt(),
                Instant.now(clock)
        );
        Dream saved = dreamRepository.save(userId, updated);
        logSuccess("updateDream", userId, date, dreamId, started);
        return saved;
    }

    public void delete(String userId, String dateValue, String dreamId) {
        RequestGuards.requireUserId(userId);
        RequestGuards.requireDreamId(dreamId);
        LocalDate date = DateParser.parse(dateValue);
        long started = System.nanoTime();
        if (!dreamRepository.delete(userId, date, dreamId)) {
            throw new ResourceNotFoundException("Dream not found");
        }
        logSuccess("deleteDream", userId, date, dreamId, started);
    }

    public List<Dream> list(String userId, String dateValue) {
        RequestGuards.requireUserId(userId);
        LocalDate date = DateParser.parse(dateValue);
        return dreamRepository.findByUserIdAndDate(userId, date).stream()
                .sorted(Comparator.comparing(Dream::sortOrder).thenComparing(Dream::createdAt))
                .toList();
    }

    public List<Dream> reorder(
            String userId,
            String dateValue,
            List<String> orderedIds
    ) {
        RequestGuards.requireUserId(userId);

        LocalDate date = DateParser.parse(dateValue);

        if (orderedIds == null || orderedIds.isEmpty()) {
            throw new IllegalArgumentException("orderedIds must not be empty");
        }

        // Make sure there are no duplicate IDs.
        Set<String> uniqueIds = new HashSet<>(orderedIds);

        if (uniqueIds.size() != orderedIds.size()) {
            throw new IllegalArgumentException(
                    "orderedIds must not contain duplicates"
            );
        }

        // Make sure all supplied IDs actually belong to this date.
        List<Dream> existingDreams =
                dreamRepository.findByUserIdAndDate(userId, date);

        Set<String> existingIds = existingDreams.stream()
                .map(Dream::id)
                .collect(java.util.stream.Collectors.toSet());

        if (!existingIds.equals(uniqueIds)) {
            throw new IllegalArgumentException(
                    "orderedIds must contain exactly the dreams for this date"
            );
        }

        return dreamRepository.reorder(
                userId,
                date,
                orderedIds
        );
    }

    public List<String> findRecentTags(String userId) {
        RequestGuards.requireUserId(userId);

        return dreamRepository.findRecentTags(userId);
    }

    private List<String> normalizeTags(List<String> tags) {
        if (tags == null) {
            return List.of();
        }
        LinkedHashSet<String> unique = new LinkedHashSet<>();
        for (String tag : tags) {
            if (tag != null) {
                unique.add(tag);
            }
        }
        return new ArrayList<>(unique);
    }

    private void logSuccess(String operation, String userId, LocalDate date, String dreamId, long started) {
        logger.info("operation={} userId={} date={} dreamId={} status=success durationMs={}",
                operation, userId, date, dreamId, (System.nanoTime() - started) / 1_000_000);
    }
}
