package com.smartcampus.exception;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Maps {@link LinkedResourceNotFoundException} to HTTP 422
 * Unprocessable Entity.
 *
 * <p>422 is used rather than 404 because the request itself is
 * well-formed JSON targeting a valid collection endpoint - the problem is
 * with a referenced identifier inside the payload, not the URL path.</p>
 */
@Provider
public class LinkedResourceNotFoundExceptionMapper
        implements ExceptionMapper<LinkedResourceNotFoundException> {

    private static final int UNPROCESSABLE_ENTITY = 422;

    @Override
    public Response toResponse(LinkedResourceNotFoundException ex) {

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", "error");
        body.put("httpCode", UNPROCESSABLE_ENTITY);
        body.put("error", "Unprocessable Entity");
        body.put("message", ex.getMessage());
        body.put("hint",
                "Ensure every foreign-key field (e.g. roomId) points to an existing resource.");

        return Response.status(UNPROCESSABLE_ENTITY)
                .type(MediaType.APPLICATION_JSON)
                .entity(body)
                .build();
    }
}
