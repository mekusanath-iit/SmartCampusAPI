package com.smartcampus.exception;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Global "safety net" exception mapper (Part 5.4).
 *
 * <p>Catches any {@link Throwable} that has not been caught by a more
 * specific mapper and returns a sanitised HTTP 500 response. This
 * prevents raw Java stack traces, class names or framework paths from
 * ever leaking to external API consumers.</p>
 *
 * <p>We intentionally delegate {@link WebApplicationException} back to
 * the JAX-RS runtime so that responses like the built-in 404 (from
 * NotFoundException) or 405 (from method-not-allowed) keep their
 * correct status codes rather than being re-wrapped as 500.</p>
 */
@Provider
public class GlobalExceptionMapper implements ExceptionMapper<Throwable> {

    private static final Logger LOGGER =
            Logger.getLogger(GlobalExceptionMapper.class.getName());

    @Override
    public Response toResponse(Throwable ex) {

        // Let JAX-RS handle its own framework-level exceptions.
        if (ex instanceof WebApplicationException) {
            return ((WebApplicationException) ex).getResponse();
        }

        // Log full stack trace SERVER-SIDE for debugging, but NEVER expose
        // it to the client. This is the whole point of this mapper.
        LOGGER.log(Level.SEVERE,
                "Unhandled internal error: " + ex.getMessage(), ex);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", "error");
        body.put("httpCode", 500);
        body.put("error", "Internal Server Error");
        body.put("message",
                "An unexpected error occurred. Please contact the system administrator.");

        return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .type(MediaType.APPLICATION_JSON)
                .entity(body)
                .build();
    }
}
