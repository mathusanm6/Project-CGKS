package com.github.cgks.exceptions;

/**
 * Custom exception for mining-related errors.
 */
public class MiningException extends Exception {
    public MiningException(String message) {
        super(message);
    }

    public MiningException(String message, Throwable cause) {
        super(message, cause);
    }
}