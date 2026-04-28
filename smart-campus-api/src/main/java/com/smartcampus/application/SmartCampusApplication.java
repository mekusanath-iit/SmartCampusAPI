package com.smartcampus.application;

import com.smartcampus.exception.GlobalExceptionMapper;
import com.smartcampus.exception.LinkedResourceNotFoundExceptionMapper;
import com.smartcampus.exception.RoomNotEmptyExceptionMapper;
import com.smartcampus.exception.SensorUnavailableExceptionMapper;
import com.smartcampus.filter.LoggingFilter;
import com.smartcampus.resource.DiscoveryResource;
import com.smartcampus.resource.RoomResource;
import com.smartcampus.resource.SensorResource;
import org.glassfish.jersey.jackson.JacksonFeature;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;
import java.util.HashSet;
import java.util.Set;

/**
 * Root JAX-RS Application for the Smart Campus API.
 *
 * The @ApplicationPath annotation establishes the versioned entry point
 * (/api/v1) required by the coursework specification. Every resource
 * path declared in individual resource classes is relative to this base.
 *
 * IMPORTANT: Because getClasses() returns a non-empty set, Jersey disables
 * its auto-discovery mechanism. This means JacksonFeature MUST be registered
 * here explicitly - otherwise no JSON MessageBodyWriter/Reader will be available
 * and every request/response involving JSON will fail with 415 or 500.
 */
@ApplicationPath("/api/v1")
public class SmartCampusApplication extends Application {

    @Override
    public Set<Class<?>> getClasses() {
        Set<Class<?>> classes = new HashSet<>();

        // --- Jackson JSON provider (MUST be explicit when getClasses() is non-empty) ---
        // Jersey disables auto-discovery when getClasses() returns a non-empty set,
        // so we must register JacksonFeature manually to get JSON serialization working.
        classes.add(JacksonFeature.class);

        // --- JAX-RS resource classes ---------------------------------------------
        classes.add(DiscoveryResource.class);     // GET /api/v1
        classes.add(RoomResource.class);          // /api/v1/rooms
        classes.add(SensorResource.class);        // /api/v1/sensors
        // SensorReadingResource is NOT registered here - it is returned
        // by the sub-resource locator in SensorResource (Part 4.1).

        // --- Exception mappers ---------------------------------------------------
        classes.add(RoomNotEmptyExceptionMapper.class);            // 409
        classes.add(LinkedResourceNotFoundExceptionMapper.class);  // 422
        classes.add(SensorUnavailableExceptionMapper.class);       // 403
        classes.add(GlobalExceptionMapper.class);                  // 500 - catch-all

        // --- Cross-cutting filters -----------------------------------------------
        classes.add(LoggingFilter.class);

        return classes;
    }
}
