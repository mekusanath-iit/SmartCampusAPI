package com.smartcampus.model;

import java.util.UUID;

/**
 * A single measurement event captured by a sensor. The server is
 * responsible for assigning {@code id} (UUID) and {@code timestamp}
 * when the client does not supply them; only {@code value} is ever
 * truly required from the caller.
 */
public class SensorReading {

    private String id;         // UUID assigned by the server if absent
    private long timestamp;    // epoch millis - set by server if 0
    private double value;      // the actual measurement

    public SensorReading() {
        // required by Jackson
    }

    public SensorReading(double value) {
        this.id = UUID.randomUUID().toString();
        this.timestamp = System.currentTimeMillis();
        this.value = value;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public double getValue() { return value; }
    public void setValue(double value) { this.value = value; }
}
