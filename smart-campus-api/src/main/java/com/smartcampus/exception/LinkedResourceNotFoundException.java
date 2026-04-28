package com.smartcampus.exception;

/**
 * Thrown when a request's body is syntactically valid JSON, but references
 * another resource that does not exist in the system (e.g. creating a
 * sensor with a {@code roomId} that isn't in the room registry).
 *
 * Mapped to HTTP 422 Unprocessable Entity by
 * {@link LinkedResourceNotFoundExceptionMapper}.
 */
public class LinkedResourceNotFoundException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public LinkedResourceNotFoundException(String message) {
        super(message);
    }
}
