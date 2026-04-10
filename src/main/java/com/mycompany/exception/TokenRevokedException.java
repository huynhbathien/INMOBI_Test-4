package com.mycompany.exception;

/**
 * Custom exception for token revocation/blacklist
 */
public class TokenRevokedException extends RuntimeException {
    public TokenRevokedException(String message) {
        super(message);
    }

    public TokenRevokedException(String message, Throwable cause) {
        super(message, cause);
    }
}
