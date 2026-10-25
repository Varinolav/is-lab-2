package ru.varino.lab1.resource;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import ru.varino.lab1.exception.ImportValidationException;

import java.util.Map;

@Provider
public class ImportValidationExceptionMapper implements ExceptionMapper<ImportValidationException> {
    @Override
    public Response toResponse(ImportValidationException exception) {
        return Response.status(Response.Status.BAD_REQUEST)
                .type(MediaType.APPLICATION_JSON)
                .entity(Map.of("error", exception.getMessage()))
                .build();
    }
}
