package ru.varino.lab1.service;

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

    private Movie movie(String name) {
        Movie movie = new Movie();
        movie.setName(name);
        return movie;
    }

}
