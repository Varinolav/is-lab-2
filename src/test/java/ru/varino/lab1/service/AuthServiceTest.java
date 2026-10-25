package ru.varino.lab1.service;

import jakarta.security.enterprise.identitystore.Pbkdf2PasswordHash;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.varino.lab1.entity.AppUser;
import ru.varino.lab1.exception.UsernameAlreadyExistsException;
import ru.varino.lab1.repository.UserRepository;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class AuthServiceTest {
    private AuthService auth;
    private final Map<String, AppUser> accounts = new HashMap<>();

    @BeforeEach
    void setup() throws Exception {
        auth = new AuthService();
        set("users", new UserRepository() {
            public AppUser find(String username) { return accounts.get(username); }
            public void save(AppUser user) { accounts.put(user.getUsername(), user); }
        });
        set("passwordHash", new Pbkdf2PasswordHash() {
            public void initialize(Map<String, String> parameters) {}
            public String generate(char[] password) {
                assertArrayEquals("example123".toCharArray(), password);
                return "encoded-password";
            }
            public boolean verify(char[] password, String hash) { throw new UnsupportedOperationException(); }
        });
    }

    @Test
    void registersUserWithHashedPasswordAndTrimmedUsername() {
        AppUser user = auth.register(" alice ", "example123", "USER");
        assertSame(user, accounts.get("alice"));
        assertEquals("alice", user.getUsername());
        assertEquals("USER", user.getRole());
        assertEquals("encoded-password", user.getPasswordHash());
    }

    @Test
    void registersSelectedAdminRole() {
        assertEquals("ADMIN", auth.register("admin", "example123", "ADMIN").getRole());
    }

    @Test
    void duplicateUsernameCannotOverwriteAccountOrRole() {
        AppUser original = auth.register("alice", "example123", "USER");
        assertThrows(UsernameAlreadyExistsException.class,
                () -> auth.register(" alice ", "another-password", "ADMIN"));
        assertSame(original, accounts.get("alice"));
        assertEquals("USER", original.getRole());
        assertEquals(1, accounts.size());
    }

    @Test
    void concurrentDuplicateInsertBecomesDomainException() throws Exception {
        set("users", new UserRepository() {
            public AppUser find(String username) { return null; }
            public void save(AppUser user) {
                throw new ConstraintViolationException("duplicate",
                        new SQLException("duplicate", "23505"), "app_user_pkey");
            }
        });
        assertThrows(UsernameAlreadyExistsException.class,
                () -> auth.register("alice", "example123", "USER"));
    }

    private void set(String name, Object value) throws Exception {
        var field = AuthService.class.getDeclaredField(name);
        field.setAccessible(true);
        field.set(auth, value);
    }
}
