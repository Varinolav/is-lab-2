package ru.varino.lab1.security;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.security.enterprise.credential.Credential;
import jakarta.security.enterprise.credential.UsernamePasswordCredential;
import jakarta.security.enterprise.identitystore.CredentialValidationResult;
import jakarta.security.enterprise.identitystore.IdentityStore;
import jakarta.security.enterprise.identitystore.Pbkdf2PasswordHash;
import jakarta.transaction.Transactional;
import ru.varino.lab1.entity.AppUser;
import ru.varino.lab1.repository.UserRepository;

import java.util.Set;

@ApplicationScoped
public class UserIdentityStore implements IdentityStore {
    @Inject
    private UserRepository users;

    @Inject
    private Pbkdf2PasswordHash passwordHash;

    @Override
    @Transactional
    public CredentialValidationResult validate(Credential credential) {
        UsernamePasswordCredential login = (UsernamePasswordCredential) credential;
        AppUser user = users.find(login.getCaller().trim());
        if (user == null || !passwordHash.verify(login.getPassword().getValue(), user.getPasswordHash())) {
            return CredentialValidationResult.INVALID_RESULT;
        }
        return new CredentialValidationResult(user.getUsername(), Set.of(user.getRole()));
    }
}
