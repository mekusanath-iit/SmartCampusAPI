package com.smartcampus.exception;

/**
 * Thrown when a client tries to DELETE a room that still has one or more
 * sensors attached. Mapped to HTTP 409 Conflict by
 * {@link RoomNotEmptyExceptionMapper}.
 */
public class RoomNotEmptyException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public RoomNotEmptyException(String message) {
        super(message);
    }
}
