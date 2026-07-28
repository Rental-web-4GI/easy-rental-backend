package com.yowyob.easyrental.modules.rental.dto;

import com.yowyob.easyrental.modules.inspection.dto.InspectionCreateRequest;

/**
 * Request body for the check-out operation (vehicle drop-off): records the
 * ending odometer reading and carries the CHECK_OUT inspection payload.
 *
 * @author Easy Rental Team
 * @since 2026-07-25
 */
public record CheckOutRequest(Integer endOdometer, InspectionCreateRequest inspection) {
}
