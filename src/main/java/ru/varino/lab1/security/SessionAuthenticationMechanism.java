package ru.varino.lab1.security;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.security.enterprise.AuthenticationStatus;
import jakarta.security.enterprise.authentication.mechanism.http.AutoApplySession;
import jakarta.security.enterprise.authentication.mechanism.http.HttpAuthenticationMechanism;
import jakarta.security.enterprise.authentication.mechanism.http.HttpMessageContext;
import jakarta.security.enterprise.identitystore.CredentialValidationResult;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@ApplicationScoped
@AutoApplySession
public class SessionAuthenticationMechanism implements HttpAuthenticationMechanism {
    @Inject
    private UserIdentityStore identityStore;

    @Override
    public AuthenticationStatus validateRequest(HttpServletRequest request, HttpServletResponse response,
                                                HttpMessageContext context) {
        var credential = context.getAuthParameters().getCredential();
        if (credential != null) {
            var result = identityStore.validate(credential);
            if (result.getStatus() == CredentialValidationResult.Status.VALID) {
                return context.notifyContainerAboutLogin(result);
            }
            return AuthenticationStatus.SEND_FAILURE;
        }
        if (context.isProtected()) {
            return context.responseUnauthorized();
        }
        return context.doNothing();
    }
}
