package ru.varino.lab1.resource;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import ru.varino.lab1.exception.UsernameAlreadyExistsException;

import java.util.Map;

@Provider
public class UsernameAlreadyExistsExceptionMapper implements ExceptionMapper<UsernameAlreadyExistsException> {
    @Override
    public Response toResponse(UsernameAlreadyExistsException exception) {
        return Response.status(Response.Status.CONFLICT).type(MediaType.APPLICATION_JSON)
                .entity(Map.of("error", exception.getMessage())).build();
    }
}
