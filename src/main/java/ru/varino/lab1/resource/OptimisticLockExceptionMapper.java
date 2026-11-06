package ru.varino.lab1.resource;

import jakarta.persistence.OptimisticLockException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import java.util.Map;

@Provider
public class OptimisticLockExceptionMapper implements ExceptionMapper<OptimisticLockException> {
    @Override
    public Response toResponse(OptimisticLockException exception) {
        return Response.status(Response.Status.CONFLICT)
                .type(MediaType.APPLICATION_JSON)
                .entity(Map.of("error", "Объект уже изменён или удалён другим пользователем. Обновите данные."))
                .build();
    }
}
