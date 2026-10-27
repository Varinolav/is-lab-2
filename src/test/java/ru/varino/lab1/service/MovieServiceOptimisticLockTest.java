package ru.varino.lab1.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.varino.lab1.entity.Movie;
import ru.varino.lab1.repository.MovieRepository;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MovieServiceOptimisticLockTest {
    private final List<String> calls = new ArrayList<>();
    private MovieService service;

    @BeforeEach
    void setup() throws Exception {
        service = new MovieService();
        var repository = MovieService.class.getDeclaredField("movieRepository");
        repository.setAccessible(true);
        repository.set(service, new MovieRepository() {
            @Override public void lockMovieNames() { calls.add("lock"); }
            @Override public Movie findById(Integer id) { calls.add("find"); return new Movie(); }
            @Override public List<Movie> findAll() { calls.add("list"); return List.of(); }
            @Override public void save(Movie movie) { calls.add("save"); }
            @Override public Movie update(Movie movie) { calls.add("update"); return movie; }
            @Override public boolean deleteById(Integer id) { calls.add("delete"); return true; }
        });
        var names = MovieService.class.getDeclaredField("movieNames");
        names.setAccessible(true);
        names.set(service, new MovieNameService() {
            @Override public void check(List<Movie> movies, Integer excludedId) { calls.add("check"); }
        });
    }

    @Test
    void updateLocksRegistryBeforeItsFirstRead() {
        service.update(1, new Movie());
        assertEquals(List.of("lock", "find", "check", "update"), calls);
    }

    @Test
    void creationProtectsCheckAndWriteTogether() {
        service.save(new Movie());
        assertEquals(List.of("lock", "check", "save"), calls);
    }

    @Test
    void readsAndDeletionDoNotLockNameRegistry() {
        service.findById(1);
        service.findAll();
        service.deleteById(1);
        assertEquals(List.of("find", "list", "delete"), calls);
    }
}
