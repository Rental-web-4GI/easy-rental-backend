package com.yowyob.easyrental.shared.exception;

/**
 * Thrown when a rental conflicts with availability or state rules.
 *
 * @author Easy Rental Team
 * @since 2026-05-31
 */
public class RentalConflictException extends BusinessException {

    public RentalConflictException(String message) {
        super(message);
    }
}
