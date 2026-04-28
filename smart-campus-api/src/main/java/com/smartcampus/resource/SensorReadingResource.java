package com.smartcampus.resource;

import com.smartcampus.application.DataStore;
import com.smartcampus.exception.SensorUnavailableException;
import com.smartcampus.model.Sensor;
import com.smartcampus.model.SensorReading;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.POST;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Sub-resource for the path
 * {@code /api/v1/sensors/{sensorId}/readings}.
 *
 * <p>This class is NOT annotated with {@link javax.ws.rs.Path @Path} at
 * the class level and is NOT registered in the JAX-RS Application. It is
 * created on demand by the sub-resource locator method in
 * {@link SensorResource#getReadingResource(String)}, which passes in the
 * sensor ID through the constructor.</p>
 *
 * <p>This pattern isolates reading-specific logic from the sensor
 * controller and keeps the SensorResource class focused on sensor-level
 * operations only.</p>
 */
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class SensorReadingResource {

    private final String sensorId;
    private final DataStore store = DataStore.getInstance();

    public SensorReadingResource(String sensorId) {
        this.sensorId = sensorId;
    }

    // ---------------------------------------------------------------------
    // GET /sensors/{sensorId}/readings  -  entire reading history
    // ---------------------------------------------------------------------
    @GET
    public Response getReadings() {

        List<SensorReading> readings = store.getSensorReadings()
                .getOrDefault(sensorId, new ArrayList<>());

        // Copy under the list's intrinsic lock for a consistent snapshot.
        List<SensorReading> snapshot;
        synchronized (readings) {
            snapshot = new ArrayList<>(readings);
        }

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("sensorId", sensorId);
        body.put("totalReadings", snapshot.size());
        body.put("readings", snapshot);
        body.put("_links", linkMap());
        return Response.ok(body).build();
    }

    // ---------------------------------------------------------------------
    // POST /sensors/{sensorId}/readings
    //
    // Business rule (Part 5.3): if the parent sensor is currently in
    // MAINTENANCE we refuse the reading with HTTP 403.
    //
    // Side effect (Part 4.2): a successful append updates the parent
    // sensor's currentValue so subsequent GET /sensors/{id} reflects
    // the latest measurement.
    // ---------------------------------------------------------------------
    @POST
    public Response addReading(SensorReading reading) {

        Sensor sensor = store.getSensors().get(sensorId);
        if (sensor == null) {
            // Belt-and-braces: the sub-resource locator already guards
            // against this, but if someone ever invokes the class directly
            // we still return a clean 404 instead of an NPE.
            throw new NotFoundException(
                    "Sensor '" + sensorId + "' not found.");
        }

        if ("MAINTENANCE".equalsIgnoreCase(sensor.getStatus())) {
            throw new SensorUnavailableException(
                    "Sensor '" + sensorId
                            + "' is in MAINTENANCE and cannot accept new readings."
                            + " Flip its status to ACTIVE before posting readings.");
        }

        if (reading == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(errorBody("Reading body is required."))
                    .build();
        }

        // Server-assigns ID and timestamp when absent
        if (reading.getId() == null || reading.getId().trim().isEmpty()) {
            reading.setId(UUID.randomUUID().toString());
        }
        if (reading.getTimestamp() == 0L) {
            reading.setTimestamp(System.currentTimeMillis());
        }

        // Append to the per-sensor list (created on demand)
        List<SensorReading> readings = store.getSensorReadings()
                .computeIfAbsent(sensorId,
                        k -> Collections.synchronizedList(new ArrayList<>()));
        synchronized (readings) {
            readings.add(reading);
        }

        // Side effect: keep parent sensor's currentValue in sync.
        sensor.setCurrentValue(reading.getValue());

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("message", "Reading recorded successfully.");
        body.put("reading", reading);
        body.put("updatedSensorValue", sensor.getCurrentValue());
        body.put("_links", linkMap());

        return Response.status(Response.Status.CREATED)
                .entity(body)
                .build();
    }

    // ---------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------

    private Map<String, Object> errorBody(String message) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", "error");
        body.put("error", message);
        return body;
    }

    private Map<String, String> linkMap() {
        Map<String, String> links = new LinkedHashMap<>();
        links.put("self", "/api/v1/sensors/" + sensorId + "/readings");
        links.put("sensor", "/api/v1/sensors/" + sensorId);
        return links;
    }
}
