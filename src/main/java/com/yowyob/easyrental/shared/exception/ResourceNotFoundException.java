package com.yowyob.easyrental.shared.exception;

/**
 * Thrown when a requested resource is not found.
 *
 * @author Easy Rental Team
 * @since 2026-05-31
 */
public class ResourceNotFoundException extends BusinessException {

    public ResourceNotFoundException(String message) {
        super(message);
    }
}
