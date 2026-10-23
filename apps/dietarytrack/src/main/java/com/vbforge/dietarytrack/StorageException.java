package com.vbforge.dietarytrack;

/** Thrown when reading or writing the database or CSV file fails. */
public class StorageException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public StorageException(String message, Throwable cause) {
        super(message, cause);
    }
}
