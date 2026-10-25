package ru.varino.lab1.resource;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import ru.varino.lab1.exception.MovieNameConflictException;

import java.util.Map;

@Provider
public class MovieNameConflictExceptionMapper implements ExceptionMapper<MovieNameConflictException> {
    @Override
    public Response toResponse(MovieNameConflictException exception) {
        return Response.status(Response.Status.CONFLICT).type(MediaType.APPLICATION_JSON)
                .entity(Map.of("error", exception.getMessage())).build();
    }
}
