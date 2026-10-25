package ru.varino.lab1.resource;

import jakarta.inject.Inject;
import jakarta.security.enterprise.SecurityContext;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import ru.varino.lab1.entity.ImportOperation;
import ru.varino.lab1.service.HistoryService;

import java.util.List;

@Path("/imports")
@Produces(MediaType.APPLICATION_JSON)
public class ImportHistoryResource {
    @Inject
    private HistoryService historyService;

    @Inject
    private SecurityContext security;

    @GET
    public List<ImportOperation> history() {
        return historyService.findForUser(security.getCallerPrincipal().getName(),
                security.isCallerInRole("ADMIN"));
    }
}
