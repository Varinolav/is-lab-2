package ru.varino.lab1.service;

import jakarta.persistence.OptimisticLockException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.StaleStateException;
import org.hibernate.Transaction;
import org.hibernate.cfg.Configuration;
import org.hibernate.context.internal.ManagedSessionContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.varino.lab1.entity.Coordinates;
import ru.varino.lab1.entity.Location;
import ru.varino.lab1.entity.Movie;
import ru.varino.lab1.entity.MovieNameRegistry;
import ru.varino.lab1.entity.MpaaRating;
import ru.varino.lab1.entity.Person;
import ru.varino.lab1.exception.MovieNameConflictException;
import ru.varino.lab1.repository.MovieRepository;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class MovieNameConcurrencyTest {
    private SessionFactory factory;
    private MovieRepository repository;
    private MovieNameService names;

    @BeforeEach
    void setup() throws Exception {
        factory = new Configuration()
                .addAnnotatedClass(MovieNameRegistry.class)
                .addResource("hibernate/Coordinates.hbm.xml")
                .addResource("hibernate/Location.hbm.xml")
                .addResource("hibernate/Person.hbm.xml")
                .addResource("hibernate/Movie.hbm.xml")
                .setProperty("hibernate.connection.driver_class", "org.h2.Driver")
                .setProperty("hibernate.connection.url", "jdbc:h2:mem:" + UUID.randomUUID())
                .setProperty("hibernate.connection.isolation", "2")
                .setProperty("hibernate.hbm2ddl.auto", "create-drop")
                .setProperty("hibernate.current_session_context_class", "managed")
                .buildSessionFactory();
        repository = new MovieRepository();
        var factoryField = MovieRepository.class.getDeclaredField("sessionFactory");
        factoryField.setAccessible(true);
        factoryField.set(repository, factory);
        names = new MovieNameService();
        var repositoryField = MovieNameService.class.getDeclaredField("movieRepository");
        repositoryField.setAccessible(true);
        repositoryField.set(names, repository);
        try (Session session = factory.openSession()) {
            Transaction transaction = session.beginTransaction();
            session.persist(new MovieNameRegistry());
            transaction.commit();
        }
    }

    @AfterEach
    void close() {
        if (factory != null) {
            ManagedSessionContext.unbind(factory);
            factory.close();
        }
    }

    @Test
    void overlappingCreationRollsBackLoserAndItsNestedObjects() {
        try (Session first = factory.openSession(); Session second = factory.openSession()) {
            Transaction firstTransaction = first.beginTransaction();
            Transaction secondTransaction = second.beginTransaction();
            saveBatch(first, List.of(movie("Дюна")));
            saveBatch(second, List.of(movie(" дЮнА ")));

            firstTransaction.commit();
            RuntimeException conflict = assertThrows(RuntimeException.class, secondTransaction::commit);
            assertOptimisticConflict(conflict);
            if (secondTransaction.isActive()) secondTransaction.rollback();
        }
        assertCount(Movie.class, 1);
        assertCount(Coordinates.class, 1);
        assertCount(Person.class, 2);
        assertCount(Location.class, 2);
        try (Session retry = factory.openSession()) {
            Transaction transaction = retry.beginTransaction();
            ManagedSessionContext.bind(retry);
            repository.lockMovieNames();
            assertThrows(MovieNameConflictException.class,
                    () -> names.check(List.of(movie(" ДЮНА ")), null));
            transaction.rollback();
        }
    }

    @Test
    void conflictingBatchLeavesNoPartiallyImportedMovies() {
        try (Session first = factory.openSession(); Session second = factory.openSession()) {
            Transaction firstTransaction = first.beginTransaction();
            Transaction secondTransaction = second.beginTransaction();
            saveBatch(first, List.of(movie("Общее имя")));
            saveBatch(second, List.of(movie("Уникальная часть пакета"), movie("Общее имя")));

            firstTransaction.commit();
            assertOptimisticConflict(assertThrows(RuntimeException.class, secondTransaction::commit));
            if (secondTransaction.isActive()) secondTransaction.rollback();
        }
        assertCount(Movie.class, 1);
        assertCount(Coordinates.class, 1);
        assertCount(Person.class, 2);
        assertCount(Location.class, 2);
    }

    @Test
    void sequentialWritesAdvanceVersionAndKeepBothDifferentNames() {
        for (String name : List.of("Дюна", "Интерстеллар")) {
            try (Session session = factory.openSession()) {
                Transaction transaction = session.beginTransaction();
                saveBatch(session, List.of(movie(name)));
                transaction.commit();
            }
        }
        assertCount(Movie.class, 2);
        try (Session session = factory.openSession()) {
            assertEquals(2L, session.find(MovieNameRegistry.class, MovieNameRegistry.ID).getVersion());
        }
    }

    private void saveBatch(Session session, List<Movie> movies) {
        ManagedSessionContext.bind(session);
        repository.lockMovieNames();
        names.check(movies, null);
        movies.forEach(repository::saveImported);
    }

    private void assertCount(Class<?> entity, long expected) {
        try (Session session = factory.openSession()) {
            assertEquals(expected, session.createQuery("select count(*) from " + entity.getSimpleName(), Long.class)
                    .getSingleResult());
        }
    }

    private void assertOptimisticConflict(Throwable failure) {
        for (Throwable cause = failure; cause != null; cause = cause.getCause()) {
            if (cause instanceof OptimisticLockException || cause instanceof StaleStateException) return;
        }
        fail("Ожидался оптимистический конфликт", failure);
    }

    private Movie movie(String name) {
        Coordinates coordinates = new Coordinates();
        coordinates.setX(10.0);
        coordinates.setY(20);
        Movie movie = new Movie();
        movie.setName(name);
        movie.setCoordinates(coordinates);
        movie.setMpaaRating(MpaaRating.G);
        movie.setDirector(person());
        movie.setOperator(person());
        return movie;
    }

    private Person person() {
        Location location = new Location();
        location.setY(2.0);
        Person person = new Person();
        person.setName("Участник");
        person.setLocation(location);
        return person;
    }
}
