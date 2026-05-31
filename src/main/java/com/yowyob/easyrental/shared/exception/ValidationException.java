package com.yowyob.easyrental.shared.exception;

/**
 * Thrown when input validation fails.
 *
 * @author Easy Rental Team
 * @since 2026-05-31
 */
public class ValidationException extends BusinessException {

    public ValidationException(String message) {
        super(message);
    }
}
