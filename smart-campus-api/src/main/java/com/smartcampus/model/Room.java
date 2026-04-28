package com.smartcampus.model;

import java.util.ArrayList;
import java.util.List;

/**
 * A physical room on the Smart Campus. Rooms are containers for sensors -
 * the list of {@code sensorIds} gives the foreign-key relationship in
 * reverse so we can answer "what sensors are in this room?" without a scan.
 */
public class Room {

    private String id;                               // e.g. "LIB-301"
    private String name;                             // human-readable label
    private int capacity;                            // max occupancy
    private List<String> sensorIds = new ArrayList<>();

    public Room() {
        // required by Jackson for JSON deserialization
    }

    public Room(String id, String name, int capacity) {
        this.id = id;
        this.name = name;
        this.capacity = capacity;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public int getCapacity() { return capacity; }
    public void setCapacity(int capacity) { this.capacity = capacity; }

    public List<String> getSensorIds() { return sensorIds; }
    public void setSensorIds(List<String> sensorIds) { this.sensorIds = sensorIds; }
}
