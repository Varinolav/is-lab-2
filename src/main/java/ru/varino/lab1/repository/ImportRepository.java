package ru.varino.lab1.repository;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import ru.varino.lab1.entity.ImportOperation;

import java.util.List;

@ApplicationScoped
@Transactional
public class ImportRepository {

    @Inject
    private SessionFactory sessionFactory;

    private Session session() {
        return sessionFactory.getCurrentSession();
    }

    public void save(ImportOperation operation) {
        session().persist(operation);
    }

    public ImportOperation findById(Long id) {
        return session().find(ImportOperation.class, id);
    }

    public List<ImportOperation> findAll() {
        return session().createQuery("FROM ImportOperation ORDER BY id", ImportOperation.class).getResultList();
    }

    public List<ImportOperation> findByUsername(String username) {
        return session().createQuery(
                        "SELECT io FROM ImportOperation io WHERE io.username = :username ORDER BY io.id",
                        ImportOperation.class)
                .setParameter("username", username)
                .getResultList();
    }
}
