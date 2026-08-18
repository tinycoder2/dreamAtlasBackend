package com.example.dreamjournal.service;

import com.example.dreamjournal.dto.DayDetailsResponse;
import com.example.dreamjournal.dto.DayLogRequest;
import com.example.dreamjournal.exception.ResourceNotFoundException;
import com.example.dreamjournal.model.DayLog;
import com.example.dreamjournal.model.Dream;
import com.example.dreamjournal.repository.DayLogRepository;
import com.example.dreamjournal.repository.DreamRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@Service
public class DayLogService {

    private static final Logger logger = LoggerFactory.getLogger(DayLogService.class);

    private final DayLogRepository dayLogRepository;
    private final DreamRepository dreamRepository;
    private final Clock clock;

    public DayLogService(DayLogRepository dayLogRepository, DreamRepository dreamRepository, Clock clock) {
        this.dayLogRepository = dayLogRepository;
        this.dreamRepository = dreamRepository;
        this.clock = clock;
    }

    public DayLog upsert(String userId, String dateValue, DayLogRequest request) {
        RequestGuards.requireUserId(userId);
        LocalDate date = DateParser.parse(dateValue);
        long started = System.nanoTime();
        Instant now = Instant.now(clock);
        DayLog existing = dayLogRepository.findByUserIdAndDate(userId, date).orElse(null);
        DayLog dayLog = new DayLog(
                date,
                request.sleepHours(),
                request.sleepQuality(),
                existing == null ? now : existing.createdAt(),
                now
        );
        DayLog saved = dayLogRepository.save(userId, dayLog);
        logSuccess("upsertDay", userId, date, started);
        return saved;
    }

    public DayLog get(String userId, String dateValue) {
        RequestGuards.requireUserId(userId);
        LocalDate date = DateParser.parse(dateValue);
        return dayLogRepository.findByUserIdAndDate(userId, date)
                .orElseThrow(() -> new ResourceNotFoundException("Day log not found"));
    }

    public List<DayLog> list(String userId, String fromValue, String toValue) {
        RequestGuards.requireUserId(userId);
        LocalDate from = fromValue == null ? null : DateParser.parse(fromValue);
        LocalDate to = toValue == null ? null : DateParser.parse(toValue);
        if (from != null && to != null && from.isAfter(to)) {
            throw new IllegalArgumentException("from must be on or before to");
        }
        return dayLogRepository.findByUserId(userId, from, to);
    }

    public void delete(String userId, String dateValue) {
        RequestGuards.requireUserId(userId);
        LocalDate date = DateParser.parse(dateValue);
        long started = System.nanoTime();
        if (dayLogRepository.findByUserIdAndDate(userId, date).isEmpty()) {
            throw new ResourceNotFoundException("Day log not found");
        }
        dreamRepository.deleteAllForDate(userId, date);
        dayLogRepository.delete(userId, date);
        logSuccess("deleteDay", userId, date, started);
    }

    public DayDetailsResponse details(String userId, String dateValue) {
        RequestGuards.requireUserId(userId);
        LocalDate date = DateParser.parse(dateValue);
        DayLog dayLog = dayLogRepository.findByUserIdAndDate(userId, date).orElse(null);
        List<Dream> dreams = dreamRepository.findByUserIdAndDate(userId, date);
        if (dayLog == null && dreams.isEmpty()) {
            throw new ResourceNotFoundException("Day details not found");
        }
        DayDetailsResponse.SleepDetails sleep = dayLog == null ? null : new DayDetailsResponse.SleepDetails(
                dayLog.sleepHours(),
                dayLog.sleepQuality(),
                dayLog.createdAt(),
                dayLog.updatedAt()
        );
        List<DayDetailsResponse.DreamDetails> dreamDetails = dreams.stream()
                .map(dream -> new DayDetailsResponse.DreamDetails(
                        dream.id(),
                        dream.text(),
                        dream.mood(),
                        dream.dreamType(),
                        dream.tags(),
                        dream.sortOrder(),
                        dream.createdAt(),
                        dream.updatedAt()
                ))
                .toList();
        return new DayDetailsResponse(date.toString(), sleep, dreamDetails);
    }

    private void logSuccess(String operation, String userId, LocalDate date, long started) {
        logger.info("operation={} userId={} date={} status=success durationMs={}",
                operation, userId, date, (System.nanoTime() - started) / 1_000_000);
    }
}
