package com.yowyob.easyrental.shared.exception;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void shouldReturnNotFoundWhenResourceNotFoundException() {
        var response = handler.handleNotFound(new ResourceNotFoundException("Not found")).block();
        assertEquals(HttpStatus.NOT_FOUND.value(), response.getStatusCode().value());
        assertEquals("Not found", response.getBody().message());
    }

    @Test
    void shouldReturnBadRequestWhenValidationException() {
        var response = handler.handleValidation(new ValidationException("Invalid input")).block();
        assertEquals(HttpStatus.BAD_REQUEST.value(), response.getStatusCode().value());
    }

    @Test
    void shouldReturnUnauthorizedWhenUnauthorizedException() {
        var response = handler.handleUnauthorized(new UnauthorizedException("Denied")).block();
        assertEquals(HttpStatus.UNAUTHORIZED.value(), response.getStatusCode().value());
    }

    @Test
    void shouldReturnForbiddenWhenQuotaExceededException() {
        var response = handler.handleQuota(new QuotaExceededException("Quota exceeded")).block();
        assertEquals(HttpStatus.FORBIDDEN.value(), response.getStatusCode().value());
    }

    @Test
    void shouldReturnConflictWhenRentalConflictException() {
        var response = handler.handleRentalConflict(new RentalConflictException("Conflict")).block();
        assertEquals(HttpStatus.CONFLICT.value(), response.getStatusCode().value());
    }

    @Test
    void shouldReturnInternalErrorWhenGenericException() {
        var response = handler.handleGeneral(new Exception("boom")).block();
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.value(), response.getStatusCode().value());
    }
}
