package com.smartcampus.resource;

import com.smartcampus.application.DataStore;
import com.smartcampus.exception.LinkedResourceNotFoundException;
import com.smartcampus.model.Room;
import com.smartcampus.model.Sensor;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Resource class for the {@code /api/v1/sensors} collection.
 *
 * <p>Supports:</p>
 * <ul>
 *   <li>GET  /sensors            - list all sensors, with optional {@code ?type=} filter (Part 3.2)</li>
 *   <li>POST /sensors            - register a new sensor; referenced roomId must exist (Part 3.1)</li>
 *   <li>GET  /sensors/{id}       - fetch a specific sensor by ID</li>
 *   <li>PUT  /sensors/{id}       - update a sensor's status/type (used to trigger MAINTENANCE for demo)</li>
 *   <li>DELETE /sensors/{id}     - remove a sensor and detach it from its parent room</li>
 *   <li>Sub-resource locator at {id}/readings → {@link SensorReadingResource} (Part 4.1)</li>
 * </ul>
 */
@Path("/sensors")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class SensorResource {

    private final DataStore store = DataStore.getInstance();

    @Context
    private UriInfo uriInfo;

    // -------------------------------------------------------------------------
    // GET /sensors  -  optional ?type= filter (Part 3.2)
    // -------------------------------------------------------------------------
    @GET
    public Response getAllSensors(@QueryParam("type") String type) {

        List<Sensor> sensors = new ArrayList<>(store.getSensors().values());

        if (type != null && !type.trim().isEmpty()) {
            // Case-insensitive match so "co2", "CO2" and "Co2" all work.
            sensors = sensors.stream()
                    .filter(s -> type.equalsIgnoreCase(s.getType()))
                    .collect(Collectors.toList());
        }

        return Response.ok(sensors).build();
    }

    // -------------------------------------------------------------------------
    // POST /sensors  -  register a new sensor (Part 3.1)
    //
    // Validation order:
    //   1. payload present and ID supplied
    //   2. ID not already taken
    //   3. referenced roomId actually exists — otherwise 422
    // Then we attach the sensor to the room and initialise an empty readings list.
    // -------------------------------------------------------------------------
    @POST
    public Response createSensor(Sensor sensor) {

        if (sensor == null
                || sensor.getId() == null
                || sensor.getId().trim().isEmpty()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(errorBody("Sensor ID is required."))
                    .build();
        }

        if (sensor.getType() == null || sensor.getType().trim().isEmpty()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(errorBody("Sensor type is required."))
                    .build();
        }

        if (store.getSensors().containsKey(sensor.getId())) {
            return Response.status(Response.Status.CONFLICT)
                    .entity(errorBody(
                            "Sensor '" + sensor.getId() + "' already exists."))
                    .build();
        }

        // Part 5.2 / Part 3.1: dependency validation - roomId must reference an existing room.
        if (sensor.getRoomId() == null
                || !store.getRooms().containsKey(sensor.getRoomId())) {
            throw new LinkedResourceNotFoundException(
                    "Cannot register sensor: room '"
                            + sensor.getRoomId()
                            + "' does not exist. Provide a valid roomId.");
        }

        // Normalise and validate status
        String status = (sensor.getStatus() == null || sensor.getStatus().trim().isEmpty())
                ? "ACTIVE"
                : sensor.getStatus().toUpperCase();
        if (!status.equals("ACTIVE")
                && !status.equals("MAINTENANCE")
                && !status.equals("OFFLINE")) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(errorBody(
                            "Invalid status. Must be ACTIVE, MAINTENANCE, or OFFLINE."))
                    .build();
        }
        sensor.setStatus(status);

        // Commit sensor to store
        store.getSensors().put(sensor.getId(), sensor);
        store.getSensorReadings().put(sensor.getId(),
                Collections.synchronizedList(new ArrayList<>()));

        // Attach sensor ID to parent room (synchronized for thread safety)
        Room room = store.getRooms().get(sensor.getRoomId());
        synchronized (room.getSensorIds()) {
            room.getSensorIds().add(sensor.getId());
        }

        URI location = uriInfo.getAbsolutePathBuilder()
                .path(sensor.getId())
                .build();

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("message", "Sensor registered successfully.");
        body.put("sensor", sensor);
        body.put("_links", linkMap(sensor.getId()));

        return Response.created(location).entity(body).build();
    }

    // -------------------------------------------------------------------------
    // GET /sensors/{sensorId}
    // -------------------------------------------------------------------------
    @GET
    @Path("/{sensorId}")
    public Response getSensorById(@PathParam("sensorId") String sensorId) {
        Sensor sensor = store.getSensors().get(sensorId);
        if (sensor == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(errorBody("Sensor '" + sensorId + "' not found."))
                    .build();
        }
        return Response.ok(sensor).build();
    }

    // -------------------------------------------------------------------------
    // PUT /sensors/{sensorId}
    //
    // Allows updating a sensor's status and/or type.
    // Primary demo use: flip a sensor to MAINTENANCE to trigger the 403 flow
    // when a client then tries to POST a new reading to that sensor.
    // -------------------------------------------------------------------------
    @PUT
    @Path("/{sensorId}")
    public Response updateSensor(@PathParam("sensorId") String sensorId,
                                 Sensor updated) {

        Sensor existing = store.getSensors().get(sensorId);
        if (existing == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(errorBody("Sensor '" + sensorId + "' not found."))
                    .build();
        }
        if (updated == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(errorBody("Request body is required."))
                    .build();
        }

        // Only status and type are mutable; roomId and id are immutable.
        if (updated.getStatus() != null && !updated.getStatus().trim().isEmpty()) {
            String status = updated.getStatus().toUpperCase();
            if (!status.equals("ACTIVE")
                    && !status.equals("MAINTENANCE")
                    && !status.equals("OFFLINE")) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(errorBody(
                                "Invalid status. Must be ACTIVE, MAINTENANCE, or OFFLINE."))
                        .build();
            }
            existing.setStatus(status);
        }
        if (updated.getType() != null && !updated.getType().trim().isEmpty()) {
            existing.setType(updated.getType());
        }

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("message", "Sensor '" + sensorId + "' updated successfully.");
        body.put("sensor", existing);
        body.put("_links", linkMap(sensorId));
        return Response.ok(body).build();
    }

    // -------------------------------------------------------------------------
    // DELETE /sensors/{sensorId}
    //
    // Removes the sensor from the store and detaches it from its parent room's
    // sensorIds list. This makes it possible to empty a room before deleting it,
    // which is required to demonstrate the successful room-delete flow in the
    // video demo (Part 2.2 requires a non-empty room to trigger 409; the demo
    // also needs to show a successful delete of a room that becomes empty).
    //
    // Idempotent: calling DELETE on an already-deleted sensor returns 404 on
    // subsequent calls — the server state does not change after the first call.
    // -------------------------------------------------------------------------
    @DELETE
    @Path("/{sensorId}")
    public Response deleteSensor(@PathParam("sensorId") String sensorId) {

        Sensor sensor = store.getSensors().get(sensorId);
        if (sensor == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(errorBody("Sensor '" + sensorId + "' not found."))
                    .build();
        }

        // Detach from parent room
        Room room = store.getRooms().get(sensor.getRoomId());
        if (room != null) {
            synchronized (room.getSensorIds()) {
                room.getSensorIds().remove(sensorId);
            }
        }

        // Remove sensor and its reading history
        store.getSensors().remove(sensorId);
        store.getSensorReadings().remove(sensorId);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("message", "Sensor '" + sensorId + "' deleted successfully.");
        body.put("status", "success");
        return Response.ok(body).build();
    }

    // -------------------------------------------------------------------------
    // Sub-resource locator  (Part 4.1)
    //
    // NOT annotated with @GET / @POST — JAX-RS treats any @Path-only method
    // as a locator and invokes the HTTP-method annotations on the returned object.
    // This is the Sub-Resource Locator pattern: it keeps reading logic in its
    // own dedicated class (SensorReadingResource) rather than bloating this one.
    // -------------------------------------------------------------------------
    @Path("/{sensorId}/readings")
    public SensorReadingResource getReadingResource(@PathParam("sensorId") String sensorId) {

        Sensor sensor = store.getSensors().get(sensorId);
        if (sensor == null) {
            // NotFoundException is mapped natively by JAX-RS → 404
            throw new NotFoundException("Sensor '" + sensorId + "' not found.");
        }
        return new SensorReadingResource(sensorId);
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private Map<String, Object> errorBody(String message) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", "error");
        body.put("error", message);
        return body;
    }

    private Map<String, String> linkMap(String sensorId) {
        Map<String, String> links = new LinkedHashMap<>();
        links.put("self", "/api/v1/sensors/" + sensorId);
        links.put("readings", "/api/v1/sensors/" + sensorId + "/readings");
        links.put("all-sensors", "/api/v1/sensors");
        return links;
    }
}
