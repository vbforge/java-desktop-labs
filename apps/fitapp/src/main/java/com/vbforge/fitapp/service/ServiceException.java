package com.vbforge.fitapp.service;

/** A user-facing error (validation failure, wrong credentials, etc.). The message is safe to display. */
public class ServiceException extends RuntimeException {
    public ServiceException(String message) {
        super(message);
    }

    public ServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}
