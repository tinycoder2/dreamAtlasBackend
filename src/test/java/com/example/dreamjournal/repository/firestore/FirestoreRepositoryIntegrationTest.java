package com.example.dreamjournal.repository.firestore;

import com.example.dreamjournal.model.DayLog;
import com.example.dreamjournal.model.Dream;
import com.example.dreamjournal.repository.DayLogRepository;
import com.example.dreamjournal.repository.DreamRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.net.InetSocketAddress;
import java.net.Socket;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@EnabledIfEnvironmentVariable(named = "FIRESTORE_EMULATOR_HOST", matches = ".+")
@TestPropertySource(properties = "google.cloud.project-id=dream-journal-test")
class FirestoreRepositoryIntegrationTest {

    @Autowired
    private DayLogRepository dayLogRepository;

    @Autowired
    private DreamRepository dreamRepository;

    @Test
    void coversFirestoreCrudFlowsAndUserIsolation() {
        assumeFirestoreEmulatorIsReachable();

        String userId = "user-" + UUID.randomUUID();
        String otherUserId = "user-" + UUID.randomUUID();
        LocalDate date = LocalDate.parse("2026-08-18");
        Instant first = Instant.parse("2026-08-18T07:30:00Z");
        Instant second = Instant.parse("2026-08-18T07:40:00Z");

        DayLog created = dayLogRepository.save(userId, new DayLog(date, 7.5, "GOOD", first, first));
        assertThat(dayLogRepository.findByUserIdAndDate(userId, date)).contains(created);
        assertThat(dayLogRepository.findByUserIdAndDate(otherUserId, date)).isEmpty();

        DayLog updated = dayLogRepository.save(userId, new DayLog(date, 8.0, "GREAT", first, second));
        assertThat(dayLogRepository.findByUserIdAndDate(userId, date).orElseThrow().updatedAt()).isEqualTo(second);

        Dream laterSort = dreamRepository.create(userId, new Dream(null, date, "second", null, null, List.of("flying"), 1, second, second));
        Dream earlierSort = dreamRepository.create(userId, new Dream(null, date, "first", null, null, List.of("home"), 0, first, first));
        assertThat(dreamRepository.findById(userId, date, earlierSort.id())).contains(earlierSort);
        assertThat(dreamRepository.findById(otherUserId, date, earlierSort.id())).isEmpty();
        assertThat(dreamRepository.findByUserIdAndDate(userId, date)).extracting(Dream::id)
                .containsExactly(earlierSort.id(), laterSort.id());

        Dream updatedDream = new Dream(earlierSort.id(), date, "updated", "CURIOUS", "NORMAL", List.of("updated"), 0, first, second);
        dreamRepository.save(userId, updatedDream);
        assertThat(dreamRepository.findById(userId, date, earlierSort.id()).orElseThrow().text()).isEqualTo("updated");

        assertThat(dreamRepository.delete(userId, date, laterSort.id())).isTrue();
        assertThat(dreamRepository.findById(userId, date, laterSort.id())).isEmpty();

        assertThat(dreamRepository.deleteAllForDate(userId, date)).isEqualTo(1);
        dayLogRepository.delete(userId, date);
        assertThat(dayLogRepository.findByUserIdAndDate(userId, date)).isEmpty();
        assertThat(dreamRepository.findByUserIdAndDate(userId, date)).isEmpty();
    }

    private void assumeFirestoreEmulatorIsReachable() {
        String emulatorHost = System.getenv("FIRESTORE_EMULATOR_HOST");
        String[] parts = emulatorHost.split(":", 2);
        String host = parts[0];
        int port = parts.length == 2 ? Integer.parseInt(parts[1]) : 8080;

        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port), 500);
        } catch (Exception ex) {
            Assumptions.abort("Firestore emulator is not reachable at " + emulatorHost);
        }
    }
}
