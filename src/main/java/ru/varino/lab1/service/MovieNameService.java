package ru.varino.lab1.service;

import jakarta.annotation.Resource;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Synchronization;
import jakarta.transaction.TransactionSynchronizationRegistry;
import ru.varino.lab1.entity.Movie;
import ru.varino.lab1.exception.MovieNameConflictException;
import ru.varino.lab1.repository.MovieRepository;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Semaphore;

@ApplicationScoped
public class MovieNameService {
    @Inject
    private MovieRepository movieRepository;

    @Resource
    private TransactionSynchronizationRegistry transactions;

    private final Semaphore writes = new Semaphore(1, true);

    public void check(List<Movie> movies, Integer excludedId) {
        lockUntilTransactionCompletes();
        var names = new HashSet<String>();
        for (String name : movieRepository.findNamesExcept(excludedId)) {
            names.add(normalize(name));
        }
        for (Movie movie : movies) {
            if (!names.add(normalize(movie.getName()))) {
                throw new MovieNameConflictException(movie.getName());
            }
        }
    }

    private String normalize(String name) {
        return name.strip().toLowerCase();
    }

    private void lockUntilTransactionCompletes() {
        if (transactions.getResource(writes) != null) return;
        writes.acquireUninterruptibly();
        try {
            transactions.putResource(writes, true);
            transactions.registerInterposedSynchronization(new Synchronization() {
                @Override
                public void beforeCompletion() {}

                @Override
                public void afterCompletion(int status) {
                    // Completion may run on another thread, so use a semaphore.
                    writes.release();
                }
            });
        } catch (RuntimeException e) {
            writes.release();
            throw e;
        }
    }
}
