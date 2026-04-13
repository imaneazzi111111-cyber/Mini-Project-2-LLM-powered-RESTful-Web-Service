package com.aui.translator.error;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
public class GlobalExceptionMapper implements ExceptionMapper<Throwable> {
    @Override
    public Response toResponse(Throwable exception) {
        if (exception instanceof WebApplicationException webApplicationException) {
            int status = webApplicationException.getResponse().getStatus();
            return Response.status(status)
                    .type(MediaType.APPLICATION_JSON)
                    .entity(new ErrorPayload(webApplicationException.getMessage(), status))
                    .build();
        }

        return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .type(MediaType.APPLICATION_JSON)
                .entity(new ErrorPayload("Unexpected server error.", 500))
                .build();
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record ErrorPayload(
            String message,
            int status
    ) {
    }
}
