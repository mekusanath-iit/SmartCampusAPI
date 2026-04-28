package com.smartcampus.filter;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.ext.Provider;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Cross-cutting request / response logging filter (Part 5.5).
 *
 * <p>Implementing both {@link ContainerRequestFilter} and
 * {@link ContainerResponseFilter} lets a single class observe every
 * incoming request and every outgoing response without the resource
 * classes having to know about it. This is JAX-RS's answer to the
 * "cross-cutting concern" problem.</p>
 *
 * <p>We use {@link java.util.logging.Logger} as required by the
 * coursework specification. The output also goes to stdout so it shows
 * up in Tomcat's catalina.out regardless of logging.properties setup -
 * handy during the video demo.</p>
 */
@Provider
public class LoggingFilter
        implements ContainerRequestFilter, ContainerResponseFilter {

    private static final Logger LOGGER =
            Logger.getLogger(LoggingFilter.class.getName());

    // -----------------------------------------------------------------
    // Incoming request
    // -----------------------------------------------------------------
    @Override
    public void filter(ContainerRequestContext requestContext)
            throws IOException {

        String message = String.format(
                "[SMART-CAMPUS][REQUEST ] %s %s",
                requestContext.getMethod(),
                requestContext.getUriInfo().getRequestUri());

        LOGGER.log(Level.INFO, message);
        System.out.println(message);
    }

    // -----------------------------------------------------------------
    // Outgoing response
    // -----------------------------------------------------------------
    @Override
    public void filter(ContainerRequestContext requestContext,
                       ContainerResponseContext responseContext)
            throws IOException {

        String message = String.format(
                "[SMART-CAMPUS][RESPONSE] %s %s -> %d %s",
                requestContext.getMethod(),
                requestContext.getUriInfo().getRequestUri(),
                responseContext.getStatus(),
                responseContext.getStatusInfo().getReasonPhrase());

        LOGGER.log(Level.INFO, message);
        System.out.println(message);
    }
}
