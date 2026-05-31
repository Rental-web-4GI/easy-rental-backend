package com.yowyob.easyrental.shared.exception;

/**
 * Thrown when a subscription quota is exceeded.
 *
 * @author Easy Rental Team
 * @since 2026-05-31
 */
public class QuotaExceededException extends BusinessException {

    public QuotaExceededException(String message) {
        super(message);
    }
}
