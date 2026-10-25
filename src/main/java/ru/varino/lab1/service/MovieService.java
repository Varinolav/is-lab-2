package ru.varino.lab1.service;


import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.Validator;
import ru.varino.lab1.exception.ImportValidationException;
import ru.varino.lab1.entity.Movie;
import ru.varino.lab1.entity.Person;
import ru.varino.lab1.repository.MovieRepository;

import java.util.List;
import java.util.stream.Collectors;

@ApplicationScoped
@Transactional
public class MovieService {
    @Inject
    private MovieRepository movieRepository;

    public void save(Movie movie) {
        movieRepository.save(movie);
    }
    public Movie findById(Integer id) {
        return movieRepository.findById(id);
    }

    public List<Movie> findAll() {
        return movieRepository.findAll();
    }

    public Movie update(Integer id, Movie movie) {
        if (movieRepository.findById(id) == null) {
            return null;
        }

        movie.setId(id);
        return movieRepository.update(movie);
    }

    public boolean deleteById(Integer id) {
        return movieRepository.deleteById(id);
    }
}
