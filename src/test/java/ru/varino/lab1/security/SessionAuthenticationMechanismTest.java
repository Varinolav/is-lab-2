package ru.varino.lab1.security;

import jakarta.security.enterprise.AuthenticationStatus;
import jakarta.security.enterprise.authentication.mechanism.http.AuthenticationParameters;
import jakarta.security.enterprise.authentication.mechanism.http.HttpMessageContext;
import jakarta.security.enterprise.credential.UsernamePasswordCredential;
import jakarta.security.enterprise.identitystore.CredentialValidationResult;
import jakarta.security.enterprise.credential.Credential;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;

class SessionAuthenticationMechanismTest {
    @Test
    void notifiesContainerOnlyAfterSuccessfulValidation() throws Exception {
        var valid = new CredentialValidationResult("user", Set.of("USER"));
        var mechanism = mechanism(credential -> valid);
        AtomicInteger logins = new AtomicInteger();
        var context = context(AuthenticationParameters.withParams()
                .credential(new UsernamePasswordCredential("user", "password")), false, logins);
        assertEquals(AuthenticationStatus.SUCCESS, mechanism.validateRequest(null, null, context));
        assertEquals(1, logins.get());

        mechanism = mechanism(credential -> CredentialValidationResult.INVALID_RESULT);
        assertEquals(AuthenticationStatus.SEND_FAILURE, mechanism.validateRequest(null, null, context));
        assertEquals(1, logins.get());
    }

    @Test
    void anonymousProtectedRequestIsRejectedByContainer() throws Exception {
        var mechanism = mechanism(credential -> { throw new AssertionError("No credentials supplied"); });
        AtomicInteger rejections = new AtomicInteger();
        assertEquals(AuthenticationStatus.SEND_FAILURE, mechanism.validateRequest(null, null,
                context(AuthenticationParameters.withParams(), true, rejections)));
        assertEquals(1, rejections.get());
    }

    @Test
    void loginPageIsAvailableWithoutAuthentication() throws Exception {
        var mechanism = mechanism(credential -> { throw new AssertionError(); });
        assertEquals(AuthenticationStatus.NOT_DONE, mechanism.validateRequest(null, null,
                context(AuthenticationParameters.withParams(), false, new AtomicInteger())));
    }

    private SessionAuthenticationMechanism mechanism(Function<Credential, CredentialValidationResult> validate)
            throws Exception {
        var mechanism = new SessionAuthenticationMechanism();
        var field = SessionAuthenticationMechanism.class.getDeclaredField("identityStore");
        field.setAccessible(true);
        field.set(mechanism, new UserIdentityStore() {
            @Override
            public CredentialValidationResult validate(Credential credential) {
                return validate.apply(credential);
            }
        });
        return mechanism;
    }

    private HttpMessageContext context(AuthenticationParameters parameters, boolean protectedResource,
                                       AtomicInteger logins) {
        return (HttpMessageContext) Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class[]{HttpMessageContext.class}, (proxy, method, args) -> switch (method.getName()) {
                    case "getAuthParameters" -> parameters;
                    case "isProtected" -> protectedResource;
                    case "doNothing" -> AuthenticationStatus.NOT_DONE;
                    case "responseUnauthorized" -> {
                        logins.incrementAndGet();
                        yield AuthenticationStatus.SEND_FAILURE;
                    }
                    case "notifyContainerAboutLogin" -> {
                        logins.incrementAndGet();
                        yield AuthenticationStatus.SUCCESS;
                    }
                    default -> throw new UnsupportedOperationException(method.getName());
                });
    }
}
