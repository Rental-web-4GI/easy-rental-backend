package com.yowyob.easyrental.shared.exception;

/**
 * Thrown when a user is not authorized for an operation.
 *
 * @author Easy Rental Team
 * @since 2026-05-31
 */
public class UnauthorizedException extends BusinessException {

    public UnauthorizedException(String message) {
        super(message);
    }
}
