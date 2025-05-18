package com.github.cgks.exceptions;

/**
 * Custom exception for database-related errors.
 */
public class DatabaseException extends MiningException {
    public DatabaseException(String message) {
        super(message);
    }

    public DatabaseException(String message, Throwable cause) {
        super(message, cause);
    }
}
