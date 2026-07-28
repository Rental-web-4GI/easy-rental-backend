package com.yowyob.easyrental.modules.rental.dto;

import com.yowyob.easyrental.modules.inspection.dto.InspectionCreateRequest;

/**
 * Request body for the check-in operation (vehicle pickup): records the
 * starting odometer reading and carries the CHECK_IN inspection payload.
 *
 * @author Easy Rental Team
 * @since 2026-07-25
 */
public record CheckInRequest(Integer startOdometer, InspectionCreateRequest inspection) {
}
