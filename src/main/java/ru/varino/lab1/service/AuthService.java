package ru.varino.lab1.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.security.enterprise.identitystore.Pbkdf2PasswordHash;
import jakarta.transaction.Transactional;
import ru.varino.lab1.entity.AppUser;
import ru.varino.lab1.repository.UserRepository;
import ru.varino.lab1.exception.UsernameAlreadyExistsException;
import org.hibernate.exception.ConstraintViolationException;

@ApplicationScoped
@Transactional
public class AuthService {
    @Inject
    private UserRepository users;

    @Inject
    private Pbkdf2PasswordHash passwordHash;

    public AppUser register(String username, String password, String role) {
        username = username.trim();
        if (users.find(username) != null) throw new UsernameAlreadyExistsException();
        AppUser user = new AppUser();
        user.setUsername(username);
        user.setPasswordHash(passwordHash.generate(password.toCharArray()));
        user.setRole(role);
        try {
            users.save(user);
        } catch (ConstraintViolationException e) {
            throw new UsernameAlreadyExistsException();
        }
        return user;
    }
}
