package com.smartcampus.resource;

import com.smartcampus.application.DataStore;
import com.smartcampus.exception.RoomNotEmptyException;
import com.smartcampus.model.Room;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Resource class for the {@code /api/v1/rooms} collection.
 *
 * <p>Supports:</p>
 * <ul>
 *   <li>GET /rooms - list every room</li>
 *   <li>POST /rooms - register a new room (returns 201 + Location)</li>
 *   <li>GET /rooms/{roomId} - fetch a single room by ID</li>
 *   <li>DELETE /rooms/{roomId} - decommission a room (blocked by 409
 *       when the room still has sensors attached)</li>
 * </ul>
 */
@Path("/rooms")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class RoomResource {

    private final DataStore store = DataStore.getInstance();

    /** Injected by JAX-RS - used to build the Location header on POST. */
    @Context
    private UriInfo uriInfo;

    // ---------------------------------------------------------------------
    // GET /rooms  - list all rooms
    // ---------------------------------------------------------------------
    @GET
    public Response getAllRooms() {
        List<Room> rooms = new ArrayList<>(store.getRooms().values());
        return Response.ok(rooms).build();
    }

    // ---------------------------------------------------------------------
    // POST /rooms - create a new room
    //
    // On success we return 201 Created with a Location header pointing to
    // the new resource, as required by the rubric.
    // ---------------------------------------------------------------------
    @POST
    public Response createRoom(Room room) {

        if (room == null
                || room.getId() == null
                || room.getId().trim().isEmpty()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(errorBody("Room ID is required."))
                    .build();
        }

        if (room.getCapacity() < 0) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(errorBody("Capacity must be non-negative."))
                    .build();
        }

        if (store.getRooms().containsKey(room.getId())) {
            return Response.status(Response.Status.CONFLICT)
                    .entity(errorBody(
                            "Room with ID '" + room.getId() + "' already exists."))
                    .build();
        }

        // Defensive: ignore any sensorIds the client might have supplied in
        // the payload - sensors can only be attached via POST /sensors.
        if (room.getSensorIds() == null) {
            room.setSensorIds(new ArrayList<>());
        } else {
            room.getSensorIds().clear();
        }

        store.getRooms().put(room.getId(), room);

        // Build the Location URI, e.g. .../api/v1/rooms/LIB-301
        URI location = uriInfo.getAbsolutePathBuilder()
                .path(room.getId())
                .build();

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("message", "Room created successfully.");
        body.put("room", room);
        body.put("_links", linkMap(room.getId()));

        // Response.created(URI) sets the status to 201 and the Location header.
        return Response.created(location)
                .entity(body)
                .build();
    }

    // ---------------------------------------------------------------------
    // GET /rooms/{roomId}
    // ---------------------------------------------------------------------
    @GET
    @Path("/{roomId}")
    public Response getRoomById(@PathParam("roomId") String roomId) {
        Room room = store.getRooms().get(roomId);
        if (room == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(errorBody("Room '" + roomId + "' not found."))
                    .build();
        }
        return Response.ok(room).build();
    }

    // ---------------------------------------------------------------------
    // DELETE /rooms/{roomId}
    //
    // Business rule (Part 2.2): a room that still has sensors attached is
    // considered "occupied" and cannot be decommissioned. In that case we
    // throw a RoomNotEmptyException which is mapped to HTTP 409.
    //
    // Idempotency: if the room is already absent we return 404, which is
    // the standard JAX-RS default for a missing resource. The underlying
    // server state does NOT change on repeat DELETE calls - that is what
    // matters for idempotency - see the report for full reasoning.
    // ---------------------------------------------------------------------
    @DELETE
    @Path("/{roomId}")
    public Response deleteRoom(@PathParam("roomId") String roomId) {

        Room room = store.getRooms().get(roomId);
        if (room == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(errorBody("Room '" + roomId + "' not found."))
                    .build();
        }

        // Block deletion if there are still sensors linked to the room.
        // Synchronize on the list to get a consistent view while reading.
        synchronized (room.getSensorIds()) {
            if (!room.getSensorIds().isEmpty()) {
                throw new RoomNotEmptyException(
                        "Room '" + roomId + "' cannot be deleted. It still has "
                                + room.getSensorIds().size()
                                + " sensor(s) assigned. Remove or reassign them first.");
            }
        }

        store.getRooms().remove(roomId);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("message", "Room '" + roomId + "' deleted successfully.");
        body.put("status", "success");
        return Response.ok(body).build();
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

    private Map<String, String> linkMap(String roomId) {
        Map<String, String> links = new LinkedHashMap<>();
        links.put("self", "/api/v1/rooms/" + roomId);
        links.put("all-rooms", "/api/v1/rooms");
        return links;
    }
}
