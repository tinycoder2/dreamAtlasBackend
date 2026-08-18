package com.example.dreamjournal.service;

import com.example.dreamjournal.dto.DreamRequest;
import com.example.dreamjournal.exception.ResourceNotFoundException;
import com.example.dreamjournal.model.Dream;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DreamServiceTest {

    @Mock
    private DreamRepository dreamRepository;

    private DreamService service;
    private final Instant now = Instant.parse("2026-08-18T08:10:00Z");
    private final LocalDate date = LocalDate.parse("2026-08-18");

    @BeforeEach
    void setUp() {
        service = new DreamService(dreamRepository, Clock.fixed(now, ZoneOffset.UTC));
    }

    @Test
    void createsDreamWithDefaultsAndNormalizedTags() {
        when(dreamRepository.create(eq("user-1"), any())).thenAnswer(invocation -> {
            Dream input = invocation.getArgument(1);
            return new Dream("dream-1", input.date(), input.text(), input.mood(), input.dreamType(), input.tags(), input.sortOrder(), input.createdAt(), input.updatedAt());
        });

        Dream result = service.create("user-1", "2026-08-18", new DreamRequest("  hello  ", null, null, List.of("a", "b", "a"), null));

        assertThat(result.text()).isEqualTo("hello");
        assertThat(result.tags()).containsExactly("a", "b");
        assertThat(result.sortOrder()).isZero();
        assertThat(result.createdAt()).isEqualTo(now);
    }

    @Test
    void createsDreamWithEmptyTagsWhenMissing() {
        when(dreamRepository.create(eq("user-1"), any())).thenAnswer(invocation -> invocation.getArgument(1));

        Dream result = service.create("user-1", "2026-08-18", new DreamRequest("text", null, null, null, 0));

        assertThat(result.tags()).isEmpty();
    }

    @Test
    void updatesDreamAndPreservesCreatedAt() {
        Instant createdAt = Instant.parse("2026-08-18T07:45:00Z");
        Dream existing = new Dream("dream-1", date, "old", null, null, List.of("old"), 0, createdAt, createdAt);
        when(dreamRepository.findById("user-1", date, "dream-1")).thenReturn(Optional.of(existing));
        when(dreamRepository.save(eq("user-1"), any())).thenAnswer(invocation -> invocation.getArgument(1));

        Dream result = service.update("user-1", "2026-08-18", "dream-1", new DreamRequest("new", "CURIOUS", "NORMAL", List.of("new"), 1));

        assertThat(result.createdAt()).isEqualTo(createdAt);
        assertThat(result.updatedAt()).isEqualTo(now);
        assertThat(result.text()).isEqualTo("new");
    }

    @Test
    void missingDreamThrowsNotFound() {
        when(dreamRepository.findById("user-1", date, "missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.get("user-1", "2026-08-18", "missing"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void deletesDream() {
        when(dreamRepository.delete("user-1", date, "dream-1")).thenReturn(true);

        service.delete("user-1", "2026-08-18", "dream-1");

        verify(dreamRepository).delete("user-1", date, "dream-1");
    }

    @Test
    void listsDreamsInDeterministicOrder() {
        Dream third = new Dream("3", date, "third", null, null, List.of(), 1, Instant.parse("2026-08-18T07:50:00Z"), now);
        Dream first = new Dream("1", date, "first", null, null, List.of(), 0, Instant.parse("2026-08-18T07:30:00Z"), now);
        Dream second = new Dream("2", date, "second", null, null, List.of(), 0, Instant.parse("2026-08-18T07:40:00Z"), now);
        when(dreamRepository.findByUserIdAndDate("user-1", date)).thenReturn(List.of(third, second, first));

        assertThat(service.list("user-1", "2026-08-18")).extracting(Dream::id).containsExactly("1", "2", "3");
    }

    @Test
    void rejectsInvalidDateBlankUserAndNegativeSortOrderInputExpectation() {
        assertThatThrownBy(() -> service.list("user-1", "20260818"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.list("", "2026-08-18"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
