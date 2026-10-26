package ru.varino.lab1.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import ru.varino.lab1.entity.Movie;
import ru.varino.lab1.exception.MovieNameConflictException;
import ru.varino.lab1.repository.MovieRepository;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;

@ApplicationScoped
public class MovieNameService {
    @Inject
    private MovieRepository movieRepository;

    public void check(List<Movie> movies, Integer excludedId) {
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
        return name.strip().toLowerCase(Locale.ROOT);
    }

}
