package ru.varino.lab1.security;

import jakarta.security.enterprise.credential.UsernamePasswordCredential;
import jakarta.security.enterprise.identitystore.CredentialValidationResult;
import jakarta.security.enterprise.identitystore.Pbkdf2PasswordHash;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.varino.lab1.entity.AppUser;
import ru.varino.lab1.repository.UserRepository;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class UserIdentityStoreTest {
    private UserIdentityStore store;
    private AppUser user;

    @BeforeEach
    void setup() throws Exception {
        user = new AppUser();
        user.setUsername("admin");
        user.setPasswordHash("stored-hash");
        user.setRole("ADMIN");
        store = new UserIdentityStore();
        set("users", new UserRepository() {
            @Override
            public AppUser find(String username) {
                return "admin".equals(username) ? user : null;
            }
        });
        set("passwordHash", new Pbkdf2PasswordHash() {
            public void initialize(Map<String, String> parameters) {}
            public String generate(char[] password) { throw new UnsupportedOperationException(); }
            public boolean verify(char[] password, String hash) {
                return "correct-password".equals(new String(password)) && "stored-hash".equals(hash);
            }
        });
    }

    @Test
    void returnsIdentityAndDatabaseRole() {
        var result = store.validate(new UsernamePasswordCredential("admin", "correct-password"));
        assertEquals(CredentialValidationResult.Status.VALID, result.getStatus());
        assertEquals("admin", result.getCallerPrincipal().getName());
        assertEquals(Set.of("ADMIN"), result.getCallerGroups());
    }

    @Test
    void rejectsIncorrectPasswordAndUnknownUser() {
        assertEquals(CredentialValidationResult.Status.INVALID,
                store.validate(new UsernamePasswordCredential("admin", "wrong-password")).getStatus());
        assertEquals(CredentialValidationResult.Status.INVALID,
                store.validate(new UsernamePasswordCredential("unknown", "correct-password")).getStatus());
    }

    @Test
    void normalUserDoesNotReceiveAdminRole() {
        user.setRole("USER");
        var result = store.validate(new UsernamePasswordCredential("admin", "correct-password"));
        assertEquals(Set.of("USER"), result.getCallerGroups());
    }

    private void set(String name, Object value) throws Exception {
        var field = UserIdentityStore.class.getDeclaredField(name);
        field.setAccessible(true);
        field.set(store, value);
    }
}
