package com.smartcampus.model;

/**
 * A hardware sensor deployed in a Room. The {@code roomId} acts as a
 * foreign key back to the parent Room, and {@code status} is constrained
 * to the string values ACTIVE, MAINTENANCE or OFFLINE.
 */
public class Sensor {

    private String id;           // e.g. "TEMP-001"
    private String type;         // "Temperature", "CO2", "Occupancy", ...
    private String status;       // ACTIVE | MAINTENANCE | OFFLINE
    private double currentValue; // most recent reading value
    private String roomId;       // FK -> Room.id

    public Sensor() {
        // required by Jackson
    }

    public Sensor(String id, String type, String status,
                  double currentValue, String roomId) {
        this.id = id;
        this.type = type;
        this.status = status;
        this.currentValue = currentValue;
        this.roomId = roomId;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public double getCurrentValue() { return currentValue; }
    public void setCurrentValue(double currentValue) { this.currentValue = currentValue; }

    public String getRoomId() { return roomId; }
    public void setRoomId(String roomId) { this.roomId = roomId; }
}
