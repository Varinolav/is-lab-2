package ru.varino.lab1.repository;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.hibernate.SessionFactory;
import ru.varino.lab1.entity.AppUser;

@ApplicationScoped
public class UserRepository {
    @Inject
    private SessionFactory sessionFactory;

    public AppUser find(String username) {
        return sessionFactory.getCurrentSession().find(AppUser.class, username);
    }

    public void save(AppUser user) {
        sessionFactory.getCurrentSession().persist(user);
        sessionFactory.getCurrentSession().flush();
    }
}
