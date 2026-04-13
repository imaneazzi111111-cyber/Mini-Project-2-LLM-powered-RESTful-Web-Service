package com.aui.translator.api;

import com.aui.translator.model.TranslationRequest;
import com.aui.translator.model.TranslationResponse;
import com.aui.translator.security.BasicAuthGuard;
import com.aui.translator.service.TranslationService;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.OPTIONS;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/translator")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class TranslatorResource {
    private final TranslationService translationService = new TranslationService();

    @OPTIONS
    @Path("/translate")
    public Response options() {
        return Response.ok().build();
    }

    @POST
    @Path("/translate")
    public TranslationResponse translate(
            @HeaderParam("Authorization") String authorization,
            TranslationRequest request
    ) {
        BasicAuthGuard.ensureAuthorized(authorization);

        if (request == null || request.text() == null || request.text().isBlank()) {
            throw new WebApplicationException("Text is required.", 400);
        }

        try {
            return translationService.translate(request);
        } catch (IllegalStateException exception) {
            throw new WebApplicationException(exception.getMessage(), 503);
        } catch (RuntimeException exception) {
            throw new WebApplicationException(exception.getMessage(), 502);
        }
    }
}
