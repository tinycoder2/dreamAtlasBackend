package com.example.dreamjournal.service;

import com.example.dreamjournal.dto.DayLogRequest;
import com.example.dreamjournal.exception.ResourceNotFoundException;
import com.example.dreamjournal.model.DayLog;
import com.example.dreamjournal.repository.DayLogRepository;
import com.example.dreamjournal.repository.DreamRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DayLogServiceTest {

    @Mock
    private DayLogRepository dayLogRepository;

    @Mock
    private DreamRepository dreamRepository;

    private DayLogService service;
    private final Instant now = Instant.parse("2026-08-18T08:10:00Z");

    @BeforeEach
    void setUp() {
        service = new DayLogService(dayLogRepository, dreamRepository, Clock.fixed(now, ZoneOffset.UTC));
    }

    @Test
    void createsNewDay() {
        LocalDate date = LocalDate.parse("2026-08-18");
        when(dayLogRepository.findByUserIdAndDate("user-1", date)).thenReturn(Optional.empty());
        when(dayLogRepository.save(org.mockito.ArgumentMatchers.eq("user-1"), org.mockito.ArgumentMatchers.any()))
                .thenAnswer(invocation -> invocation.getArgument(1));

        DayLog result = service.upsert("user-1", "2026-08-18", new DayLogRequest(7.5, "GOOD"));

        assertThat(result.createdAt()).isEqualTo(now);
        assertThat(result.updatedAt()).isEqualTo(now);
        assertThat(result.sleepHours()).isEqualTo(7.5);
    }

    @Test
    void updatesExistingDayAndPreservesCreatedAt() {
        LocalDate date = LocalDate.parse("2026-08-18");
        Instant createdAt = Instant.parse("2026-08-18T07:30:00Z");
        when(dayLogRepository.findByUserIdAndDate("user-1", date))
                .thenReturn(Optional.of(new DayLog(date, 6.0, "OK", createdAt, createdAt)));
        when(dayLogRepository.save(org.mockito.ArgumentMatchers.eq("user-1"), org.mockito.ArgumentMatchers.any()))
                .thenAnswer(invocation -> invocation.getArgument(1));

        DayLog result = service.upsert("user-1", "2026-08-18", new DayLogRequest(8.0, "GREAT"));

        assertThat(result.createdAt()).isEqualTo(createdAt);
        assertThat(result.updatedAt()).isEqualTo(now);
        assertThat(result.sleepQuality()).isEqualTo("GREAT");
    }

    @Test
    void getsExistingDay() {
        LocalDate date = LocalDate.parse("2026-08-18");
        DayLog dayLog = new DayLog(date, 7.5, "GOOD", now, now);
        when(dayLogRepository.findByUserIdAndDate("user-1", date)).thenReturn(Optional.of(dayLog));

        assertThat(service.get("user-1", "2026-08-18")).isEqualTo(dayLog);
    }

    @Test
    void missingDayThrowsNotFound() {
        when(dayLogRepository.findByUserIdAndDate("user-1", LocalDate.parse("2026-08-18"))).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.get("user-1", "2026-08-18"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void deletesDayAndDreams() {
        LocalDate date = LocalDate.parse("2026-08-18");
        when(dayLogRepository.findByUserIdAndDate("user-1", date))
                .thenReturn(Optional.of(new DayLog(date, 7.5, "GOOD", now, now)));

        service.delete("user-1", "2026-08-18");

        verify(dreamRepository).deleteAllForDate("user-1", date);
        verify(dayLogRepository).delete("user-1", date);
    }

    @Test
    void detailsReturnsDreamsWhenSleepIsMissing() {
        LocalDate date = LocalDate.parse("2026-08-18");
        when(dayLogRepository.findByUserIdAndDate("user-1", date)).thenReturn(Optional.empty());
        when(dreamRepository.findByUserIdAndDate("user-1", date))
                .thenReturn(List.of(new com.example.dreamjournal.model.Dream("dream-1", date, "text", null, null, List.of(), 0, now, now)));

        assertThat(service.details("user-1", "2026-08-18").sleep()).isNull();
        assertThat(service.details("user-1", "2026-08-18").dreams()).hasSize(1);
    }

    @Test
    void rejectsInvalidDateAndBlankUserId() {
        assertThatThrownBy(() -> service.get("user-1", "18-08-2026"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.get(" ", "2026-08-18"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
