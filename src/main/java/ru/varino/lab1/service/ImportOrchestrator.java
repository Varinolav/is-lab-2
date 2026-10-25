package ru.varino.lab1.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import ru.varino.lab1.entity.Movie;

import java.util.List;

@ApplicationScoped
public class ImportOrchestrator {
    @Inject
    private ImportService importService;

    @Inject
    private HistoryService historyService;

    public int initiateImport(List<Movie> movies, String username) {
        try {
            return importService.importMovies(movies, username);
        } catch (RuntimeException e) {
            historyService.saveFailed(username);
            throw e;
        }
    }
}
