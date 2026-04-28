package com.smartcampus.application;

import com.smartcampus.model.Room;
import com.smartcampus.model.Sensor;
import com.smartcampus.model.SensorReading;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory, process-wide data store for the Smart Campus API.
 *
 * Because JAX-RS resource classes are request-scoped by default, they all
 * share this single instance through {@link #getInstance()}. Every map is a
 * {@link ConcurrentHashMap}, and every per-sensor reading list is wrapped in
 * a synchronized list, so concurrent requests cannot corrupt the collections.
 *
 * NOTE: the coursework explicitly forbids databases - everything lives
 * here in memory and is lost when the server restarts.
 */
public final class DataStore {

    private static final DataStore INSTANCE = new DataStore();

    // Keyed by room ID
    private final Map<String, Room> rooms = new ConcurrentHashMap<>();

    // Keyed by sensor ID
    private final Map<String, Sensor> sensors = new ConcurrentHashMap<>();

    // Keyed by sensor ID -> ordered list of readings (most recent last)
    private final Map<String, List<SensorReading>> sensorReadings = new ConcurrentHashMap<>();

    private DataStore() {
        seedData();
    }

    public static DataStore getInstance() {
        return INSTANCE;
    }

    // ---------------------------------------------------------------------
    // Accessors
    // ---------------------------------------------------------------------

    public Map<String, Room> getRooms() {
        return rooms;
    }

    public Map<String, Sensor> getSensors() {
        return sensors;
    }

    public Map<String, List<SensorReading>> getSensorReadings() {
        return sensorReadings;
    }

    // ---------------------------------------------------------------------
    // Seed data - picked deliberately so every video-demo scenario has
    // something to point at:
    //   * a room with sensors    -> triggers 409 on DELETE
    //   * a room with no sensors -> clean 204 DELETE demo
    //   * a MAINTENANCE sensor   -> triggers 403 on POST /readings
    //   * an ACTIVE sensor       -> normal POST /readings path
    // ---------------------------------------------------------------------
    private void seedData() {

        Room r1 = new Room("LIB-301", "Library Quiet Study", 50);
        Room r2 = new Room("LAB-101", "Computer Science Lab", 30);
        Room r3 = new Room("CR-205", "Conference Room 205", 20); // empty

        rooms.put(r1.getId(), r1);
        rooms.put(r2.getId(), r2);
        rooms.put(r3.getId(), r3);

        Sensor s1 = new Sensor("TEMP-001", "Temperature", "ACTIVE",      22.5, "LIB-301");
        Sensor s2 = new Sensor("CO2-001",  "CO2",         "ACTIVE",     420.0, "LAB-101");
        Sensor s3 = new Sensor("OCC-001",  "Occupancy",   "MAINTENANCE",  0.0, "LAB-101");

        sensors.put(s1.getId(), s1);
        sensors.put(s2.getId(), s2);
        sensors.put(s3.getId(), s3);

        r1.getSensorIds().add(s1.getId());
        r2.getSensorIds().add(s2.getId());
        r2.getSensorIds().add(s3.getId());

        // Every sensor gets a synchronized reading list so concurrent
        // appends from multiple threads are safe.
        sensorReadings.put(s1.getId(), Collections.synchronizedList(new ArrayList<>()));
        sensorReadings.put(s2.getId(), Collections.synchronizedList(new ArrayList<>()));
        sensorReadings.put(s3.getId(), Collections.synchronizedList(new ArrayList<>()));
    }
}
