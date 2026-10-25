package ru.varino.lab1.config;

import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import org.hibernate.SessionFactory;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.Configuration;

@ApplicationScoped
public class HibernateSessionFactoryProducer {
    private StandardServiceRegistry registry;
    private SessionFactory sessionFactory;

    @Produces
    @ApplicationScoped
    public SessionFactory sessionFactory() {
        Configuration configuration = new Configuration().configure()
                .addResource("hibernate/Coordinates.hbm.xml")
                .addResource("hibernate/Location.hbm.xml")
                .addResource("hibernate/Person.hbm.xml")
                .addResource("hibernate/Movie.hbm.xml")
                .addResource("hibernate/AppUser.hbm.xml")
                .addResource("hibernate/ImportOperation.hbm.xml");
        registry = new StandardServiceRegistryBuilder()
                .applySettings(configuration.getProperties())
                .build();
        sessionFactory = configuration.buildSessionFactory(registry);
        return sessionFactory;
    }

    @PreDestroy
    void close() {
        if (sessionFactory != null) {
            sessionFactory.close();
        }
        if (registry != null) {
            StandardServiceRegistryBuilder.destroy(registry);
        }
    }
}
