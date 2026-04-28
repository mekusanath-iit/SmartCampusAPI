package com.smartcampus.exception;

/**
 * Thrown when a client attempts an operation that is forbidden by the
 * sensor's current state - in particular, posting a reading to a sensor
 * that is flagged as MAINTENANCE.
 *
 * Mapped to HTTP 403 Forbidden by {@link SensorUnavailableExceptionMapper}.
 */
public class SensorUnavailableException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public SensorUnavailableException(String message) {
        super(message);
    }
}
