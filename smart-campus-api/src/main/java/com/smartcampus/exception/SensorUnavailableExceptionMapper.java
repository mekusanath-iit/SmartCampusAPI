package com.smartcampus.exception;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Maps {@link SensorUnavailableException} to HTTP 403 Forbidden.
 *
 * <p>Part 5.3 of the coursework - when a sensor is in MAINTENANCE it
 * is physically offline and cannot accept new readings, so the server
 * signals the client that the operation is refused for policy reasons
 * (as opposed to authentication or resource-not-found reasons).</p>
 */
@Provider
public class SensorUnavailableExceptionMapper
        implements ExceptionMapper<SensorUnavailableException> {

    @Override
    public Response toResponse(SensorUnavailableException ex) {

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", "error");
        body.put("httpCode", 403);
        body.put("error", "Forbidden");
        body.put("message", ex.getMessage());
        body.put("hint",
                "Update the sensor status to ACTIVE before submitting readings.");

        return Response.status(Response.Status.FORBIDDEN)
                .type(MediaType.APPLICATION_JSON)
                .entity(body)
                .build();
    }
}
