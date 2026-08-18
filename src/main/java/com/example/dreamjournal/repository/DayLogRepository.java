package com.example.dreamjournal.repository;

import com.example.dreamjournal.model.DayLog;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface DayLogRepository {

    Optional<DayLog> findByUserIdAndDate(String userId, LocalDate date);

    DayLog save(String userId, DayLog dayLog);

    boolean delete(String userId, LocalDate date);

    List<DayLog> findByUserId(String userId, LocalDate from, LocalDate to);
}
