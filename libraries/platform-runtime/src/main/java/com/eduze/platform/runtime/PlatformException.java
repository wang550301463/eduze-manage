package com.eduze.platform.runtime;

/** A client-safe business error, not an infrastructure exception message. */
public class PlatformException extends RuntimeException {
    private final int status;

    public PlatformException(int status, String message) {
        super(message);
        this.status = status;
    }

    public int getStatus() {
        return status;
    }

    public int status() {
        return status;
    }
}
