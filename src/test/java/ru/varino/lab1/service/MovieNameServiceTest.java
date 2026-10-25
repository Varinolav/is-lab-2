package ru.varino.lab1.service;

import jakarta.transaction.Status;
import jakarta.transaction.Synchronization;
import jakarta.transaction.TransactionSynchronizationRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.varino.lab1.entity.Movie;
import ru.varino.lab1.exception.MovieNameConflictException;
import ru.varino.lab1.repository.MovieRepository;

import java.util.*;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;

class MovieNameServiceTest {
    private final Map<Integer, String> names = new ConcurrentHashMap<>();
    private final Transactions transactions = new Transactions();
    private MovieNameService service;

    @BeforeEach
    void setup() throws Exception {
        service = new MovieNameService();
        var repository = MovieNameService.class.getDeclaredField("movieRepository");
        repository.setAccessible(true);
        repository.set(service, new MovieRepository() {
            @Override
            public List<String> findNamesExcept(Integer excludedId) {
                return names.entrySet().stream().filter(e -> !e.getKey().equals(excludedId))
                        .map(Map.Entry::getValue).toList();
            }
        });
        var registry = MovieNameService.class.getDeclaredField("transactions");
        registry.setAccessible(true);
        registry.set(service, transactions);
    }

    @AfterEach
    void rollback() {
        transactions.completion(Status.STATUS_ROLLEDBACK).run();
    }

    @Test
    void ignoresCaseAndSurroundingWhitespace() {
        names.put(1, "  Интерстеллар  ");
        assertThrows(MovieNameConflictException.class,
                () -> service.check(List.of(movie("ИНТЕРСТЕЛЛАР")), null));
    }

    @Test
    void rejectsDuplicateTitlesWithinOneImport() {
        assertThrows(MovieNameConflictException.class,
                () -> service.check(List.of(movie("Дюна"), movie(" дЮнА ")), null));
    }

    @Test
    void editingIgnoresOwnTitleButNotAnotherMovie() {
        names.put(1, "Дюна");
        names.put(2, "Интерстеллар");
        assertDoesNotThrow(() -> service.check(List.of(movie(" ДЮНА ")), 1));
        assertThrows(MovieNameConflictException.class,
                () -> service.check(List.of(movie("интерстеллар")), 1));
    }

    @Test
    void rollbackReleasesLockFromAnotherThread() throws Exception {
        service.check(List.of(movie("Дюна")), null);
        CompletableFuture.runAsync(transactions.completion(Status.STATUS_ROLLEDBACK))
                .get(2, TimeUnit.SECONDS);
        assertDoesNotThrow(() -> service.check(List.of(movie("Дюна")), null));
    }

    @Test
    void concurrentTransactionWaitsForCommitThenSeesCommittedTitle() throws Exception {
        service.check(List.of(movie("Дюна")), null);
        var started = new CountDownLatch(1);
        var executor = Executors.newSingleThreadExecutor();
        try {
            var second = executor.submit(() -> {
                started.countDown();
                try {
                    return assertThrows(MovieNameConflictException.class,
                            () -> service.check(List.of(movie(" ДЮНА ")), null));
                } finally {
                    transactions.completion(Status.STATUS_ROLLEDBACK).run();
                }
            });
            assertTrue(started.await(2, TimeUnit.SECONDS));
            assertThrows(TimeoutException.class, () -> second.get(150, TimeUnit.MILLISECONDS));
            names.put(1, "Дюна");
            transactions.completion(Status.STATUS_COMMITTED).run();
            assertNotNull(second.get(2, TimeUnit.SECONDS));
        } finally {
            transactions.completion(Status.STATUS_ROLLEDBACK).run();
            executor.shutdownNow();
        }
    }

    private Movie movie(String name) {
        Movie movie = new Movie();
        movie.setName(name);
        return movie;
    }

    private static class Transactions implements TransactionSynchronizationRegistry {
        private static class State {
            final Map<Object, Object> resources = new HashMap<>();
            final List<Synchronization> callbacks = new ArrayList<>();
            boolean rollbackOnly;
        }
        private final ThreadLocal<State> state = ThreadLocal.withInitial(State::new);

        Runnable completion(int status) {
            State completed = state.get();
            state.remove();
            return () -> completed.callbacks.forEach(callback -> callback.afterCompletion(status));
        }

        public Object getTransactionKey() { return state.get(); }
        public void putResource(Object key, Object value) { state.get().resources.put(key, value); }
        public Object getResource(Object key) { return state.get().resources.get(key); }
        public void registerInterposedSynchronization(Synchronization callback) { state.get().callbacks.add(callback); }
        public int getTransactionStatus() { return Status.STATUS_ACTIVE; }
        public void setRollbackOnly() { state.get().rollbackOnly = true; }
        public boolean getRollbackOnly() { return state.get().rollbackOnly; }
    }
}
