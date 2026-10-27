package ru.varino.lab1.repository;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.LockModeType;
import jakarta.transaction.Transactional;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import ru.varino.lab1.entity.Movie;
import ru.varino.lab1.entity.MovieNameRegistry;
import ru.varino.lab1.entity.Person;

import java.time.LocalDateTime;
import java.util.List;

@ApplicationScoped
public class MovieRepository {

    @Inject
    private SessionFactory sessionFactory;

    private Session session() {
        return sessionFactory.getCurrentSession();
    }

    @Transactional(Transactional.TxType.REQUIRES_NEW)
    public void initializeNameRegistry() {
        session().createNativeMutationQuery("INSERT INTO movie_name_registry (id, version) VALUES (:id, 0) "
                        + "ON CONFLICT (id) DO NOTHING")
                .setParameter("id", MovieNameRegistry.ID)
                .executeUpdate();
    }

    public void lockMovieNames() {
        MovieNameRegistry registry = session().find(MovieNameRegistry.class, MovieNameRegistry.ID,
                LockModeType.OPTIMISTIC_FORCE_INCREMENT);
        if (registry == null) {
            throw new IllegalStateException("Реестр названий фильмов не инициализирован.");
        }
    }

    public void save(Movie movie) {
        movie.setCreationDate(LocalDateTime.now());
        session().persist(movie);
    }

    public void saveImported(Movie movie) {
        Session session = session();
        session.persist(movie.getCoordinates());
        persistImportedPerson(session, movie.getDirector());
        persistImportedPerson(session, movie.getScreenwriter());
        persistImportedPerson(session, movie.getOperator());
        save(movie);
    }

    private void persistImportedPerson(Session session, Person person) {
        if (person != null) {
            session.persist(person.getLocation());
            session.persist(person);
        }
    }

    public Movie findById(Integer id) {
        return session().find(Movie.class, id);
    }

    public List<Movie> findAll() {
        return session().createQuery("FROM Movie ORDER BY id", Movie.class).getResultList();
    }

    public List<String> findNamesExcept(Integer excludedId) {
        var query = session().createQuery("SELECT m.name FROM Movie m"
                + (excludedId == null ? "" : " WHERE m.id <> :id"), String.class);
        if (excludedId != null) query.setParameter("id", excludedId);
        return query.getResultList();
    }

    public Movie update(Movie movie) {
        return session().merge(movie);
    }

    public boolean deleteById(Integer id) {
        Movie movie = findById(id);
        if (movie == null) {
            return false;
        }

        session().remove(movie);
        session().flush();
        return true;
    }
}
