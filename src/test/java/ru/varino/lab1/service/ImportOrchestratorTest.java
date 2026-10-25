package ru.varino.lab1.service;

import org.junit.jupiter.api.Test;
import ru.varino.lab1.entity.Movie;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class ImportOrchestratorTest {
    @Test
    void successfulImportReturnsCountWithoutRecordingFailure() throws Exception {
        var failures = new AtomicInteger();
        var orchestrator = orchestrator(new ImportService() {
            @Override
            public int importMovies(List<Movie> movies, String username) {
                assertEquals("alice", username);
                return 2;
            }
        }, new HistoryService() {
            @Override
            public void saveFailed(String username) { failures.incrementAndGet(); }
        });
        assertEquals(2, orchestrator.initiateImport(List.of(), "alice"));
        assertEquals(0, failures.get());
    }

    @Test
    void serviceFailureIsRecordedAndRethrown() throws Exception {
        var failure = new RuntimeException("Ошибка фиксации транзакции");
        var failures = new AtomicInteger();
        var orchestrator = orchestrator(failingImport(failure), new HistoryService() {
            @Override
            public void saveFailed(String username) {
                assertEquals("alice", username);
                failures.incrementAndGet();
            }
        });
        assertSame(failure, assertThrows(RuntimeException.class,
                () -> orchestrator.initiateImport(List.of(), "alice")));
        assertEquals(1, failures.get());
    }

    @Test
    void historyFailureDoesNotReplaceOriginalImportFailure() throws Exception {
        var original = new RuntimeException("Ошибка импорта");
        var historyFailure = new RuntimeException("История недоступна");
        var orchestrator = orchestrator(failingImport(original), new HistoryService() {
            @Override
            public void saveFailed(String username) { throw historyFailure; }
        });
        assertSame(original, assertThrows(RuntimeException.class,
                () -> orchestrator.initiateImport(List.of(), "alice")));
        assertArrayEquals(new Throwable[]{historyFailure}, original.getSuppressed());
    }

    private ImportService failingImport(RuntimeException failure) {
        return new ImportService() {
            @Override
            public int importMovies(List<Movie> movies, String username) { throw failure; }
        };
    }

    private ImportOrchestrator orchestrator(ImportService service, HistoryService history) throws Exception {
        var orchestrator = new ImportOrchestrator();
        var imports = ImportOrchestrator.class.getDeclaredField("importService");
        imports.setAccessible(true);
        imports.set(orchestrator, service);
        var histories = ImportOrchestrator.class.getDeclaredField("historyService");
        histories.setAccessible(true);
        histories.set(orchestrator, history);
        return orchestrator;
    }
}
