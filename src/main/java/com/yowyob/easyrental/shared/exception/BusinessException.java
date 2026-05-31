package com.yowyob.easyrental.shared.exception;

/**
 * Base class for business exceptions.
 *
 * @author Easy Rental Team
 * @since 2026-05-31
 */
public abstract class BusinessException extends RuntimeException {

    public BusinessException(String message) {
        super(message);
    }

    public BusinessException(String message, Throwable cause) {
        super(message, cause);
    }
}
