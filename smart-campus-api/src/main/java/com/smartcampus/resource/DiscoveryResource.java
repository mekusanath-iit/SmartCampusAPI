package com.smartcampus.resource;

import com.smartcampus.application.DataStore;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Discovery / "root" endpoint for the API.
 *
 * <p>A GET on {@code /api/v1} returns a single JSON document describing
 * the API - its version, a contact address, the URIs for every
 * top-level resource collection, and HATEOAS navigation links. This is
 * the client's first stop: from here it can discover the rest of the
 * API without needing out-of-band documentation.</p>
 */
@Path("/")
@Produces(MediaType.APPLICATION_JSON)
public class DiscoveryResource {

    private final DataStore store = DataStore.getInstance();

    @GET
    public Response discover() {

        // LinkedHashMap keeps field order stable in the JSON response.
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("api", "Smart Campus Sensor and Room Management API");
        body.put("version", "1.0.0");
        body.put("description",
                "RESTful service for managing campus rooms and the IoT sensors they contain.");
        body.put("status", "operational");

        // Administrative contact
        Map<String, String> contact = new LinkedHashMap<>();
        contact.put("name", "Smart Campus Platform Team");
        contact.put("email", "admin@smartcampus.ac.uk");
        contact.put("institution", "University of Westminster");
        body.put("contact", contact);

        // Live counts, so an operator can sanity-check data at a glance.
        Map<String, Integer> stats = new LinkedHashMap<>();
        stats.put("totalRooms", store.getRooms().size());
        stats.put("totalSensors", store.getSensors().size());
        body.put("stats", stats);

        // Resource collection map required by the spec.
        Map<String, String> resources = new LinkedHashMap<>();
        resources.put("rooms", "/api/v1/rooms");
        resources.put("sensors", "/api/v1/sensors");
        body.put("resources", resources);

        // HATEOAS links - navigable entry points for a generic client.
        Map<String, String> links = new LinkedHashMap<>();
        links.put("self", "/api/v1");
        links.put("rooms", "/api/v1/rooms");
        links.put("sensors", "/api/v1/sensors");
        links.put("sensorsByType",
                "/api/v1/sensors?type={Temperature|CO2|Occupancy|...}");
        body.put("_links", links);

        return Response.ok(body).build();
    }
}
