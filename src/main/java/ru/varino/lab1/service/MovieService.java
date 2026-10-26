package ru.varino.lab1.service;


import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import ru.varino.lab1.entity.Movie;
import ru.varino.lab1.repository.MovieRepository;

import java.util.List;

@ApplicationScoped
@Transactional
public class MovieService {
    @Inject
    private MovieRepository movieRepository;

    @Inject
    private MovieNameService movieNames;

    public void save(Movie movie) {
        movieRepository.useMovieWriteIsolation();
        movieNames.check(List.of(movie), null);
        movieRepository.save(movie);
    }
    public Movie findById(Integer id) {
        return movieRepository.findById(id);
    }

    public List<Movie> findAll() {
        return movieRepository.findAll();
    }

    public Movie update(Integer id, Movie movie) {
        movieRepository.useMovieWriteIsolation();
        if (movieRepository.findById(id) == null) {
            return null;
        }

        movieNames.check(List.of(movie), id);
        movie.setId(id);
        return movieRepository.update(movie);
    }

    public boolean deleteById(Integer id) {
        return movieRepository.deleteById(id);
    }
}
