package ru.varino.lab1.resource;

import jakarta.transaction.RollbackException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import java.util.Map;

@Provider
public class RollbackExceptionMapper implements ExceptionMapper<RollbackException> {
    @Override
    public Response toResponse(RollbackException exception) {
        return Response.status(Response.Status.CONFLICT)
                .type(MediaType.APPLICATION_JSON)
                .entity(Map.of("error", "Транзакция отменена. Повторите операцию."))
                .build();
    }
}
