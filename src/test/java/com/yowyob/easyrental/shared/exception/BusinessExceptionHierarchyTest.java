package com.yowyob.easyrental.shared.exception;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class BusinessExceptionHierarchyTest {

    @Test
    void shouldCreateResourceNotFoundException() {
        var ex = new ResourceNotFoundException("missing");
        assertInstanceOf(BusinessException.class, ex);
        assertEquals("missing", ex.getMessage());
    }

    @Test
    void shouldCreateValidationException() {
        var ex = new ValidationException("invalid");
        assertEquals("invalid", ex.getMessage());
    }

    @Test
    void shouldCreateUnauthorizedException() {
        var ex = new UnauthorizedException("denied");
        assertEquals("denied", ex.getMessage());
    }

    @Test
    void shouldCreateQuotaExceededException() {
        var ex = new QuotaExceededException("quota");
        assertEquals("quota", ex.getMessage());
    }

    @Test
    void shouldCreateRentalConflictException() {
        var ex = new RentalConflictException("conflict");
        assertEquals("conflict", ex.getMessage());
    }
}
