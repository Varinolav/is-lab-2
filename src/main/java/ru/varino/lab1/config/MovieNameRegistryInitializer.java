package ru.varino.lab1.config;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.Initialized;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import ru.varino.lab1.repository.MovieRepository;

@ApplicationScoped
public class MovieNameRegistryInitializer {
    @Inject
    private MovieRepository movieRepository;

    public void initialize(@Observes @Initialized(ApplicationScoped.class) Object event) {
        movieRepository.initializeNameRegistry();
    }
}
