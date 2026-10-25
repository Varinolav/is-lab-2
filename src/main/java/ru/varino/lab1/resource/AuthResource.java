package ru.varino.lab1.resource;

import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.ServletException;
import jakarta.security.enterprise.AuthenticationStatus;
import jakarta.security.enterprise.SecurityContext;
import jakarta.security.enterprise.authentication.mechanism.http.AuthenticationParameters;
import jakarta.security.enterprise.credential.UsernamePasswordCredential;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Pattern;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import ru.varino.lab1.service.AuthService;

import java.util.Map;

@Path("/auth")
@Produces(MediaType.APPLICATION_JSON)
public class AuthResource {
    @Inject
    private SecurityContext security;

    @Inject
    private AuthService auth;

    @Context
    private HttpServletRequest request;

    @Context
    private HttpServletResponse response;

    public record Credentials(
            @NotBlank(message = "Введите логин.") String username,
            @NotBlank(message = "Введите пароль.")
            @Size(max = 1024, message = "Пароль не должен содержать больше 1024 символов.") String password) {}

    public record Registration(
            @NotBlank(message = "Введите логин.")
            @Size(max = 100, message = "Логин не должен содержать больше 100 символов.") String username,
            @NotBlank(message = "Введите пароль.")
            @Size(max = 1024, message = "Пароль не должен содержать больше 1024 символов.") String password,
            @NotNull(message = "Выберите роль.")
            @Pattern(regexp = "USER|ADMIN", message = "Допустимы роли USER и ADMIN.") String role) {}

    @POST
    @Path("/register")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response register(@NotNull(message = "Передайте данные для регистрации.") @Valid Registration registration) {
        var user = auth.register(registration.username(), registration.password(), registration.role());
        return Response.status(Response.Status.CREATED)
                .entity(Map.of("username", user.getUsername(), "role", user.getRole())).build();
    }

    @POST
    @Path("/login")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response login(@NotNull(message = "Передайте логин и пароль.") @Valid Credentials credentials)
            throws ServletException {
        clearSession();
        var credential = new UsernamePasswordCredential(credentials.username(), credentials.password());
        AuthenticationStatus status = security.authenticate(request, response,
                AuthenticationParameters.withParams().newAuthentication(true).credential(credential));
        if (status != AuthenticationStatus.SUCCESS) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .entity(Map.of("error", "Неверный логин или пароль.")).build();
        }
        return me();
    }

    @GET
    @Path("/me")
    public Response me() {
        return Response.ok(Map.of("username", security.getCallerPrincipal().getName(),
                "role", security.isCallerInRole("ADMIN") ? "ADMIN" : "USER")).build();
    }

    @POST
    @Path("/logout")
    public Response logout() throws ServletException {
        clearSession();
        return Response.noContent().build();
    }

    private void clearSession() throws ServletException {
        request.logout();
        HttpSession session = request.getSession(false);
        if (session != null) session.invalidate();
    }
}
