package ru.varino.lab1.resource;

import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.EntityPart;
import ru.varino.lab1.entity.Movie;
import ru.varino.lab1.service.ImportOrchestrator;
import ru.varino.lab1.service.HistoryService;
import jakarta.security.enterprise.SecurityContext;
import ru.varino.lab1.service.MovieService;

import java.util.List;
import java.util.Arrays;
import java.io.IOException;

@Path("/movies")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class MovieResource {

    @Inject
    private MovieService movieService;

    @Inject
    private ChangeEvents events;

    @Inject
    private ImportOrchestrator importOrchestrator;

    @Inject
    private HistoryService historyService;

    @Inject
    private SecurityContext security;

    @POST
    public Response create(@Valid Movie movie) {
        movieService.save(movie);
        events.publish();

        return Response.status(Response.Status.CREATED)
                .entity(movie)
                .build();
    }

    @GET
    public Response getMovies() {
        List<Movie> movies = movieService.findAll();
        return Response.ok(movies).build();
    }

    @POST
    @Path("/import")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public Response importMovies(@FormParam("file") EntityPart file) {
        String username = security.getCallerPrincipal().getName();
        if (file == null) {
            historyService.saveFailed(username);
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(java.util.Map.of("error", "Прикрепите JSON-файл в поле file."))
                    .build();
        }
        List<Movie> movies;
        try {
            Movie[] parsed = file.getContent(Movie[].class);
            movies = parsed == null ? null : Arrays.asList(parsed);
        } catch (IOException | RuntimeException e) {
            historyService.saveFailed(username);
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(java.util.Map.of("error", "Не удалось прочитать файл. Файл должен содержать корректный JSON-массив фильмов."))
                    .build();
        }
        int importedCount = importOrchestrator.initiateImport(movies, username);
        events.publish();
        return Response.status(Response.Status.CREATED)
                .entity(java.util.Map.of("importedCount", importedCount))
                .build();
    }

    @GET
    @Path("/{id}")
    public Response getMovie(@PathParam("id") Integer id) {
        Movie movie = movieService.findById(id);

        if (movie == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        return Response.ok(movie).build();
    }

    @PUT
    @Path("/{id}")
    public Response update(@PathParam("id") Integer id, @Valid Movie movie) {
        Movie updatedMovie = movieService.update(id, movie);
        if (updatedMovie == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        events.publish();
        return Response.ok(updatedMovie).build();
    }

    @DELETE
    @Path("/{id}")
    public Response delete(@PathParam("id") Integer id) {
        if (!movieService.deleteById(id)) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        events.publish();
        return Response.noContent().build();
    }
}
